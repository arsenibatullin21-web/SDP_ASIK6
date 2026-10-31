package kz.edu.smartcity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int producerCount = args.length == 0 ? 3 : Integer.parseInt(args[0]);
        if (producerCount < 3) {
            throw new IllegalArgumentException("Use at least 3 producer threads");
        }

        District central = new District("Central", Arrays.asList(
                new AirQualityMonitor("AQ-1", "Air monitor", 50),
                new TrafficCamera("TC-1", "Traffic camera", 80)));
        District north = new District("North", Arrays.asList(
                new WaterSensor("WS-1", "Water sensor", 30),
                new StreetLight("SL-1", "Street light", 40)));
        City city = new City("Astana", Arrays.asList(central, north));

        System.out.println("Maintenance cost: " + city.accept(new MaintenanceCostVisitor()));
        System.out.println("Element count: " + city.accept(new ElementCountVisitor()));
        System.out.println("JSON: " + city.accept(new JsonExportVisitor()));

        AirQualitySource air = new AirQualitySource();
        TrafficSource traffic = new TrafficSource();
        DistrictAggregator aggregator = new DistrictAggregator();
        DashboardListener dashboard = new DashboardListener();
        AlertListener alerts = new AlertListener();
        StatisticsListener statistics = new StatisticsListener();
        TrafficLightListener trafficLights = new TrafficLightListener();
        LogListener log = new LogListener();
        EventHub reports = new EventHub();

        air.subscribe(EventTypes.AIR_ALERT, alerts);
        air.subscribe(EventTypes.AIR_ALERT, statistics);
        air.subscribe(EventTypes.AIR_ALERT, aggregator);
        air.subscribe(EventTypes.AIR_ALERT, new InspectionListener(central, new MaintenanceCostVisitor(), reports));
        traffic.subscribe(EventTypes.TRAFFIC_UPDATE, trafficLights);
        traffic.subscribe(EventTypes.TRAFFIC_UPDATE, statistics);
        traffic.subscribe(EventTypes.TRAFFIC_UPDATE, aggregator);
        aggregator.subscribe(EventTypes.CITY_SUMMARY, dashboard);
        reports.subscribe(EventTypes.INSPECTION_REPORT, log);

        ExecutorService producers = Executors.newFixedThreadPool(producerCount);
        AtomicInteger finished = new AtomicInteger();
        for (int worker = 0; worker < producerCount; worker++) {
            final int workerNumber = worker;
            producers.execute(new Runnable() {
                @Override public void run() {
                    for (int i = 0; i < 5; i++) {
                        air.report("Central", "Air reading " + workerNumber + ":" + i);
                        traffic.report("North", "Traffic reading " + workerNumber + ":" + i);
                    }
                    finished.incrementAndGet();
                }
            });
        }
        producers.shutdown();
        if (!producers.awaitTermination(30, TimeUnit.SECONDS)) {
            producers.shutdownNow();
            throw new IllegalStateException("Producer threads did not stop");
        }
        if (finished.get() != producerCount) {
            throw new IllegalStateException("Some producer tasks did not finish");
        }

        System.out.println("Producers completed: " + finished.get());
        System.out.println("Dashboard notifications: " + dashboard.getNotifications());
        System.out.println("Alert notifications: " + alerts.getNotifications());
        System.out.println("Traffic notifications: " + trafficLights.getNotifications());
        System.out.println("Reports: " + log.getNotifications());
        System.out.println("Aggregated source events: " + aggregator.getReceivedEvents());
    }
}
