// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

const namespace = 'cqrs-hotel/';

export const RESERVATION_OFFER_RECEIVED = namespace + 'RESERVATION_OFFER_RECEIVED';
export const reservationOfferReceived = (offer) => ({
  type: RESERVATION_OFFER_RECEIVED,
  offer
});

export const RESERVATION_MADE = namespace + 'RESERVATION_MADE';
export const reservationMade = (reservation) => ({
  type: RESERVATION_MADE,
  reservation
});
