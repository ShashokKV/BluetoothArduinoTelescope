package chess.android.arduino.bluetooth.coordinates;

import java.time.LocalTime;

public class HourAngle  {
    private LocalTime timer;

    public HourAngle(int hour, int minute, int second) {
        if (hour>24 || hour<0 || minute>59 || minute<0 || second>59 || second<0) {
            throw new IllegalArgumentException(String.format("Illegal time: %d, %d, %d", hour, minute, second));
        }
        timer = LocalTime.of(hour, minute, second);
    }

    public LocalTime getTimer() {
        return timer;
    }

    public void setTimer(LocalTime timer) {
        this.timer = timer;
    }

    public double toDegrees() {
        return (((double)timer.getHour()*3600.0d)+((double)timer.getMinute()*60.0d)+(double)timer.getSecond())*15.0d/3600.0d;
    }
}
