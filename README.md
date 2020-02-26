# InteractionWithGoogleCloudPlatform

Reference and motivation from - https://github.com/GoogleCloudPlatform/bigquery-data-importer

- To Run gcs-bq-ingest module

Prerequisites
- A GCP (Google Cloud Platform) project.
- GCS, BigQuery and Dataflow APIs are enabled.
    - The runner (either end user or service account as recommended below) needs to have the following roles at the project level:
        - roles/bigquery.dataViewer
        - roles/bigquery.jobUser
        - roles/bigquery.user
        - roles/compute.viewer
        - roles/dataflow.developer
    - The dataflow controller service account needs roles/storage.admin on the temporary bucket (provided to the pipeline by flag --temp_bucket, see below). Besides, it needs roles/bigquery.dataEditor on the target BigQuery dataset.
        - Alternatively, you could use a customized controller service account --dataflow_controller_service_account (which has to be roles/dataflow.worker). In this case you only have to manage one service account.
- Google Cloud SDK is installed.
- JDK 8+ is installed.
- maven is installed

java -jar target/gcs-bq-ingest-executable-0.0.1-SNAPSHOT.jar --region=asia-south1 --zone=asia-south1-b --gcp_project_id=flawless-mason-258102 --gcs_uri=gs://empire_landing_zone/input/activity.zip --bq_dataset=activity-dataset --temp_bucket=gs://empire_landing_zone/temp --gcp_credentials=/Users/vishakhrameshan/Documents/Workspace/InteractionWithGoogleCloudPlatform/flawless-mason-258102-6672dbdbc5ff.json --dataflow_controller_service_account=empire-gcs-dataflow-bq-compose@flawless-mason-258102.iam.gserviceaccount.com --verbose=true
