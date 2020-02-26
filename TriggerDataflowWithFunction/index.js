/**
 * Triggered from a message on a Cloud Pub/Sub topic.
 *
 * @param {object} pubsubMessage The Cloud Pub/Sub Message object.
 * @param {string} pubsubMessage.data The "data" property of the Cloud Pub/Sub Message.
 */
exports.subscribe = pubsubMessage => {
    // Print out the data from Pub/Sub, to prove that it worked
    console.log(Buffer.from(pubsubMessage.data, 'base64').toString());

    const data = pubsubMessage.data;

    google.auth.getApplicationDefault(function(err, authClient, projectId) {
    
        if (err) {
            throw err;
        }
    
        if (authClient.createScopedRequired && authClient.createScopedRequired()) {
            authClient = authClient.createScoped([
                'https://www.googleapis.com/auth/cloud-platform',
                'https://www.googleapis.com/auth/userinfo.email'
            ]);
        }
    
        const dataflow = google.dataflow({
            version: 'v1b3',
            auth: authClient
        });
            
        dataflow.projects.templates.create({
            projectId: projectId,
            resource: {
                parameters: {
                    inputFile: `gs://${file.bucket}/${file.name}`,
                    outputFile: `gs://${file.bucket}/${file.name}-wordcount`
                },
                jobName: 'cloud-fn-dataflow-test',
                gcsPath: 'gs://pipeline-staging/templates/WordCount'
            }
        }, function(err, response) {
            if (err) {
                console.error("problem running dataflow template, error was: ", err);
            }
            console.log("Dataflow template response: ", response);
            callback();
        });
    });
 
};
// [END functions_helloworld_pubsub_node8]

const google = require('googleapis');

exports.triggerDataflow = function(event, callback) {
    const file = event.data;
    if (file.resourceState === 'exists' && file.name) {
        google.auth.getApplicationDefault(function(err, authClient, projectId) {
            if (err) {
                throw err;
            }
            if (authClient.createScopedRequired && authClient.createScopedRequired()) {
                authClient = authClient.createScoped([
                    'https://www.googleapis.com/auth/cloud-platform',
                    'https://www.googleapis.com/auth/userinfo.email'
                ]);
            }
            const dataflow = google.dataflow({
                version: 'v1b3',
                auth: authClient
            });
            dataflow.projects.templates.create({
                projectId: projectId,
                resource: {
                    parameters: {
                        inputFile: `gs://${file.bucket}/${file.name}`,
                        outputFile: `gs://${file.bucket}/${file.name}-wordcount`
                    },
                    jobName: 'cloud-fn-dataflow-test',
                    gcsPath: 'gs://pipeline-staging/templates/WordCount'
                }
            }, function(err, response) {
                if (err) {
                    console.error("problem running dataflow template, error was: ", err);
                }
                console.log("Dataflow template response: ", response);
                callback();
            });
        });
    }
};
