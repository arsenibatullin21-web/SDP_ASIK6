package kz.edu.smartcity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

interface CityElement {
    <R> R accept(Visitor<R> visitor);
}

interface Visitor<R> {
    R visit(City city);
    R visit(District district);
    R visit(AirQualityMonitor asset);
    R visit(TrafficCamera asset);
    R visit(WaterSensor asset);
    R visit(StreetLight asset);
    R visit(NoiseSensor asset);
}

final class City implements CityElement {
    private final String name;
    private final List<District> districts;

    City(String name, List<District> districts) {
        this.name = name;
        this.districts = Collections.unmodifiableList(new ArrayList<>(districts));
    }

    String getName() { return name; }
    List<District> getDistricts() { return districts; }

    @Override
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

final class District implements CityElement {
    private final String name;
    private final List<CityAsset> assets;

    District(String name, List<CityAsset> assets) {
        this.name = name;
        this.assets = Collections.unmodifiableList(new ArrayList<>(assets));
    }

    String getName() { return name; }
    List<CityAsset> getAssets() { return assets; }

    @Override
    public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

abstract class CityAsset implements CityElement {
    private final String id;
    private final String name;
    private final int monthlyCost;

    CityAsset(String id, String name, int monthlyCost) {
        this.id = id;
        this.name = name;
        this.monthlyCost = monthlyCost;
    }

    String getId() { return id; }
    String getName() { return name; }
    int getMonthlyCost() { return monthlyCost; }
}

final class AirQualityMonitor extends CityAsset {
    AirQualityMonitor(String id, String name, int monthlyCost) { super(id, name, monthlyCost); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

final class TrafficCamera extends CityAsset {
    TrafficCamera(String id, String name, int monthlyCost) { super(id, name, monthlyCost); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

final class WaterSensor extends CityAsset {
    WaterSensor(String id, String name, int monthlyCost) { super(id, name, monthlyCost); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

final class StreetLight extends CityAsset {
    StreetLight(String id, String name, int monthlyCost) { super(id, name, monthlyCost); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}

final class NoiseSensor extends CityAsset {
    NoiseSensor(String id, String name, int monthlyCost) { super(id, name, monthlyCost); }
    @Override public <R> R accept(Visitor<R> visitor) { return visitor.visit(this); }
}
