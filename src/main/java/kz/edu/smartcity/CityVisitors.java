package kz.edu.smartcity;

import java.util.ArrayList;
import java.util.List;

final class MaintenanceCostVisitor implements Visitor<Integer> {
    @Override public Integer visit(City city) {
        int total = 0;
        for (District district : city.getDistricts()) total += district.accept(this);
        return total;
    }
    @Override public Integer visit(District district) {
        int total = 0;
        for (CityAsset asset : district.getAssets()) total += asset.accept(this);
        return total;
    }
    @Override public Integer visit(AirQualityMonitor asset) { return asset.getMonthlyCost(); }
    @Override public Integer visit(TrafficCamera asset) { return asset.getMonthlyCost(); }
    @Override public Integer visit(WaterSensor asset) { return asset.getMonthlyCost(); }
    @Override public Integer visit(StreetLight asset) { return asset.getMonthlyCost(); }
}

final class ElementCountVisitor implements Visitor<Integer> {
    private int count;
    @Override public Integer visit(City city) {
        count++;
        for (District district : city.getDistricts()) district.accept(this);
        return count;
    }
    @Override public Integer visit(District district) {
        count++;
        for (CityAsset asset : district.getAssets()) asset.accept(this);
        return count;
    }
    @Override public Integer visit(AirQualityMonitor asset) { count++; return count; }
    @Override public Integer visit(TrafficCamera asset) { count++; return count; }
    @Override public Integer visit(WaterSensor asset) { count++; return count; }
    @Override public Integer visit(StreetLight asset) { count++; return count; }
}

final class AssetFilterVisitor implements Visitor<List<CityAsset>> {
    private final int minimumMonthlyCost;
    private final List<CityAsset> found = new ArrayList<>();
    AssetFilterVisitor(int minimumMonthlyCost) { this.minimumMonthlyCost = minimumMonthlyCost; }
    @Override public List<CityAsset> visit(City city) {
        for (District district : city.getDistricts()) district.accept(this);
        return new ArrayList<>(found);
    }
    @Override public List<CityAsset> visit(District district) {
        for (CityAsset asset : district.getAssets()) asset.accept(this);
        return new ArrayList<>(found);
    }
    private List<CityAsset> addIfExpensive(CityAsset asset) {
        if (asset.getMonthlyCost() >= minimumMonthlyCost) found.add(asset);
        return new ArrayList<>(found);
    }
    @Override public List<CityAsset> visit(AirQualityMonitor asset) { return addIfExpensive(asset); }
    @Override public List<CityAsset> visit(TrafficCamera asset) { return addIfExpensive(asset); }
    @Override public List<CityAsset> visit(WaterSensor asset) { return addIfExpensive(asset); }
    @Override public List<CityAsset> visit(StreetLight asset) { return addIfExpensive(asset); }
}

final class JsonExportVisitor implements Visitor<String> {
    @Override public String visit(City city) {
        StringBuilder json = new StringBuilder("{\"type\":\"City\",\"name\":\"")
                .append(escape(city.getName())).append("\",\"districts\":[");
        for (int i = 0; i < city.getDistricts().size(); i++) {
            if (i > 0) json.append(',');
            json.append(city.getDistricts().get(i).accept(this));
        }
        return json.append("]}").toString();
    }
    @Override public String visit(District district) {
        StringBuilder json = new StringBuilder("{\"type\":\"District\",\"name\":\"")
                .append(escape(district.getName())).append("\",\"assets\":[");
        for (int i = 0; i < district.getAssets().size(); i++) {
            if (i > 0) json.append(',');
            json.append(district.getAssets().get(i).accept(this));
        }
        return json.append("]}").toString();
    }
    private String asset(String type, CityAsset asset) {
        return "{\"type\":\"" + type + "\",\"id\":\"" + escape(asset.getId())
                + "\",\"name\":\"" + escape(asset.getName())
                + "\",\"monthlyCost\":" + asset.getMonthlyCost() + "}";
    }
    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
    @Override public String visit(AirQualityMonitor asset) { return asset("AirQualityMonitor", asset); }
    @Override public String visit(TrafficCamera asset) { return asset("TrafficCamera", asset); }
    @Override public String visit(WaterSensor asset) { return asset("WaterSensor", asset); }
    @Override public String visit(StreetLight asset) { return asset("StreetLight", asset); }
}
