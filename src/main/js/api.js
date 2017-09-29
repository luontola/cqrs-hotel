// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import axios from "axios";

let observedPosition = 0;

export function updateObservedPosition() {
  // TODO
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
  get: (url) => http.get(url, buildConfig()),
  post: (url, data) => http.post(url, data, buildConfig())
}
