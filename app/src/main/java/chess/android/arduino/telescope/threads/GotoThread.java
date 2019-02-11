package chess.android.arduino.telescope.threads;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

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
    private HourAngle hourAngle;
    private EquatorialCoordinates equatorialCoordinates;

    private Handler activityHandler;
    private Handler btHandler;
    private final Handler writeHandler;
    private double latitude;

    public GotoThread(double latitude, EquatorialCoordinates equatorialCoordinates, Handler activityHandler, Handler btHandler) {
        this.activityHandler = activityHandler;
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

            Message coordinatesMessage = Message.obtain();
            coordinatesMessage.obj = getHorizontalCoordinates(equatorialCoordinates);
            btHandler.sendMessage(coordinatesMessage);

            hourAngle.plusSecond();
            equatorialCoordinates.setHourAngle(hourAngle);
            try {
                Thread.sleep(1000 - (System.currentTimeMillis() - timeElapsed));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private String getHorizontalCoordinates(EquatorialCoordinates equatorialCoordinates) {
        Coordinate hourAngle = equatorialCoordinates.getHourAngle();
        Coordinate declination = equatorialCoordinates.getDeclination();

        double lat = toRadians(latitude);
        double decl = toRadians(declination.toDegrees());
        double hour = toRadians(hourAngle.toDegrees());

        double alt = asin((sin(decl) * sin(lat)) + (cos(decl) * cos(lat) * cos(hour)));
        double a = toDegrees(acos((sin(decl) - (sin(alt) * sin(lat))) / (cos(alt) * cos(lat))));
        alt = toDegrees(alt);

        double az;
        if (sin(hour) < 0) {
            az = a;
        } else {
            az = 360 - a;
        }

        return "alt=" + alt+"&az="+az;
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
}