package kz.edu.smartcity;

import java.util.concurrent.atomic.AtomicInteger;

final class EventTypes {
    static final String AIR_ALERT = "AIR_ALERT";
    static final String TRAFFIC_UPDATE = "TRAFFIC_UPDATE";
    static final String CITY_SUMMARY = "CITY_SUMMARY";
    static final String INSPECTION_REPORT = "INSPECTION_REPORT";
    private EventTypes() { }
}

abstract class CitySource implements Subject<CityEvent> {
    private final EventHub hub;
    protected CitySource(EventHub hub) { this.hub = hub; }
    protected void send(String type, String district, String message) {
        hub.publish(new CityEvent(type, district, message));
    }
    @Override public void subscribe(String type, Observer<CityEvent> observer) { hub.subscribe(type, observer); }
    @Override public void unsubscribe(String type, Observer<CityEvent> observer) { hub.unsubscribe(type, observer); }
    @Override public void publish(CityEvent event) { hub.publish(event); }
}

final class AirQualitySource extends CitySource {
    AirQualitySource(EventHub hub) { super(hub); }
    void report(String district, String reading) { send(EventTypes.AIR_ALERT, district, reading); }
}

final class TrafficSource extends CitySource {
    TrafficSource(EventHub hub) { super(hub); }
    void report(String district, String reading) { send(EventTypes.TRAFFIC_UPDATE, district, reading); }
}

final class DistrictAggregator implements Observer<CityEvent>, Subject<CityEvent> {
    private final EventHub output;
    private final AtomicInteger receivedEvents = new AtomicInteger();

    DistrictAggregator(EventHub output) { this.output = output; }

    @Override public void update(CityEvent event) {
        int total = receivedEvents.incrementAndGet();
        output.publish(new CityEvent(EventTypes.CITY_SUMMARY, event.getDistrict(),
                "Aggregated city event count: " + total));
    }
    int getReceivedEvents() { return receivedEvents.get(); }
    @Override public void subscribe(String type, Observer<CityEvent> observer) { output.subscribe(type, observer); }
    @Override public void unsubscribe(String type, Observer<CityEvent> observer) { output.unsubscribe(type, observer); }
    @Override public void publish(CityEvent event) { output.publish(event); }
}

abstract class CityListener implements Observer<CityEvent> {
    private final String name;
    private final AtomicInteger notifications = new AtomicInteger();
    private volatile CityEvent lastEvent;
    CityListener(String name) { this.name = name; }
    @Override public void update(CityEvent event) {
        lastEvent = event;
        notifications.incrementAndGet();
    }
    int getNotifications() { return notifications.get(); }
    CityEvent getLastEvent() { return lastEvent; }
    String getName() { return name; }
}

final class DashboardListener extends CityListener { DashboardListener() { super("Dashboard"); } }
final class AlertListener extends CityListener { AlertListener() { super("Alert display"); } }
final class StatisticsListener extends CityListener { StatisticsListener() { super("Statistics"); } }
final class TrafficLightListener extends CityListener { TrafficLightListener() { super("Traffic lights"); } }
final class LogListener extends CityListener { LogListener() { super("Event log"); } }

final class InspectionListener implements Observer<CityEvent> {
    private final District district;
    private final MaintenanceCostVisitor visitor;
    private final Subject<CityEvent> reportSubject;
    InspectionListener(District district, MaintenanceCostVisitor visitor, Subject<CityEvent> reportSubject) {
        this.district = district;
        this.visitor = visitor;
        this.reportSubject = reportSubject;
    }
    @Override public void update(CityEvent event) {
        if (EventTypes.AIR_ALERT.equals(event.getType())
                && district.getName().equals(event.getDistrict())) {
            int cost = district.accept(visitor);
            reportSubject.publish(new CityEvent(EventTypes.INSPECTION_REPORT, district.getName(),
                    "Inspection maintenance estimate: " + cost));
        }
    }
}


