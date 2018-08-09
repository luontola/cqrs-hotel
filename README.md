
# CQRS Hotel

Example application demonstrating the use of [CQRS](http://martinfowler.com/bliki/CQRS.html) and [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) within the domain of hotel reservations. #NoFrameworks

This project is a sandbox for exploring how CQRS+ES affects the design of a system. The hypothesis is that it will lead to a better design than a typical database-centric approach; a design that is easily testable and does not detiorate as features are added. To answer that question, the problem being solved needs to be complex enough.

This project strives to differ from your typical toy examples in that *the problem domain is complex enough to warrant all the techniques being used.* The solution has been simplified, but the implemented features are production quality.

**Source Code:** <https://github.com/luontola/cqrs-hotel>

 `master` branch  | API container | Web container
:----------------:|:-------------:|:-------------:
[![CI Build Status](https://travis-ci.org/luontola/cqrs-hotel.svg?branch=master)](https://travis-ci.org/luontola/cqrs-hotel) | [![Docker Build Status - API](https://img.shields.io/docker/build/luontola/cqrs-hotel-api.svg)](https://hub.docker.com/r/luontola/cqrs-hotel-api/) | [![Docker Build Status - Web](https://img.shields.io/docker/build/luontola/cqrs-hotel-web.svg)](https://hub.docker.com/r/luontola/cqrs-hotel-web/)


## Project Status

- technical features
    - [x] event store
    - [x] aggregate roots (write model)
    - [x] projections (read model)
    - [ ] process managers
    - [ ] GDPR compliance
- business features
    - [x] making a reservation
    - [ ] room allocation
    - [ ] payment
    - [ ] check-in, check-out
    - [ ] changing the departure date
    - [ ] changing the room


## Getting Started / Codebase Tour

Here are some pointers for where to look first in the code.

The [**web application's**](https://github.com/luontola/cqrs-hotel/tree/master/src/main/js) entry point is [index.js](https://github.com/luontola/cqrs-hotel/blob/master/src/main/js/index.js) and the entry points for each page are in [routes.js](https://github.com/luontola/cqrs-hotel/blob/master/src/main/js/routes.js). The UI is a single-page application which uses React and Redux but otherwise tries to avoid frameworks.

The [**backend application's**](https://github.com/luontola/cqrs-hotel/tree/master/src/main/java/fi/luontola/cqrshotel) main method is in [Application.java](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/Application.java) and the entry points for each operation are in [ApiController.java](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/ApiController.java). External dependencies are wired with Spring in `Application`, but the application core is wired in `ApiController` constructor. See there the command handlers and query handlers which are the entry point to the business logic.

The **framework** code is in the [fi.luontola.cqrshotel.framework package](https://github.com/luontola/cqrs-hotel/tree/master/src/main/java/fi/luontola/cqrshotel/framework). It contains in-memory and PostgreSQL implementations of the event store (the latter's PL/SQL scripts are in [src/main/resources/db/migration](https://github.com/luontola/cqrs-hotel/tree/master/src/main/resources/db/migration)), and base classes for aggregate roots and projections. CQRS with event sourcing requires very little infrastructure code, so you can easily write it yourself without external frameworks, which helps to reduce complexity.

To learn how the **write models** work, read how a reservation is made, starting from [SearchForAccommodationCommandHandler](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/reservation/commands/SearchForAccommodationCommandHandler.java) and [MakeReservationHandler](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/reservation/commands/MakeReservationHandler.java). The handlers contain no business logic. Instead, they delegate to [Reservation](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/reservation/Reservation.java) which does all the work. Read the [AggregateRoot](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/framework/AggregateRoot.java) base class, including its documentation, to understand how it should be used.

Of particular interest is how easy it is to **unit test** event sourced business logic. See [SearchForAccommodationTest](https://github.com/luontola/cqrs-hotel/blob/master/src/test/java/fi/luontola/cqrshotel/reservation/SearchForAccommodationTest.java) and [MakeReservationTest](https://github.com/luontola/cqrs-hotel/blob/master/src/test/java/fi/luontola/cqrshotel/reservation/MakeReservationTest.java). The given/when/then methods are in the simple [AggregateRootTester](https://github.com/luontola/cqrs-hotel/blob/master/src/test/java/fi/luontola/cqrshotel/framework/AggregateRootTester.java) base class.

To learn how the **read models** work, read [ReservationsView](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/reservation/queries/ReservationsView.java) and the base class [Projection](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/framework/Projection.java). Unit testing is again simple: [ReservationsViewTest](https://github.com/luontola/cqrs-hotel/blob/master/src/test/java/fi/luontola/cqrshotel/reservation/queries/ReservationsViewTest.java). Unlike aggregate roots, projections can listen to all events in the system; for example [CapacityView](https://github.com/luontola/cqrs-hotel/blob/master/src/main/java/fi/luontola/cqrshotel/capacity/CapacityView.java) is based on events from both Rooms and Reservations.


## Running

The easiest way to run this project is to use [Docker](https://www.docker.com/community-edition).

Start the application

    docker-compose pull
    docker-compose up -d 

The application will run at http://localhost:8080/

View application logs (in `--follow` mode)

    docker-compose logs -f api

Stop the application

    docker-compose stop

Stop the application and remove all data 

    docker-compose down


## Developing

To develop this project, you must have installed recent versions of [Java (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/), [Maven](https://maven.apache.org/), [Node.js](https://nodejs.org/), [Yarn](https://yarnpkg.com/) and [Docker](https://www.docker.com/community-edition). You can do a clean build with the `./build.sh` script. You can run this project's components individually with the following commands.

Start the database

    docker-compose up -d db

Start the API backend (without an IDE)

    mvn spring-boot:run

Start the web frontend (with live reloading)

    yarn install
    yarn start

The application will run at http://localhost:8080/


## More Resources

This example was mostly inspired by the following resources.

* [Greg Young's CQRS Class](https://goodenoughsoftware.net/online-videos/)
    * [An older free video](https://www.youtube.com/watch?v=whCk1Q87_ZI) and [its documentation](https://cqrs.wordpress.com/documents/)
* [Simple CQRS example](https://github.com/gregoryyoung/m-r)
* [Building an Event Storage](https://cqrs.wordpress.com/documents/building-event-storage/)

For more resources visit [Awesome Domain-Driven Design](https://github.com/heynickc/awesome-ddd). Ask questions at the [DDD/CQRS discussion group](https://groups.google.com/forum/#!forum/dddcqrs).
