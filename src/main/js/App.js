// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import Layout from "./Layout";
import {dummyDataLoaded} from "./dummyActions";
import {uuid, apiFetch} from "./util";

const reservationId = uuid();

const App = ({dummyData, loadDummyData, searchForAccommodation, makeReservation}) => {
  return (
    <Layout>
      <p>
        <button type="button" onClick={searchForAccommodation}>Find A Room</button>
      </p>
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
    searchForAccommodation: () => {
      apiFetch('/api/search-for-accommodation', {
        method: 'post',
        body: {
          reservationId: reservationId,
          startDate: '2016-11-15',
          endDate: '2016-11-16',
        },
      })
        .then(json => {
          console.log("searchForAccommodation", json);
        })
        .catch(ex => console.log("searchForAccommodation failed", ex));
    },
    makeReservation: () => {
      apiFetch('/api/make-reservation', {
        method: 'post',
        body: {
          reservationId: reservationId,
          startDate: '2016-11-15',
          endDate: '2016-11-16',
          name: "John Doe",
          email: "john@example.com",
        },
      })
        .then(json => {
          console.log("makeReservation", json);
        })
        .catch(ex => console.log("makeReservation failed", ex));
    },
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
