package com.efx.empire.bigquery.injestion.pipelineoptions;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation.Required;

public interface BqIngestionOptions extends PipelineOptions{
	
    @Description("Path of the file to read from")
    @Required
    public String getInputFile();
    public void setInputFile(String inputFile);

    @Description("Path of the file to write to")
    @Required
    public String getOutput();
    public void setOutput(String output);
    
    @Description("Schema for the input file")
    @Required
    public String getSchema();
    public void setSchema(String schema);
   
    /** Specify the GCP resources */
    /*
   
    @Default
    @Description("GCP Project")
	String getProject();
	void setConfigKind(String project);
	
	@Default
    @Description("GCP Region")
	String getRegion();
	void setRegion(String region);
	
	@Description("GCP Zone")
	@Default
	String getZone();
	void setZone(String zone);
*/
}
