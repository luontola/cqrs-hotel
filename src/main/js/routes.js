// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import BookingPage from "./ui/BookingPage";
import ReservationsPage from "./ui/ReservationsPage";
import RoomsPage from "./ui/RoomsPage";
import ErrorPage from "./ui/ErrorPage";
import api from "./api";

async function getReservations() {
  const response = await api.get('/api/reservations');
  return response.data;
}

async function getRooms() {
  const response = await api.get('/api/rooms');
  return response.data;
}

export default [
  {
    path: '/',
    action: () => <BookingPage/>
  },
  {
    path: '/reservations',
    action: async () => {
      const reservations = await getReservations();
      return <ReservationsPage reservations={reservations}/>;
    }
  },
  {
    path: '/rooms',
    action: async () => {
      const rooms = await getRooms();
      return <RoomsPage rooms={rooms}/>;
    }
  },
  {
    path: '/error',
    action: ({error}) => <ErrorPage error={error}/>
  },
];
