package kz.edu.smartcity;

public final class CityEvent {
    private final String type;
    private final String district;
    private final String message;

    public CityEvent(String type, String district, String message) {
        this.type = type;
        this.district = district;
        this.message = message;
    }

    public String getType() { return type; }
    public String getDistrict() { return district; }
    public String getMessage() { return message; }
}
