// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

export const RESERVATION_OFFER_RECEIVED = 'RESERVATION_OFFER_RECEIVED';
export const reservationOfferReceived = (offer) => ({
  type: RESERVATION_OFFER_RECEIVED,
  offer
});

export const RESERVATION_MADE = 'RESERVATION_MADE';
export const reservationMade = (reservation) => ({
  type: RESERVATION_MADE,
  reservation
});
