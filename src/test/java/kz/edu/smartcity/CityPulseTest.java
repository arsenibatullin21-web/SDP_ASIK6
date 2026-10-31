package kz.edu.smartcity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CityPulseTest {
    private City city() {
        District central = new District("Central", Arrays.asList(
                new AirQualityMonitor("AQ-1", "Air monitor", 50),
                new TrafficCamera("TC-1", "Traffic camera", 80)));
        District north = new District("North", Arrays.asList(
                new WaterSensor("WS-1", "Water sensor", 30),
                new StreetLight("SL-1", "Street light", 40)));
        return new City("Astana", Arrays.asList(central, north));
    }

    private static class Counter implements Observer<CityEvent> {
        private final AtomicInteger count = new AtomicInteger();
        private volatile CityEvent last;
        @Override public void update(CityEvent event) { last = event; count.incrementAndGet(); }
    }

    private static class SelfRemoving implements Observer<CityEvent> {
        private final Subject<CityEvent> subject;
        private final AtomicInteger count = new AtomicInteger();
        SelfRemoving(Subject<CityEvent> subject) { this.subject = subject; }
        @Override public void update(CityEvent event) {
            count.incrementAndGet();
            subject.unsubscribe(EventTypes.AIR_ALERT, this);
        }
    }

    private static class AddingObserver implements Observer<CityEvent> {
        private final Subject<CityEvent> subject;
        private final Observer<CityEvent> added;
        AddingObserver(Subject<CityEvent> subject, Observer<CityEvent> added) {
            this.subject = subject;
            this.added = added;
        }
        @Override public void update(CityEvent event) {
            subject.subscribe(EventTypes.AIR_ALERT, added);
            subject.unsubscribe(EventTypes.AIR_ALERT, this);
        }
    }

    @Test void notifiesObserverForMatchingType() {
        EventHub hub = new EventHub(); Counter observer = new Counter();
        hub.subscribe(EventTypes.AIR_ALERT, observer);
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "High pollution"));
        assertEquals(1, observer.count.get());
    }

    @Test void doesNotNotifyObserverForAnotherType() {
        EventHub hub = new EventHub(); Counter observer = new Counter();
        hub.subscribe(EventTypes.AIR_ALERT, observer);
        hub.publish(new CityEvent(EventTypes.TRAFFIC_UPDATE, "Central", "Slow traffic"));
        assertEquals(0, observer.count.get());
    }

    @Test void unsubscribeStopsLaterNotifications() {
        EventHub hub = new EventHub(); Counter observer = new Counter();
        hub.subscribe(EventTypes.AIR_ALERT, observer);
        hub.unsubscribe(EventTypes.AIR_ALERT, observer);
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "High pollution"));
        assertEquals(0, observer.count.get());
    }

    @Test void observerCanUnsubscribeInsideItsHandler() {
        EventHub hub = new EventHub(); SelfRemoving observer = new SelfRemoving(hub);
        hub.subscribe(EventTypes.AIR_ALERT, observer);
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "One"));
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "Two"));
        assertEquals(1, observer.count.get());
    }

    @Test void observerCanSubscribeInsideItsHandler() {
        EventHub hub = new EventHub(); Counter added = new Counter();
        hub.subscribe(EventTypes.AIR_ALERT, new AddingObserver(hub, added));
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "One"));
        hub.publish(new CityEvent(EventTypes.AIR_ALERT, "Central", "Two"));
        assertEquals(1, added.count.get());
    }

    @Test void airSourcePublishesAirAlert() {
        AirQualitySource source = new AirQualitySource(); Counter observer = new Counter();
        source.subscribe(EventTypes.AIR_ALERT, observer);
        source.report("Central", "High pollution");
        assertEquals(EventTypes.AIR_ALERT, observer.last.getType());
    }

    @Test void trafficSourcePublishesTrafficUpdate() {
        TrafficSource source = new TrafficSource(); Counter observer = new Counter();
        source.subscribe(EventTypes.TRAFFIC_UPDATE, observer);
        source.report("North", "Slow traffic");
        assertEquals(EventTypes.TRAFFIC_UPDATE, observer.last.getType());
    }

    @Test void aggregatorActsAsObserverAndSubject() {
        AirQualitySource source = new AirQualitySource();
        DistrictAggregator aggregator = new DistrictAggregator(); Counter dashboard = new Counter();
        source.subscribe(EventTypes.AIR_ALERT, aggregator);
        aggregator.subscribe(EventTypes.CITY_SUMMARY, dashboard);
        source.report("Central", "High pollution");
        assertEquals(1, aggregator.getReceivedEvents());
        assertEquals(EventTypes.CITY_SUMMARY, dashboard.last.getType());
    }

    @Test void aggregatorCountsEventsFromTwoSources() {
        AirQualitySource air = new AirQualitySource(); TrafficSource traffic = new TrafficSource();
        DistrictAggregator aggregator = new DistrictAggregator();
        air.subscribe(EventTypes.AIR_ALERT, aggregator);
        traffic.subscribe(EventTypes.TRAFFIC_UPDATE, aggregator);
        air.report("Central", "High pollution"); traffic.report("North", "Slow traffic");
        assertEquals(2, aggregator.getReceivedEvents());
    }

    @Test void maintenanceVisitorCalculatesWholeCityCost() {
        assertEquals(200, city().accept(new MaintenanceCostVisitor()));
    }

    @Test void maintenanceVisitorCanCalculateOneDistrict() {
        assertEquals(130, city().getDistricts().get(0).accept(new MaintenanceCostVisitor()));
    }

    @Test void countVisitorCountsContainersAndAssets() {
        assertEquals(7, city().accept(new ElementCountVisitor()));
    }

    @Test void countVisitorKeepsItsCountAcrossVisits() {
        ElementCountVisitor visitor = new ElementCountVisitor(); City city = city();
        assertEquals(7, city.accept(visitor));
        assertEquals(14, city.accept(visitor));
    }

    @Test void filterVisitorReturnsAssetsAboveMinimumCost() {
        List<CityAsset> found = city().accept(new AssetFilterVisitor(50));
        assertEquals(2, found.size());
        assertEquals("AQ-1", found.get(0).getId());
        assertEquals("TC-1", found.get(1).getId());
    }

    @Test void jsonVisitorKeepsCityDistrictAndAssetNesting() {
        String json = city().accept(new JsonExportVisitor());
        assertTrue(json.startsWith("{\"type\":\"City\""));
        assertTrue(json.contains("\"districts\":[{\"type\":\"District\""));
        assertTrue(json.contains("\"assets\":[{\"type\":\"AirQualityMonitor\""));
        assertTrue(json.endsWith("]}]"));
    }

    @Test void airEventInspectsOnlyMatchingDistrictAndPublishesReport() {
        AirQualitySource air = new AirQualitySource(); EventHub reports = new EventHub(); Counter report = new Counter();
        District central = city().getDistricts().get(0);
        air.subscribe(EventTypes.AIR_ALERT, new InspectionListener(central, new MaintenanceCostVisitor(), reports));
        reports.subscribe(EventTypes.INSPECTION_REPORT, report);
        air.report("North", "High pollution");
        assertEquals(0, report.count.get());
        air.report("Central", "High pollution");
        assertEquals(1, report.count.get());
        assertTrue(report.last.getMessage().contains("130"));
    }

    @Test void highCostVisitorCountsMatchingAssets() {
        assertEquals(2, city().accept(new HighCostVisitor(50)));
    }

    @Test void newNoiseSensorWorksWithEveryVisitor() {
        District district = new District("East", Arrays.asList(
                new NoiseSensor("NS-1", "Noise sensor", 60)));
        City smallCity = new City("Astana", Arrays.asList(district));
        assertEquals(60, smallCity.accept(new MaintenanceCostVisitor()));
        assertEquals(3, smallCity.accept(new ElementCountVisitor()));
        assertEquals(1, smallCity.accept(new AssetFilterVisitor(50)).size());
        assertEquals(1, smallCity.accept(new HighCostVisitor(50)));
        assertTrue(smallCity.accept(new JsonExportVisitor()).contains("NoiseSensor"));
    }

    @Test void stressTestFourThreadsPublishExactlyFourThousandEvents() throws InterruptedException {
        EventHub hub = new EventHub(); Counter observer = new Counter();
        hub.subscribe(EventTypes.TRAFFIC_UPDATE, observer);
        ExecutorService producers = Executors.newFixedThreadPool(4);
        for (int thread = 0; thread < 4; thread++) {
            producers.execute(new Runnable() {
                @Override public void run() {
                    for (int event = 0; event < 1000; event++) {
                        hub.publish(new CityEvent(EventTypes.TRAFFIC_UPDATE, "Central", "Reading"));
                    }
                }
            });
        }
        producers.shutdown();
        assertTrue(producers.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(4000, observer.count.get());
    }
}
