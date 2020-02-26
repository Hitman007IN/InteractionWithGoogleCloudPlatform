package com.efx.empire.bigquery.injestion.operations;

import org.apache.beam.sdk.transforms.DoFn;

import com.efx.empire.bigquery.injestion.Driver;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;

public class IngestionParDo extends DoFn<String, TableRow> {

	@ProcessElement
	public void processElement(ProcessContext context) throws Exception {
		
		String[] split = context.element().split(",");
		TableRow row = new TableRow();
		
		for (int i = 0; i < split.length; i++) {
			
			TableFieldSchema col = Driver.schema.getTableSchema().getFields().get(i);
			row.set(col.getName(), split[i]);
		}
		
		context.output(row);
		
	}
	
}
