package com.demo.ingest.gcsbqingest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.FileIO.ReadableFile;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ParDo.SingleOutput;
import org.apache.beam.sdk.transforms.Reshuffle;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;

import com.demo.ingest.gcsbqingest.config.CsvConfiguration;
import com.demo.ingest.gcsbqingest.config.GcpConfiguration;
import com.demo.ingest.gcsbqingest.decompress.Decompressor;
import com.demo.ingest.gcsbqingest.io.GcsInputReader;
import com.demo.ingest.gcsbqingest.io.GcsOutputWriterFactory;
import com.demo.ingest.gcsbqingest.io.InputReader;
import com.demo.ingest.gcsbqingest.process.pipeline.BigQueryDestinations;
import com.demo.ingest.gcsbqingest.process.pipeline.FillTableRowFn;
import com.demo.ingest.gcsbqingest.process.pipeline.GcsReadChunksFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.CsvDetectSchemaFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.CsvExtractHeadersFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.CsvMergeSchemaFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.CsvParseDataFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.GcsSplitCsvFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.advance.CsvParseDataAdvanceFn;
import com.demo.ingest.gcsbqingest.process.pipeline.csv.advance.GcsSplitCsvAdvanceFn;
import com.demo.ingest.gcsbqingest.process.schema.FieldType;
import com.demo.ingest.gcsbqingest.process.schema.GcpUtil;
import com.demo.ingest.gcsbqingest.util.StringUtil;
import com.google.api.gax.paging.Page;
import com.google.api.services.bigquery.model.TableRow;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.common.base.Strings;

/** Run the whole pipeline. */
public class PipelineRunner {

	public static void run(String projectId, String dataflowServiceAccount, String datasetId, String tempBucket,
			String gcsUri) throws IOException {
		String[] uriParts = StringUtil.splitGcsUri(gcsUri);
		String bucket = uriParts[0];
		String path = uriParts[1];

		try {
			List<String> uris = decompress(tempBucket, bucket, path);
			runDataflowPipeline(projectId, dataflowServiceAccount, tempBucket, uris, datasetId);
		} finally {
			cleanUp(tempBucket);
		}
	}

	private static List<String> decompress(String tempBucket, String bucket, String path) throws IOException {
		GoogleCredentials credentials = GcpConfiguration.getInstance().getCredentials();
		InputReader decompressReader = new GcsInputReader(credentials, bucket, path);
		Decompressor decompressor = new Decompressor(
				new GcsOutputWriterFactory(credentials, StringUtil.getGcsDecompressUri(tempBucket)));
		return decompressor.decompress(decompressReader);
	}

	private static void runDataflowPipeline(String projectId, @Nullable String serviceAccount, String tempBucket,
			List<String> uris, String datasetId) {
		GoogleCredentials credentials = GcpConfiguration.getInstance().getCredentials();
		DataflowPipelineOptions options = PipelineOptionsFactory.as(DataflowPipelineOptions.class);
		if (credentials != null) {
			options.setGcpCredential(credentials);
		}
		if (!Strings.isNullOrEmpty(serviceAccount)) {
			options.setServiceAccount(serviceAccount);
		}
		if (!Strings.isNullOrEmpty(projectId)) {
			options.setProject(projectId);
		}
		options.setTempLocation(String.format("%s/dataflow", StringUtil.getGcsTempDir(tempBucket)));
		options.setRunner(DataflowRunner.class);
		Pipeline p = Pipeline.create(options);

		PCollection<ReadableFile> files = p.apply(Create.of(uris)).apply(FileIO.matchAll()).apply(FileIO.readMatches())
				// Filter empty files.
				.apply(Filter.by(f -> f.getMetadata().sizeBytes() > 0));

		// Extract headers.
		PCollectionView<Map<String, String[]>> headersView = files.apply(ParDo.of(new CsvExtractHeadersFn()))
				.apply(View.asMap());

		// Determine which functions to use.
		CsvConfiguration config = CsvConfiguration.getInstance();
		boolean useAdvancedFns = config.getRecordSeparatorRegex() != null && config.getDelimiterRegex() != null;
		SingleOutput<ReadableFile, KV<String, Set<Long>>> splitFn = ParDo
				.of(useAdvancedFns ? new GcsSplitCsvAdvanceFn(config, headersView)
						: new GcsSplitCsvFn(config, headersView));
		SingleOutput<KV<String, byte[]>, KV<String, String[]>> parseFn = ParDo
				.of(useAdvancedFns ? new CsvParseDataAdvanceFn(config) : new CsvParseDataFn(config));

		GcpConfiguration gcpConfig = GcpConfiguration.getInstance();

		// Process data.
		PCollection<KV<String, String[]>> parsedData = files.apply(splitFn.withSideInputs(headersView))
				.apply(ParDo.of(new GcsReadChunksFn(gcpConfig)))
				// Reshuffle all chunk data for better scalability.
				.apply(Reshuffle.viaRandomKey()).apply(parseFn);

		// Schema detection.
		PCollectionView<Map<String, FieldType[]>> schemasView = parsedData.apply(ParDo.of(new CsvDetectSchemaFn()))
				.apply(Combine.perKey(new CsvMergeSchemaFn())).apply(View.asMap());

		// Fill CSV records into BigQuery TableRows.
		PCollection<KV<String, TableRow>> tableRows = parsedData
				.apply(ParDo.of(new FillTableRowFn(schemasView, headersView)).withSideInputs(schemasView, headersView));

		// Write TableRows to BigQuery.
		tableRows.apply(BigQueryIO.<KV<String, TableRow>>write()
				.to(new BigQueryDestinations(schemasView, headersView, projectId, datasetId))
				.withFormatFunction(KV::getValue).withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
				.withWriteDisposition(WriteDisposition.WRITE_TRUNCATE));

		PipelineResult result = p.run();
		result.waitUntilFinish();
	}

	private static void cleanUp(String bucket) {
		String prefix = StringUtil.getGcsTempDir(bucket);
		String[] parts = StringUtil.splitGcsUri(prefix);

		GoogleCredentials credentials = GcpConfiguration.getInstance().getCredentials();
		Storage storage = GcpUtil.getGcsClient(credentials);
		StorageBatch batch = storage.batch();
		Page<Blob> blobs = storage.list(bucket, Storage.BlobListOption.prefix(String.format("%s/", parts[1])));
		for (Blob blob : blobs.iterateAll()) {
			batch.delete(blob.getBlobId());
		}
		batch.submit();
	}
}