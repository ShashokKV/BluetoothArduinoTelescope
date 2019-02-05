package chess.android.arduino.bluetooth;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import chess.android.arduino.bluetooth.coordinates.Declination;
import chess.android.arduino.bluetooth.coordinates.EquatorialCoordinates;
import chess.android.arduino.bluetooth.coordinates.HourAngle;

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

    GotoThread(EquatorialCoordinates equatorialCoordinates, Handler activityHandler, Handler btHandler) {
        this.activityHandler = activityHandler;
        this.btHandler = btHandler;
        this.writeHandler = new WriteHandler(this);
        this.equatorialCoordinates = equatorialCoordinates;
        this.hourAngle = equatorialCoordinates.getHourAngle();
    }

    void setActivityHandler(Handler activityHandler) {
        this.activityHandler = activityHandler;
    }

    public void run() {
        new LocationListener() {
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        long timeElapsed;
        while (!this.isInterrupted()) {
            timeElapsed = System.currentTimeMillis();
            Message message = Message.obtain();
            message.obj = hourAngle;
            activityHandler.sendMessage(message);

            Message coordinatesMessage = Message.obtain();
            equatorialCoordinates.setHourAngle(hourAngle);
            coordinatesMessage.obj = getHorizontalCoordinates(equatorialCoordinates);
            btHandler.sendMessage(coordinatesMessage);
            try {
                Thread.sleep(1000 - (System.currentTimeMillis() - timeElapsed));
            } catch (InterruptedException e) {
                break;
            }

            hourAngle.setTimer(hourAngle.getTimer().plusSeconds(1));
        }
    }

    private String getHorizontalCoordinates(EquatorialCoordinates equatorialCoordinates) {
        HourAngle hourAngle = equatorialCoordinates.getHourAngle();
        Declination declination = equatorialCoordinates.getDeclination();
        double lat = toRadians(latitude);
        double decl = toRadians(declination.toDegrees());

        double hour = toRadians(hourAngle.toDegrees());
        double alt = asin(sin(decl) * sin(lat) + cos(decl) * cos(lat) * cos(hour));
        double a = toDegrees(acos((sin(decl) - sin(alt) * sin(lat)) / (cos(alt) * cos(lat))));

        double az;
        if (sin(hour) < 0) {
            az = a;
        } else {
            az = 360 - a;
        }

        alt = toDegrees(alt);

        System.out.println("alt=" + alt);
        System.out.println("Az=" + az);
        return "alt=" + alt+"&az="+az;
    }

    private void setEquatorialCoordinates(EquatorialCoordinates equatorialCoordinates) {
        this.equatorialCoordinates = equatorialCoordinates;
    }

    Handler getWriteHandler() {
        return writeHandler;
    }

    void setBluetoothHandler(Handler btHandler) {
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
