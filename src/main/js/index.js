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
import {BookingPage} from "./ui/BookingPage";
import {AdminPage} from "./ui/AdminPage";
import {ErrorPage} from "./ui/ErrorPage";
import reducers from "./reducers";
import history from "./history";
import router from "./router";

const logger = createLogger();
const store = createStore(reducers, applyMiddleware(logger));
const root = document.getElementById('root');

const routes = [
  {
    path: '/',
    action: () =>
      <Provider store={store}>
        <BookingPage/>
      </Provider>
  },
  {
    path: '/admin',
    action: () => <AdminPage />
  },
  {
    path: '/error',
    action: ({error}) => <ErrorPage error={error}/>
  },
];

function renderComponent(component) {
  ReactDOM.render(component, root);
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
  console.log(`The current URL is ${location.pathname}${location.search}${location.hash}`);
  console.log(`The last navigation action was ${action}`);
  render(location);
});
