// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import Layout from "./Layout";

const ReservationsPage = ({reservations}) => (
  <Layout>
    <h2 className="content-subhead">Reservations</h2>
    <table>
      <thead>
      <tr>
        <th>Status</th>
        <th>Check-In</th>
        <th>Check-Out</th>
        <th>Guest</th>
        <th>E-mail</th>
      </tr>
      </thead>
      <tbody>
      {reservations.map(reservation =>
        <tr key={reservation.reservationId}>
          <td>{reservation.status}</td>
          <td>{reservation.checkInTime}</td>
          <td>{reservation.checkOutTime}</td>
          <td>{reservation.name}</td>
          <td>{reservation.email}</td>
        </tr>
      )}
      </tbody>
    </table>
  </Layout>
);

export default ReservationsPage;
