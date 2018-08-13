// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import Layout from "./Layout";
import api from "../api";
import AccommodationSearchForm from "./AccommodationSearchForm";
import history from "../history";

const BookingPage = ({reservation, reservationOffer, makeReservation}) => (
  <Layout>
    <h2 className="content-subhead">Booking a Room</h2>
    <AccommodationSearchForm/>
    {reservationOffer && <div>
      <b>Arrival:</b> {reservationOffer.arrival}<br/>
      <b>Departure:</b> {reservationOffer.departure}<br/>
      {reservationOffer.totalPrice ? <div>
        <b>Cost:</b> {reservationOffer.totalPrice}<br/>
        <button type="button" // TODO: fields for inputting name and email
                onClick={() => makeReservation({...reservationOffer, name: "John Doe", email: "john@example.com"})}>
          Make Reservation
        </button>
      </div> : <div style={{color: 'red'}}>
        Sold out!
      </div>}
    </div>
    }
    {reservation && <div>
      Reservation: {JSON.stringify(reservation)}
    </div>
    }
  </Layout>
);

function mapStateToProps(state) {
  return {
    reservation: state.reservation.current,
    reservationOffer: state.reservation.offer,
  }
}

function mapDispatchToProps(dispatch) {
  return {
    makeReservation: (params) => {
      api.post('/api/make-reservation', params)
        .then(response => {
          const {reservationId} = params;
          history.push({pathname: `/reservations/${reservationId}`});
          // TODO: empty the state; must not be able to go back and make the same reservation again
        })
        .catch(error => {
          console.log("makeReservation failed", error.response, error);
          alert(`Something went wrong in making the reservation.\n${error.response.data.exception}: ${error.response.data.message}`);
        });
    },
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(BookingPage);
