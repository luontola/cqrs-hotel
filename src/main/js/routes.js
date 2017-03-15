// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {BookingPage} from "./ui/BookingPage";
import {AdminPage} from "./ui/AdminPage";
import {ErrorPage} from "./ui/ErrorPage";

const routes = [
  {
    path: '/',
    action: () => <BookingPage/>
  },
  {
    path: '/admin',
    action: () => <AdminPage />
  },
  {
    path: '/error',
    action: ({error}) => <ErrorPage error={error}/>
  },
];

export default routes;
