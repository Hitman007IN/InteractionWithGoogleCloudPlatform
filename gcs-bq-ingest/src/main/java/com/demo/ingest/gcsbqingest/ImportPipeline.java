package com.demo.ingest.gcsbqingest;

import java.io.IOException;

import com.demo.ingest.gcsbqingest.config.CsvConfiguration;
import com.demo.ingest.gcsbqingest.config.GcpConfiguration;
import com.demo.ingest.gcsbqingest.options.PipelineOptions;
import com.google.devtools.common.options.OptionsParser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author vishakhrameshan
 * @version 1.0.0
 *
 */
public class ImportPipeline {
	
	public static void main(String[] args) throws IOException {
		
		OptionsParser parser = OptionsParser.newOptionsParser(PipelineOptions.class);
		parser.parseAndExitUponError(args);
		PipelineOptions options = parser.getOptions(PipelineOptions.class);

		if (options.help) {
			options.printUsage(parser);
			return;
		}

		try {
			validateAndConstructOptions(options);
			PipelineRunner.run(options.projectId, options.dataflowControllerServiceAccount, options.bigQueryDataset,
					options.tempBucket, options.gcsUri);
		} catch (Exception e) {
			if (options.verbose) {
				throw new RuntimeException(e);
			} else {
				System.out.println(e.getMessage());
			}
		}
	}

	private static void validateAndConstructOptions(PipelineOptions options) throws IOException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(options.gcsUri),
				"GCS URI is required to provide the source file.");
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(options.bigQueryDataset),
				"BigQuery dataset and table are required.");
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(options.tempBucket), 
				"Temporary bucket is required.");
		
		boolean isCsvRecordSeparatorRegexSet = !Strings.isNullOrEmpty(options.csvRecordSeparatorRegex);
		boolean isCsvDelimiterRegexSet = !Strings.isNullOrEmpty(options.csvDelimiterRegex);
		
		Preconditions.checkArgument(!(isCsvDelimiterRegexSet ^ isCsvRecordSeparatorRegexSet),
				"csv_delimiter_regex and csv_record_separator_regex need to be specified together.");

		CsvConfiguration.getInstance().withRecordSeparator(options.csvRecordSeparator)
				.withDelimiter(options.csvDelimiter).withQuote(options.csvQuoteChar);

		if (isCsvDelimiterRegexSet && isCsvRecordSeparatorRegexSet) {
			CsvConfiguration.getInstance().withDelimiterRegex(options.csvDelimiterRegex)
					.withRecordSeparatorRegex(options.csvRecordSeparatorRegex);
		}

		GcpConfiguration.getInstance().withCredentials(options.gcpCredentials);
	}

}
