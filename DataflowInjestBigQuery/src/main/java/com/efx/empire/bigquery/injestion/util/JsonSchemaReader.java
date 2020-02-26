package com.efx.empire.bigquery.injestion.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonSchemaReader {

	public static JsonObject readSchemaFile(String schemaFileName) throws FileNotFoundException {
		//File schemaFile = new File(
				//Objects.requireNonNull(JsonSchemaReader.class.getClassLoader().getResource(schemaFileName)).getFile());
		File schemaFile = new File(schemaFileName);
		return new Gson().fromJson(new FileReader(schemaFile), JsonObject.class);
	}
}
