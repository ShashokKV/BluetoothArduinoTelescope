package chess.android.arduino.telescope.coordinates;

public class Declination extends Coordinate{

    public Declination(int degree, int minute, int second) {
        super(0, minute, second);
        this.setDegree(degree);
    }

    @Override
    public void plusMinute() {
        super.plusMinute();
        if (this.getMinute()==0) {
            this.plusDegree();
        }
    }

    @Override
    public void minusMinute() {
        super.minusMinute();
        if (this.getMinute()==59) {
            this.minusDegree();
        }
    }

    @Override
    public double toDegrees() {
        return ((this.getDegree()*3600.0d)+(this.getMinute()*60.0d)+this.getSecond())/3600.0d;
    }
}
