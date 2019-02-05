package chess.android.arduino.bluetooth.coordinates;

public class Declination {
    private int degree;
    private int minute;
    private int second;

    public Declination(int degree, int minute, int second) {
        this.degree = degree;
        this.minute = minute;
        this.second = second;
    }

    public int getSecond() {
        return second;
    }

    public int getMinute() {
        return minute;
    }

    public int getDegree() {
        return degree;
    }

    public double toDegrees() {
        return (((double)degree*3600.0d)+((double)minute*60.0d)+(double)second)/3600.0d;
    }
}
