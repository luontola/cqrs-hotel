// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import Layout from "./Layout";
import sortBy from "lodash-es/sortBy";
import Link from "./Link";

const ReservationsPage = ({reservations}) => (
  <Layout>
    <h2 className="content-subhead">Reservations</h2>
    <table className="pure-table">
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
      {sortBy(reservations, ['checkInTime', 'checkOutTime', 'name', 'reservationId']).map(reservation =>
        <tr key={reservation.reservationId}>
          <td><Link to={`/reservations/${reservation.reservationId}`}>{reservation.status}</Link></td>
          <td>{reservation.checkInTime}</td>
          <td>{reservation.checkOutTime}</td>
          <td>{reservation.name}</td>
          <td><a href={`mailto:${reservation.email}`}>{reservation.email}</a></td>
        </tr>
      )}
      </tbody>
    </table>
  </Layout>
);

export default ReservationsPage;
