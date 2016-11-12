// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import "purecss/build/pure-min.css";
import "../css/style.css";
import "whatwg-fetch";
import React from "react";
import ReactDOM from "react-dom";
import {Provider} from "react-redux";
import {createStore, applyMiddleware} from "redux";
import createLogger from "redux-logger";
import {BookingPage} from "./ui/BookingPage";
import reducers from "./reducers";

function init() {
  const logger = createLogger();
  const store = createStore(reducers, applyMiddleware(logger));

  ReactDOM.render(
    <Provider store={store}>
      <BookingPage/>
    </Provider>,
    document.getElementById('root')
  );
}

init();
