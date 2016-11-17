// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import axios from "axios";

const api = axios.create({
  timeout: 15000,
  headers: {
    'Accept': 'application/json',
  },
});

export {api};
