// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import Layout from "./Layout";
import {dummyDataLoaded} from "./dummyActions";

var App = ({dummyData, loadDummyData}) => {
  return (
    <Layout>
      <button type="button" onClick={loadDummyData}>Load Dummy Data</button>
      <ul>
        {dummyData.map((data, index) => <li key={index}>{data}</li>)}
      </ul>
    </Layout>
  );
};

function mapStateToProps(state) {
  return {
    dummyData: state.dummy,
  }
}

function mapDispatchToProps(dispatch) {
  return {
    loadDummyData: () => {
      fetch('/api/dummy', {
        method: 'get',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        }
      })
        .then(response => response.json())
        .then(json => {
          console.log('parsed json', json);
          dispatch(dummyDataLoaded(json))
        })
        .catch(ex => console.log('parsing failed', ex))
    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
