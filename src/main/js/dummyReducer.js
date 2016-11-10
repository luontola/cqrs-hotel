// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import {DUMMY_DATA_LOADED} from "./dummyActions";

const dummy = (state = [], action) => {
  switch (action.type) {
    case DUMMY_DATA_LOADED:
      return [
        ...state,
        ...action.data,
      ];
    default:
      return state;
  }
};

export default dummy;
