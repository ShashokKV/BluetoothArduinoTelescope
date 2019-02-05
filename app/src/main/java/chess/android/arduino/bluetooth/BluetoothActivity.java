package chess.android.arduino.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.Locale;

import chess.android.arduino.bluetooth.coordinates.Declination;
import chess.android.arduino.bluetooth.coordinates.EquatorialCoordinates;
import chess.android.arduino.bluetooth.coordinates.HourAngle;
import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * Simple UI demonstrating how to connect to a Bluetooth device,
 * send and receive messages using Handlers, and update the UI.
 */
public class BluetoothActivity extends Activity {

    public static final int DEFAULT_STRENGTH = 0;
    // Tag for logging
    private static final String TAG = "BluetoothActivity";
    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    private static final String address = "00:06:66:66:33:89";

    private static final int Y_MAX = 100;
    private static final int X_MAX = 100;
    private static final String CONNECTED_STATE = "connectedState";
    private static final String GOTO_STATE = "gotoState";
    public static final int DEFAULT_ANGLE = 0;
    private boolean connected = false;
    private boolean gotoOn = false;

    // The thread that does all the work
    static BluetoothThread btt;
    static GotoThread gotoThread;
    static Thread joystickThread;

    // Handler for writing messages to the Bluetooth connection
    Handler btWriteHandler;
    Handler gotoHandler;

    BluetoothActivity bluetoothActivity;

    /**
     * Launch the Bluetooth thread.
     */
    @SuppressLint("SetTextI18n")
    public void connectButtonPressed(View v) {
        Log.v(TAG, "Connect button pressed.");

        // Initialize the Bluetooth thread, passing in a MAC address
        // and a Handler that will receive incoming messages
        if (btt==null) btt = new BluetoothThread(address, new BTHandler(this));

        // Get the handler that is used to send messages
        btWriteHandler = btt.getWriteHandler();

        // Run the thread
        btt.start();

        Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
    }

    /**
     * Kill the Bluetooth thread.
     */
    public void disconnectButtonPressed(View v) {
        Log.v(TAG, "Disconnect button pressed.");

        if(btt != null) {
            btt.interrupt();
            btt = null;
        }
        if (gotoThread != null) {
            gotoThread.interrupt();
            gotoThread = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothActivity = this;
        setContentView(R.layout.activity_bluetooth);

        if (savedInstanceState!=null) {
            connected = savedInstanceState.getBoolean(CONNECTED_STATE);
            gotoOn = savedInstanceState.getBoolean(GOTO_STATE);
        }

        if (joystickThread!=null) joystickThread.interrupt();

        if (btt!=null) {
            btWriteHandler = btt.getWriteHandler();
            btt.setReadHandler(new BTHandler(this));
        }

        if (gotoThread!=null) {
            gotoHandler = gotoThread.getWriteHandler();
            gotoThread.setActivityHandler(new GotoHandler(this));
            gotoThread.setBluetoothHandler(btt.getWriteHandler());
        }

        ToggleButton gotoToggle = findViewById(R.id.gotoButton);
        gotoToggle.setEnabled(connected);
        gotoToggle.setChecked(gotoOn);
        gotoToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (joystickThread!=null) joystickThread.interrupt();
                if (isChecked) {
                    beginTrack();
                    JoystickView joystick = findViewById(R.id.joystickView);
                    joystick.setEnabled(false);
                }
                gotoOn = isChecked;
            }
        });

        JoystickView joystick = findViewById(R.id.joystickView);
        joystick.setEnabled(connected && !gotoOn);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                Message msg = Message.obtain();
                String coordinates = parseJoystickInput(angle, strength);
                msg.obj = coordinates;
                if (joystickThread!=null) joystickThread.interrupt();
                if (angle != DEFAULT_ANGLE && strength != DEFAULT_STRENGTH) {
                    joystickThread = new Thread(new JoystickRunner(btWriteHandler, msg));
                    joystickThread.start();
                }else{
                    btWriteHandler.sendMessage(msg);
                }

                TextView tv = findViewById(R.id.statusText);
                tv.setText(coordinates);
            }
        });

        EditText editText = findViewById(R.id.hAngHour);
        editText.addTextChangedListener(new AngleTextWatcher());
        editText = findViewById(R.id.hAngMinute);
        editText.addTextChangedListener(new AngleTextWatcher());
        editText = findViewById(R.id.hAngSecond);
        editText.addTextChangedListener(new AngleTextWatcher());
        editText = findViewById(R.id.declDegree);
        editText.addTextChangedListener(new AngleTextWatcher());
        editText = findViewById(R.id.declMinute);
        editText.addTextChangedListener(new AngleTextWatcher());
        editText = findViewById(R.id.declSecond);
        editText.addTextChangedListener(new AngleTextWatcher());
    }

    class AngleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (bluetoothActivity.gotoOn) bluetoothActivity.beginTrack();
        }
    }

    private String parseJoystickInput(int angle, int strength) {
        int x, y;
        x = Double.valueOf((strength*X_MAX*Math.cos(Math.toRadians(angle)))/100).intValue();
        y = Double.valueOf((strength*Y_MAX*Math.sin(Math.toRadians(angle)))/100).intValue();

        return "X"+x+"#"+"Y"+y;
    }

    void beginTrack() {
        //if (gotoThread!=null) gotoThread.interrupt();
        EquatorialCoordinates equatorialCoordinates = new EquatorialCoordinates(getDeclination(), getHourAngle());
        gotoThread = new GotoThread(equatorialCoordinates, new GotoHandler(this), btt.getWriteHandler());
        gotoThread.start();
    }

    private HourAngle getHourAngle() {
        int hour, minute, second;
        TextView hourTextView = findViewById(R.id.hAngHour);
        hour = Integer.parseInt(hourTextView.getText().toString());
        TextView minuteTextView = findViewById(R.id.hAngMinute);
        minute = Integer.parseInt(minuteTextView.getText().toString());
        TextView secondsTextView = findViewById(R.id.hAngSecond);
        second = Integer.parseInt(secondsTextView.getText().toString());
        return new HourAngle(hour,minute,second);
    }

    private void setHourAngle(HourAngle hourAngle) {
        TextView hourTextView = findViewById(R.id.hAngHour);
        TextView minuteTextView = findViewById(R.id.hAngMinute);
        TextView secondsTextView = findViewById(R.id.hAngSecond);
        hourTextView.setText(String.format(Locale.ENGLISH,"%d", hourAngle.getTimer().getHour()));
        minuteTextView.setText(String.format(Locale.ENGLISH,"%d", hourAngle.getTimer().getMinute()));
        secondsTextView.setText(String.format(Locale.ENGLISH,"%d", hourAngle.getTimer().getSecond()));
    }

    private Declination getDeclination() {
        int degree, minute, second;
        TextView hourTextView = findViewById(R.id.declDegree);
        degree = Integer.parseInt(hourTextView.getText().toString());
        TextView minuteTextView = findViewById(R.id.declMinute);
        minute = Integer.parseInt(minuteTextView.getText().toString());
        TextView secondsTextView = findViewById(R.id.declSecond);
        second = Integer.parseInt(secondsTextView.getText().toString());
        return new Declination(degree,minute,second);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(CONNECTED_STATE, connected);
        savedInstanceState.putBoolean(GOTO_STATE, gotoOn);
        super.onSaveInstanceState(savedInstanceState);
    }

    static class GotoHandler extends Handler {
        private final WeakReference<BluetoothActivity> bluetoothActivityWeakReference;

        GotoHandler(BluetoothActivity activity) {
            bluetoothActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HourAngle hourAngle = (HourAngle) msg.obj;
            BluetoothActivity bluetoothActivity = bluetoothActivityWeakReference.get();
            bluetoothActivity.setHourAngle(hourAngle);
        }
    }

    static class BTHandler extends Handler {
        private final WeakReference<BluetoothActivity> bActivity;

        BTHandler(BluetoothActivity activity) {
            bActivity = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message message) {

            String s = (String) message.obj;

            BluetoothActivity bluetoothActivity = bActivity.get();

            // Do something with the message
            switch (s) {
                case "CONNECTED": {
                    Toast.makeText(bluetoothActivity.getApplicationContext(),
                            "Connected.", Toast.LENGTH_SHORT).show();
                    bluetoothActivity.findViewById(R.id.joystickView).setEnabled(true);
                    bluetoothActivity.findViewById(R.id.gotoButton).setEnabled(true);
                    bluetoothActivity.connected = true;
                    break;
                }
                case "DISCONNECTED": {
                    Toast.makeText(bluetoothActivity.getApplicationContext(),
                            "Disconnected.", Toast.LENGTH_SHORT).show();
                    bluetoothActivity.findViewById(R.id.joystickView).setEnabled(false);
                    bluetoothActivity.findViewById(R.id.gotoButton).setEnabled(false);
                    bluetoothActivity.connected = false;
                    if (joystickThread!=null) joystickThread.interrupt();
                    break;
                }
                case "CONNECTION FAILED": {
                    Toast.makeText(bluetoothActivity.getApplicationContext(),
                            "Connection failed!.", Toast.LENGTH_SHORT).show();
                    btt = null;
                    bluetoothActivity.connected = false;
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
