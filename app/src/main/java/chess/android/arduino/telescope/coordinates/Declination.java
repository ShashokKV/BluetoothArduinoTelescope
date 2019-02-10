package chess.android.arduino.telescope.coordinates;

public class Declination {
    private int degree;
    private int minute;
    private int second;

    public Declination(int degree, int minute, int second) {
        this.degree = degree;
        this.minute = minute;
        this.second = second;
    }

    public double toDegrees() {
        return ((degree*3600.0d)+(minute*60.0d)+second*1.0d)/3600.0d;
    }
}
