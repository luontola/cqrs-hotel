
# CQRS Hotel

Example application demonstrating the use of [CQRS](http://martinfowler.com/bliki/CQRS.html) and [Event Sourcing](http://martinfowler.com/eaaDev/EventSourcing.html) within the domain of hotel reservations.

This project strives to differ from your typical toy examples in that *the problem domain is complex enough to warrant all the techniques being used.* The solution has been simplified, but the implemented features are production quality.


## Running

Start the database:

    docker-compose up -d db

Start the application:

    mvn spring-boot:run

Start the web frontend:

    yarn install
    yarn start

open http://localhost:8080/


## Building

You must have installed Java 8, Maven 3.2.5, Node.js 6.8.0, Docker 1.12 or higher versions of those. Then build this project with the command:

    ./build.sh


## More Resources

This example was mostly inspired by the following resources.

* [Greg Young's CQRS Class](https://goodenoughsoftware.net/online-videos/)
    * [An older free video](https://www.youtube.com/watch?v=whCk1Q87_ZI) and [its documentation](https://cqrs.wordpress.com/documents/)
* [Simple CQRS example](https://github.com/gregoryyoung/m-r)
* [Building an Event Storage](https://cqrs.wordpress.com/documents/building-event-storage/)

For more resources visit [Awesome Domain-Driven Design](https://github.com/heynickc/awesome-ddd). Ask questions at the [DDD/CQRS discussion group](https://groups.google.com/forum/#!forum/dddcqrs).
