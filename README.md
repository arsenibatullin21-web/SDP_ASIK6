# Smart City Monitoring

This Java 17 project uses Observer for city sensor events and Visitor for reports about a city tree. It uses only the JDK and JUnit 5.

## Run

Open `Main.java` in IntelliJ and run it. The default uses three producer threads. Pass a number such as `4` to use four.

Run the tests with `mvn test`.

## Observer design

`AirQualitySource` and `TrafficSource` publish immutable `CityEvent` objects. The generic `Subject<E>` and `Observer<E>` interfaces allow different event subjects and listeners. Five listener objects handle alerts, traffic, statistics, dashboard summaries, and reports. `DistrictAggregator` is both an observer and a subject. It counts received updates and republishes a city summary. A listener can subscribe or unsubscribe during its own update. `EventHub` uses concurrent collections so publishing does not lock the whole subject.

Events use push: each immutable event carries its type, district, and message to matching observers. This keeps listeners from needing to query the sources.

## Visitor design

The Composite tree has `City`, `District`, and four asset types. Visitors perform the tree traversal. The four required operations calculate maintenance cost, count elements, filter expensive assets, and export nested JSON. Element objects only store names, IDs, costs, and child lists.

Observer and Visitor are connected by `InspectionListener`. An air alert for its district runs the cost visitor on that district only. It publishes the result as a new inspection report event.

## Concurrency and tests

The demo runs at least three producer tasks in an `ExecutorService` and waits for graceful completion. The stress test publishes 1,000 events from each of four threads and checks an exact total of 4,000. Tests also cover event routing, subscriptions, Visitor results, JSON nesting, and the Observer-Visitor integration.

## Trade-off commits

The two trade-off commits and their changed files will be listed here after they are created. One adds a visitor without changing elements. The other adds an element and updates the visitors that must handle it.
