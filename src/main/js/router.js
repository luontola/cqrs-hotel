// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import toRegex from "path-to-regexp";

export function matchURI(path, uri) {
  const keys = [];
  const pattern = toRegex(path, keys); // TODO: Use caching
  const match = pattern.exec(uri);
  if (!match) {
    return null;
  }
  const params = Object.create(null);
  for (let i = 1; i < match.length; i++) {
    params[keys[i - 1].name] = (match[i] !== undefined ? match[i] : undefined);
  }
  return params;
}

async function resolve(routes, context) {
  for (const route of routes) {
    const uri = context.error ? '/error' : context.pathname;
    const params = matchURI(route.path, uri);
    if (!params) {
      continue;
    }
    const result = await route.action({...context, params});
    if (result) {
      return result;
    }
  }
  const error = new Error('Not found');
  error.status = 404;
  throw error;
}

export default {resolve};
