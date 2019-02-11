package chess.android.arduino.telescope.coordinates;

import chess.android.arduino.telescope.MainActivity;

public class EquatorialCoordinates {
    private Declination declination;
    private HourAngle hourAngle;

    public EquatorialCoordinates(MainActivity activity) {
        this(activity.getDeclination(), activity.getHourAngle());
    }

    private EquatorialCoordinates(Declination declination, HourAngle hourAngle) {
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
