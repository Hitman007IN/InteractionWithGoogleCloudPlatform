package com.efx.empire.bigquery.injestion;

import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED;
import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition.WRITE_APPEND;

import java.io.FileNotFoundException;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;

import com.efx.empire.bigquery.injestion.operations.IngestionParDo;
import com.efx.empire.bigquery.injestion.pipelineoptions.BqIngestionOptions;
import com.efx.empire.bigquery.injestion.util.JsonSchemaReader;
import com.efx.empire.bigquery.injestion.util.Schema;

public class Driver {
	
	public static Schema schema;
	
	public static void main(String[] args) throws FileNotFoundException {
		
		BqIngestionOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(BqIngestionOptions.class);
		
		Pipeline pipeline = Pipeline.create(options);

		schema = new Schema(JsonSchemaReader.readSchemaFile(options.getSchema()));
		
		if(null != schema)
			pipeline.apply("READ", TextIO.read().from(options.getInputFile()))
	        	.apply("TRANSFORM", ParDo.of(new IngestionParDo()))
	        	.apply("WRITE", BigQueryIO.writeTableRows()
                        //.to(String.format("%s:empire_demo", options.getProject()))
	        			.to("usis-sg-nfn-npe-474a:empire_demo.user")
                        .withCreateDisposition(CREATE_IF_NEEDED)
                        .withWriteDisposition(WRITE_APPEND)
                        .withSchema(schema.getTableSchema()));
		else 
			System.out.println("schema is null");
		
	    pipeline.run().waitUntilFinish();

	}
		     
}
