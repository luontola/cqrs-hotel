// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import Layout from "./Layout";

const ReservationPage = ({reservation}) => (
  <Layout>
    <h2 className="content-subhead">Reservation</h2>
    <p><b>Status:</b> {reservation.status}</p>
    <p><b>Check-In:</b> {reservation.checkInTime}</p>
    <p><b>Check-Out:</b> {reservation.checkOutTime}</p>

    <h3>Guest Details</h3>
    <p><b>Name:</b> {reservation.name}</p>
    <p><b>Email:</b> {reservation.email}</p>
  </Layout>
);

export default ReservationPage;
