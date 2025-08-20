package de.bund.zrb.type.emulation;

/**
 * Represent the emulation.GeolocationCoordinates struct.
 */
public class WDGeolocationCoordinates {
    private final double latitude;   // -90..90
    private final double longitude;  // -180..180
    private Double accuracy;         // >= 0 (optional, default 1.0)
    private Double altitude;         // nullable
    private Double altitudeAccuracy; // nullable
    private Double heading;          // 0..360 or null
    private Double speed;            // >=0 or null

    public WDGeolocationCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getAccuracy() { return accuracy; }
    public Double getAltitude() { return altitude; }
    public Double getAltitudeAccuracy() { return altitudeAccuracy; }
    public Double getHeading() { return heading; }
    public Double getSpeed() { return speed; }

    /** Set accuracy in meters. */
    public WDGeolocationCoordinates withAccuracy(Double accuracy) { this.accuracy = accuracy; return this; }
    public WDGeolocationCoordinates withAltitude(Double altitude) { this.altitude = altitude; return this; }
    public WDGeolocationCoordinates withAltitudeAccuracy(Double altitudeAccuracy) { this.altitudeAccuracy = altitudeAccuracy; return this; }
    public WDGeolocationCoordinates withHeading(Double heading) { this.heading = heading; return this; }
    public WDGeolocationCoordinates withSpeed(Double speed) { this.speed = speed; return this; }
}
