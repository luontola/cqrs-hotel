// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {Layout} from "./Layout";

let ErrorPage = ({error}) => (
  <Layout>
    <h2 className="content-subhead">Error {error.status}: {error.message}</h2>
  </Layout>
);

export {ErrorPage};
