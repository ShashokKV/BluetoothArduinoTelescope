package chess.android.arduino.telescope.coordinates;

import java.time.LocalTime;

public abstract class Coordinate {
    private int degree;
    private LocalTime timer;

    Coordinate(int hour, int minute, int second) {
        if (hour>24 || hour<0 || minute>59 || minute<0 || second>59 || second<0) {
            throw new IllegalArgumentException(String.format("Illegal time: %d, %d, %d", hour, minute, second));
        }
        timer = LocalTime.of(hour, minute, second);
    }

    public int getDegree() {
        return degree;
    }

    void setDegree(int degree) {
        this.degree = degree;
    }

    public int getHour() {
        return  this.timer.getHour();
    }

    public int getMinute() {
        return this.timer.getMinute();
    }


    public int getSecond() {
        return this.timer.getSecond();
    }

    public void plusHour() {
        timer = timer.plusHours(1);
    }

    public void plusDegree() {
        if(degree!=89) degree++;
    }

    public void minusDegree() {
        if(degree!=-89) degree--;
    }


    public void minusHour() {
        timer = timer.plusHours(-1);
    }

    public void plusMinute() {
        timer = timer.plusMinutes(1);
    }

    public void minusMinute() {
        timer = timer.plusMinutes(-1);
    }

    public void  plusSecond() {
        timer = timer.plusSeconds(1);
    }

    public void minusSecond() {
        timer = timer.plusSeconds(-1);
    }

    public abstract double toDegrees();




}
