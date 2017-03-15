// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import "babel-polyfill";
import "purecss/build/pure-min.css";
import "../css/layout.css";
import React from "react";
import ReactDOM from "react-dom";
import {Provider} from "react-redux";
import {createStore, applyMiddleware} from "redux";
import createLogger from "redux-logger";
import reducers from "./reducers";
import history from "./history";
import router from "./router";
import routes from "./routes";

const logger = createLogger();
const store = createStore(reducers, applyMiddleware(logger));
const root = document.getElementById('root');

function renderComponent(component) {
  ReactDOM.render(<Provider store={store}>{component}</Provider>, root);
}

function render(location) {
  router.resolve(routes, location)
    .then(renderComponent)
    .catch(error =>
      router.resolve(routes, {...location, error})
        .then(renderComponent));
}

render(history.location);
history.listen((location, action) => {
  console.log(`Current URL is now ${location.pathname}${location.search}${location.hash} (${action})`);
  render(location);
});
