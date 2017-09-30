// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import axios from "axios";

let observedPosition = 0;

export async function handleResponse(responsePromise) {
  const response = await responsePromise;
  //console.log(response);
  //console.log(response.headers);
  // TODO: make the server return a X-Observed-Position header instead of detecting commit objects
  if (response.data.committedPosition) {
    observedPosition = response.data.committedPosition;
  }
  return response;
}

export function buildConfig() {
  return {
    headers: {'X-Observed-Position': '' + observedPosition}
  };
}

const http = axios.create({
  timeout: 15000,
  headers: {
    'Accept': 'application/json',
  },
});

export default {
  get: (url) => handleResponse(http.get(url, buildConfig())),
  post: (url, data) => handleResponse(http.post(url, data, buildConfig()))
}
