package com.demo.ingest.gcsbqingest.options;

import java.util.Collections;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParser;

/** Customized option parser. */
public class PipelineOptions extends OptionsBase {

    @Option(
        name = "help",
        abbrev = 'h',
        help = "Print usage information.",
        defaultValue = "false")
    public boolean help;

    @Option(
        name = "csv_delimiter",
        help = "The delimiter used to separator CSV columns.",
        category = "csv",
        converter = CharOptionsConverter.class,
        defaultValue = ",")
    public char csvDelimiter;

    @Option(
        name = "csv_quote",
        help = "The quote character used in the file.",
        category = "csv",
        converter = CharOptionsConverter.class,
        defaultValue = "\"")
    public char csvQuoteChar;

    @Option(
        name = "csv_record_separator",
        help = "Row separator, typically this is either '\n' or '\r\n'.",
        category = "csv",
        defaultValue = "\n")
    public String csvRecordSeparator;

    @Option(
        name = "csv_delimiter_regex",
        help = "Optional: A regular expression used to separate fields in a record. Lookahead and"
            + "lookbehind can be used. This has to be used in combination with"
            + "csv_record_separator_regex.",
        category = "csv",
        defaultValue = "")
    public String csvDelimiterRegex;

    @Option(
        name = "csv_record_separator_regex",
        help = "Optional: A regular expression used to separate records. This has to be used in"
            + "combination wtih csv_delimiter_regex.",
        category = "csv",
        defaultValue = "")
    public String csvRecordSeparatorRegex;

    // TODO(b/120795556): update this to take a list of URIs (file or directory, can have
    // wildcards).
    @Option(
        name = "gcs_uri",
        help = "The URI of the source file on GCS.",
        defaultValue = "")
    public String gcsUri;

    @Option(
        name = "bq_dataset",
        help = "The BigQuery dataset to import the data.",
        defaultValue = "")
    public String bigQueryDataset;

    @Option(
        name = "temp_bucket",
        help = "Used to store temporary files.",
        defaultValue = "")
    public String tempBucket;

    @Option(
        name = "gcp_project_id",
        help = "The project id used to run the pipeline.",
        defaultValue = "")
    public String projectId;

    @Option(
        name = "gcp_credentials",
        help = "Path to the credentials (usually a .json file) of a service account used to access"
            + "resources (GCS, Dataflow, BigQuery), current users credentials will be used if not"
            + "specified.",
        defaultValue = "")
    public String gcpCredentials;

    @Option(
        name = "dataflow_controller_service_account",
        help = "Customized Dataflow controller service account, see"
            + "https://cloud.google.com/dataflow/docs/concepts/security-and-permissions"
            + "#controller_service_account. The default will be used if not specified.",
        defaultValue = "")
    public String dataflowControllerServiceAccount;

    @Option(
        name = "verbose",
        abbrev = 'v',
        help = "Whether to output verbose messages.",
        defaultValue = "false")
    public boolean verbose;

    public void printUsage(OptionsParser parser) {
      System.out.println("Usage: java -jar import.jar OPTIONS");
      System.out.println(
          parser.describeOptions(
              Collections.emptyMap(), OptionsParser.HelpVerbosity.LONG));
    }
    
}