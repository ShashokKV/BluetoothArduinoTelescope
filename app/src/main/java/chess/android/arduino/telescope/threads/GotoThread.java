package chess.android.arduino.telescope.threads;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;

import chess.android.arduino.telescope.coordinates.Coordinate;
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates;
import chess.android.arduino.telescope.coordinates.HourAngle;

import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class GotoThread extends Thread {
    private final static int ROUND_PLACES = 5;
    private HourAngle hourAngle;
    private EquatorialCoordinates equatorialCoordinates;

    private Handler activityHandler;
    private Handler statusHandler;
    private Handler btHandler;
    private final Handler writeHandler;
    private double latitude;

    public GotoThread(double latitude, EquatorialCoordinates equatorialCoordinates, Handler activityHandler, Handler statusHandler, Handler btHandler) {
        this.activityHandler = activityHandler;
        this.statusHandler = statusHandler;
        this.btHandler = btHandler;
        this.writeHandler = new WriteHandler(this);
        this.equatorialCoordinates = equatorialCoordinates;
        this.hourAngle = equatorialCoordinates.getHourAngle();
        this.latitude = latitude;
    }

    public void setActivityHandler(Handler activityHandler) {
        this.activityHandler = activityHandler;
    }

    public void run() {
        long timeElapsed;
        while (!this.isInterrupted()) {
            timeElapsed = System.currentTimeMillis();

            Message message = Message.obtain();
            hourAngle = equatorialCoordinates.getHourAngle();
            message.obj = hourAngle;
            activityHandler.sendMessage(message);

            String horizontalCoords =  getHorizontalCoordinates(equatorialCoordinates);

            Message btCoordinatesMessage = Message.obtain();
            btCoordinatesMessage.obj = horizontalCoords;
            btHandler.sendMessage(btCoordinatesMessage);

            Message statusCoordinateMessage = Message.obtain();
            statusCoordinateMessage.obj = horizontalCoords;
            statusHandler.sendMessage(statusCoordinateMessage);

            hourAngle.plusSecond();
            equatorialCoordinates.setHourAngle(hourAngle);
            try {
                Thread.sleep(1000 - (System.currentTimeMillis() - timeElapsed));
            } catch (InterruptedException e) {
                break;
            }
        }
        Message btCoordinatesMessage = Message.obtain();
        btCoordinatesMessage.obj = "STOP#";
        btHandler.sendMessage(btCoordinatesMessage);
    }

    private String getHorizontalCoordinates(EquatorialCoordinates equatorialCoordinates) {
        Coordinate hourAngle = equatorialCoordinates.getHourAngle();
        Coordinate declination = equatorialCoordinates.getDeclination();

        double lat = toRadians(latitude);
        double decl = toRadians(declination.toDegrees());
        double hour = toRadians(hourAngle.toDegrees());

        double alt = asin((sin(decl) * sin(lat)) + (cos(decl) * cos(lat) * cos(hour)));
        double a = round(toDegrees(acos((sin(decl) - (sin(alt) * sin(lat))) / (cos(alt) * cos(lat)))));
        alt = round(toDegrees(alt));

        double az;
        if (sin(hour) < 0) {
            az = a;
        } else {
            az = 360 - a;
        }

        return "Z"+az+"#A" + alt;
    }

    private void setEquatorialCoordinates(EquatorialCoordinates equatorialCoordinates) {
        this.equatorialCoordinates = equatorialCoordinates;
    }

    public Handler getWriteHandler() {
        return writeHandler;
    }

    public void setBluetoothHandler(Handler btHandler) {
        this.btHandler = btHandler;
    }

    public static class WriteHandler extends Handler {
        private final WeakReference<GotoThread> gotoThreadWeakReference;
        WriteHandler(GotoThread gotoThread) {
            this.gotoThreadWeakReference = new WeakReference<>(gotoThread);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            GotoThread gotoThread = gotoThreadWeakReference.get();
            gotoThread.setEquatorialCoordinates((EquatorialCoordinates) msg.obj);
        }
    }

    private double round(double val) {
        BigDecimal bd = new BigDecimal(Double.toString(val));
        bd = bd.setScale(ROUND_PLACES, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}