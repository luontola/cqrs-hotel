// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import Layout from "./Layout";
import {dummyDataLoaded} from "./dummyActions";
import {uuid} from "./util";

var App = ({dummyData, loadDummyData, makeReservation}) => {
  return (
    <Layout>
      <p>
        <button type="button" onClick={makeReservation}>Make Reservation</button>
      </p>
      <p>
        <button type="button" onClick={loadDummyData}>Load Dummy Data</button>
      </p>
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
        },
      })
        .then(response => response.json())
        .then(json => {
          console.log("loadDummyData", json);
          dispatch(dummyDataLoaded(json))
        })
        .catch(ex => console.log("loadDummyData failed", ex))
    },
    makeReservation: () => {
      fetch('/api/make-reservation', {
        method: 'post',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          reservationId: uuid(),
          startDate: '2016-11-15',
          endDate: '2016-11-16',
          name: "John Doe",
          email: "john@example.com",
        }),
      })
        .then(response => response.json())
        .then(json => {
          console.log("makeReservation", json);
        })
        .catch(ex => console.log("makeReservation failed", ex))

    }
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
