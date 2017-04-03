// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
import React from "react";
import {connect} from "react-redux";
import {reduxForm, Field, SubmissionError} from "redux-form";
import uuid from "uuid/v4";
import {api} from "../util";
import {reservationOfferReceived} from "../reservationActions";
import moment from "moment";

let AccommodationSearchForm = ({handleSubmit, submitting, error}) => (
  <form onSubmit={handleSubmit(searchForAccommodation)}>
    <div>
      <Field name="startDate" component="input" type="text"/>
      <Field name="endDate" component="input" type="text"/>
      <button type="submit" disabled={submitting}>Find A Room</button>
      {error && <div style={{color: 'red'}}>{error}</div>}
    </div>
  </form>
);

AccommodationSearchForm = reduxForm({
  form: 'AccommodationSearchForm'
})(AccommodationSearchForm);

function searchForAccommodation(form, dispatch, props) {
  const {startDate, endDate} = form;
  const {reservationId} = props;
  return api.post('/api/search-for-accommodation', {
    reservationId: reservationId,
    startDate: startDate,
    endDate: endDate,
  })
    .then(response => {
      dispatch(reservationOfferReceived(response.data))
    })
    .catch(error => {
      console.log("searchForAccommodation failed", error);
      throw new SubmissionError({_error: "Search failed"})
    });
}

function mapStateToProps(state) {
  return {
    reservationId: state.reservation.id || uuid(),
    initialValues: {
      startDate: moment().format('YYYY-MM-DD'),
      endDate: moment().add(1, 'days').format('YYYY-MM-DD'),
    }
  }
}

AccommodationSearchForm = connect(mapStateToProps)(AccommodationSearchForm);

export {AccommodationSearchForm};
