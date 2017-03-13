// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import "purecss/build/pure-min.css";
import "../css/layout.css";
import React from "react";
import ReactDOM from "react-dom";
import {Provider} from "react-redux";
import {createStore, applyMiddleware} from "redux";
import createLogger from "redux-logger";
import {BookingPage} from "./ui/BookingPage";
import reducers from "./reducers";
import history from "./history";
import {Link} from "./ui/Link";

class HomePage extends React.Component {
  render() {
    return (
      <ul>
        <li><Link to="/">Home</Link></li>
        <li><Link to="/one">One</Link></li>
        <li><Link to="/two">Two</Link></li>
        <li><Link to="/booking">Booking</Link></li>
      </ul>
    );
  }
}

const logger = createLogger();
const store = createStore(reducers, applyMiddleware(logger));
const root = document.getElementById('root');

const routes = [
  {
    path: '/',
    action: () => <HomePage />
  },
  {
    path: '/one',
    action: () => <HomePage />
  },
  {
    path: '/two',
    action: () => <HomePage />
  },
  {
    path: '/booking',
    action: () =>
      <Provider store={store}>
        <BookingPage/>
      </Provider>
  },
];

function renderComponent(component) {
  ReactDOM.render(component, root);
}

function resolveRoute(routes, location) {
  for (const route of routes) {
    if (location.pathname === route.path) {
      return route.action();
    }
  }
  return (
    <div>Page not found: {location.pathname}</div>
  );
}

function render(location) {
  const component = resolveRoute(routes, location);
  renderComponent(component);
}

render(history.location);
history.listen((location, action) => {
  console.log(`The current URL is ${location.pathname}${location.search}${location.hash}`);
  console.log(`The last navigation action was ${action}`);
  render(location);
});
