// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import "purecss/build/pure-min.css";
import "../css/style.css";
import "whatwg-fetch";
import React from "react";
import ReactDOM from "react-dom";
import Layout from "./Layout";

function init() {
  loadDummyData();
  renderUI();
}

function loadDummyData() {
  // TODO: show in the UI
  fetch('/api/dummy', {
    method: 'get',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    }
  })
    .then(response => response.json())
    .then(json => console.log('parsed json', json))
    .catch(ex => console.log('parsing failed', ex))
}

function renderUI() {
  ReactDOM.render(
    <Layout/>,
    document.getElementById('root')
  );
}

init();
