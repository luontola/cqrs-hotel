// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import Layout from "./Layout";
import {uuid, apiFetch} from "./util";
import {reservationOfferReceived} from "./reservationActions";

const App = ({reservationId, reservationOffer, searchForAccommodation, makeReservation}) => {
  return (
    <Layout>
      <p>
        <button type="button" onClick={() => searchForAccommodation(reservationId)}>Find A Room</button>
      </p>
      {reservationOffer && <p>
        <b>Check in:</b> {reservationOffer.startDate}<br/>
        <b>Check out:</b> {reservationOffer.endDate}<br/>
        <b>Cost:</b> {reservationOffer.totalPrice}<br/>
        <button type="button" onClick={() => makeReservation(reservationOffer)}>Make Reservation</button>
      </p>
      }
    </Layout>
  );
};

function mapStateToProps(state) {
  return {
    reservationId: state.reservation.id || uuid(),
    reservationOffer: state.reservation.offer,
  }
}

function mapDispatchToProps(dispatch) {
  return {
    searchForAccommodation: (reservationId) => {
      apiFetch('/api/search-for-accommodation', {
        method: 'post',
        body: {
          reservationId: reservationId,
          startDate: '2016-11-15',
          endDate: '2016-11-16',
        },
      })
        .then(result => {
          console.log("searchForAccommodation", result);
          dispatch(reservationOfferReceived(result))
        })
        .catch(ex => console.log("searchForAccommodation failed", ex));
    },
    makeReservation: (reservationOffer) => {
      apiFetch('/api/make-reservation', {
        method: 'post',
        body: {
          ...reservationOffer,
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
