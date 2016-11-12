// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export function apiFetch(url, options = {}) {
  options = {
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    },
    credentials: 'same-origin',
    redirect: 'error',
    ...options,
  };
  if (options.queryParams) {
    url += (url.indexOf('?') === -1 ? '?' : '&') + queryParams(options.queryParams);
    delete options.queryParams;
  }
  if (typeof options.body === 'object') {
    options.body = JSON.stringify(options.body);
  }
  return fetch(url, options)
    .then(response => {
      if (response.ok) {
        return response.json();
      } else {
        return response.json().then(error => Promise.reject(error));
      }
    });
}

function queryParams(params) {
  return Object.keys(params)
    .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(params[k]))
    .join('&');
}
