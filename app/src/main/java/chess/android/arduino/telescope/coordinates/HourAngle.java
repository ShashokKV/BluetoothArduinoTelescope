package chess.android.arduino.telescope.coordinates;

public class HourAngle  extends Coordinate{


    public HourAngle(int hour, int minute, int second) {
        super(hour, minute, second);
    }

    @Override
    public double toDegrees() {
        return (((this.getHour()*3600.0d)+
                (this.getMinute()*60.0d)
                +(this.getSecond()*1.0d))*15.0d)
                /3600.0d;
    }
}
