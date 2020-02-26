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

  triggerDag(pubsubMessage.data);
 
};
// [END functions_helloworld_pubsub_node8]

// [START composer_trigger]
'use strict';

const fetch = require('node-fetch');
const FormData = require('form-data');

/**
 * Triggered from a message on a Cloud Storage bucket.
 *
 * IAP authorization based on:
 * https://stackoverflow.com/questions/45787676/how-to-authenticate-google-cloud-functions-for-access-to-secure-app-engine-endpo
 * and
 * https://cloud.google.com/iap/docs/authentication-howto
 *
 * @param {!Object} data The Cloud Functions event data.
 * @returns {Promise}
 */
const triggerDag = async (data) => {
  // Fill in your Composer environment information here.

  // The project that holds your function
  const PROJECT_ID = 'your-project-id';
  // Navigate to your webserver's login page and get this from the URL
  const CLIENT_ID = 'your-iap-client-id';
  // This should be part of your webserver's URL:
  // {tenant-project-id}.appspot.com
  const WEBSERVER_ID = 'your-tenant-project-id';
  // The name of the DAG you wish to trigger

  if (data === "activity") {
  	const DAG_NAME = 'load_activity_dag';

  } else if (data === "final") {
  	const DAG_NAME = 'load_final_dag';

  } else if (data === "fallout") {
  	const DAG_NAME = 'load_fallout_dag';

  } else if (data === "fnma_sa") {
  	const DAG_NAME = 'load_fnma_sa_dag';

  } else if (data === "dimension") {
  	const DAG_NAME = 'load_dimension_dag';

  } else if (data === "lcr") {
  	const DAG_NAME = 'load_lcr_dag';

  } else {
  	console.log ("proper message in pub sub topic not specified");
  }

  // Other constants
  const WEBSERVER_URL = `https://${WEBSERVER_ID}.appspot.com/api/experimental/dags/${DAG_NAME}/dag_runs`;
  const USER_AGENT = 'gcf-event-trigger';
  const BODY = {conf: JSON.stringify(data)};

  // Make the request
  try {
    const iap = await authorizeIap(CLIENT_ID, PROJECT_ID, USER_AGENT);

    return makeIapPostRequest(WEBSERVER_URL, BODY, iap.idToken, USER_AGENT);
  } catch (err) {
    console.error('Error authorizing IAP:', err.message);
    throw new Error(err);
  }
};

/**
 * @param {string} clientId The client id associated with the Composer webserver application.
 * @param {string} projectId The id for the project containing the Cloud Function.
 * @param {string} userAgent The user agent string which will be provided with the webserver request.
 */
const authorizeIap = async (clientId, projectId, userAgent) => {
  const SERVICE_ACCOUNT = `${projectId}@appspot.gserviceaccount.com`;
  const JWT_HEADER = Buffer.from(
    JSON.stringify({alg: 'RS256', typ: 'JWT'})
  ).toString('base64');

  let jwt = '';
  let jwtClaimset = '';

  // Obtain an Oauth2 access token for the appspot service account
  const res = await fetch(
    `http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/${SERVICE_ACCOUNT}/token`,
    {
      headers: {'User-Agent': userAgent, 'Metadata-Flavor': 'Google'},
    }
  );
  const tokenResponse = await res.json();
  if (tokenResponse.error) {
    console.error('Error in token reponse:', tokenResponse.error.message);
    return Promise.reject(tokenResponse.error);
  }

  const accessToken = tokenResponse.access_token;
  const iat = Math.floor(new Date().getTime() / 1000);
  const claims = {
    iss: SERVICE_ACCOUNT,
    aud: 'https://www.googleapis.com/oauth2/v4/token',
    iat: iat,
    exp: iat + 60,
    target_audience: clientId,
  };
  jwtClaimset = Buffer.from(JSON.stringify(claims)).toString('base64');
  const toSign = [JWT_HEADER, jwtClaimset].join('.');

  const blob = await fetch(
    `https://iam.googleapis.com/v1/projects/${projectId}/serviceAccounts/${SERVICE_ACCOUNT}:signBlob`,
    {
      method: 'POST',
      body: JSON.stringify({
        bytesToSign: Buffer.from(toSign).toString('base64'),
      }),
      headers: {
        'User-Agent': userAgent,
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );
  const blobJson = await blob.json();
  if (blobJson.error) {
    console.error('Error in blob signing:', blobJson.error.message);
    return Promise.reject(blobJson.error);
  }

  // Request service account signature on header and claimset
  const jwtSignature = blobJson.signature;
  jwt = [JWT_HEADER, jwtClaimset, jwtSignature].join('.');
  const form = new FormData();
  form.append('grant_type', 'urn:ietf:params:oauth:grant-type:jwt-bearer');
  form.append('assertion', jwt);

  const token = await fetch('https://www.googleapis.com/oauth2/v4/token', {
    method: 'POST',
    body: form,
  });
  const tokenJson = await token.json();
  if (tokenJson.error) {
    console.error('Error fetching token:', tokenJson.error.message);
    return Promise.reject(tokenJson.error);
  }

  return {
    idToken: tokenJson.id_token,
  };
};

/**
 * @param {string} url The url that the post request targets.
 * @param {string} body The body of the post request.
 * @param {string} idToken Bearer token used to authorize the iap request.
 * @param {string} userAgent The user agent to identify the requester.
 */
const makeIapPostRequest = async (url, body, idToken, userAgent) => {
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'User-Agent': userAgent,
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const err = await res.text();
    console.error('Error making IAP post request:', err.message);
    throw new Error(err);
  }
};
// [END composer_trigger]