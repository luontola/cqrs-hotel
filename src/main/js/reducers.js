// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import {combineReducers} from "redux";
import reservation from "./reservationReducer";
import {reducer as form} from "redux-form";

export default combineReducers({
  reservation,
  form,
});
