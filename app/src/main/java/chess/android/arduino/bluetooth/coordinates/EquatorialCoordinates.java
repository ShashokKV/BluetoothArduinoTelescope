package chess.android.arduino.bluetooth.coordinates;

public class EquatorialCoordinates {
    private Declination declination;
    private HourAngle hourAngle;

    public EquatorialCoordinates(Declination declination, HourAngle hourAngle) {
        this.declination = declination;
        this.hourAngle = hourAngle;
    }

    public Declination getDeclination() {
        return declination;
    }

    public HourAngle getHourAngle() {
        return hourAngle;
    }

    public void setHourAngle(HourAngle hourAngle) {
        this.hourAngle = hourAngle;
    }
}
