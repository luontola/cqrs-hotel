// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import Layout from "./Layout";
import sortBy from "lodash-es/sortBy";

const RoomsPage = ({rooms}) => (
  <Layout>
    <h2 className="content-subhead">Rooms</h2>
    <table className="pure-table">
      <thead>
      <tr>
        <th>Number</th>
        <th>Availability</th>
      </tr>
      </thead>
      <tbody>
      {sortBy(rooms, ['roomNumber', 'roomId']).map(room =>
        <tr key={room.roomId}>
          <td>{room.roomNumber}</td>
          <td>{JSON.stringify(room.availability)}</td>
        </tr>
      )}
      </tbody>
    </table>

  </Layout>
);

export default RoomsPage;
