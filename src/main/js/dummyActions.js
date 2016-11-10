// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

export const DUMMY_DATA_LOADED = 'DUMMY_DATA_LOADED';
export const dummyDataLoaded = (data) => ({
  type: DUMMY_DATA_LOADED,
  data
});
