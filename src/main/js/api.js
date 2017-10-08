// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import axios from "axios";

const OBSERVED_POSITION_HEADER = 'x-observed-position';
let observedPosition = null;

function rememberObservedPosition(response) {
  const value = response.headers[OBSERVED_POSITION_HEADER];
  if (value) {
    observedPosition = value;
  }
}

async function handleResponse(responsePromise) {
  const response = await responsePromise;
  rememberObservedPosition(response);
  return response;
}

export function buildConfig() {
  const headers = {};
  if (observedPosition) {
    headers[OBSERVED_POSITION_HEADER] = observedPosition;
  }
  return {
    headers
  };
}

const http = axios.create({
  timeout: 60000,
  headers: {
    'Accept': 'application/json',
  },
});

export default {
  get: (url) => handleResponse(http.get(url, buildConfig())),
  post: (url, data) => handleResponse(http.post(url, data, buildConfig()))
}
