package chess.android.arduino.telescope;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;

import chess.android.arduino.telescope.coordinates.Declination;
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates;
import chess.android.arduino.telescope.coordinates.HourAngle;
import chess.android.arduino.telescope.filters.InputFilterMinMax;
import chess.android.arduino.telescope.threads.BluetoothThread;
import chess.android.arduino.telescope.threads.GotoThread;
import chess.android.arduino.telescope.threads.JoystickThread;
import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * Simple UI demonstrating how to connect to a Bluetooth device,
 * send and receive messages using Handlers, and update the UI.
 */
public class MainActivity extends Activity {

    public static final int DEFAULT_STRENGTH = 0;
    // Tag for logging
    private static final String TAG = "MainActivity";
    // MAC address of remote Bluetooth device
    // Replace this with the address of your own module
    private static final String address = "00:06:66:66:33:89";

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
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
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

        InputFilterMinMax secMinInputFilter = new InputFilterMinMax(0, 59);
        EditText editText = findViewById(R.id.hAngHour);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{new InputFilterMinMax(0, 23)});
        editText = findViewById(R.id.hAngMinute);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{secMinInputFilter});
        editText = findViewById(R.id.hAngSecond);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{secMinInputFilter});
        editText = findViewById(R.id.declDegree);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{new InputFilterMinMax(-89, 89)});
        editText = findViewById(R.id.declMinute);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{secMinInputFilter});
        editText = findViewById(R.id.declSecond);
        editText.addTextChangedListener(new EditTextWatcher(editText));
        editText.setFilters(new InputFilter[]{secMinInputFilter});

        ToggleButton gotoToggle = findViewById(R.id.gotoButton);
        gotoToggle.setEnabled(connected);
        gotoToggle.setChecked(gotoOn);
        gotoToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (joystickThread!=null) joystickThread.interrupt();
                JoystickView joystick = findViewById(R.id.joystickView);
                if (isChecked) {
                    beginTrack();
                    joystick.setEnabled(false);
                }else{
                    if (gotoThread!=null) gotoThread.interrupt();
                    gotoThread=null;
                    joystick.setEnabled(true);
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
                String coordinates = JoystickThread.parseJoystickInput(angle, strength);
                msg.obj = coordinates;
                if (joystickThread!=null) joystickThread.interrupt();
                if (angle != DEFAULT_ANGLE && strength != DEFAULT_STRENGTH) {
                    joystickThread = new JoystickThread(btWriteHandler, msg);
                    joystickThread.start();
                }else{
                    btWriteHandler.sendMessage(msg);
                }

                TextView tv = findViewById(R.id.statusText);
                tv.setText(coordinates);
            }
        });
    }

    private void beginTrack() {
        if (gotoThread!=null) gotoThread.interrupt();
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        double latitude;
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
        }else{
            Toast.makeText(MainActivity.this, "Can't get current location!", Toast.LENGTH_LONG).show();
            return;
        }
        EquatorialCoordinates equatorialCoordinates = new EquatorialCoordinates(getDeclination(), getHourAngle());
        gotoThread = new GotoThread(latitude, equatorialCoordinates, new GotoHandler(this), btt.getWriteHandler());
        gotoThread.start();
        gotoHandler = gotoThread.getWriteHandler();
        gpsTracker.stopUsingGPS();
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

    class EditTextWatcher implements TextWatcher {
        private EditText editText;

        EditTextWatcher(EditText editText) {
            this.editText = editText;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (editText.getTag()!=null && editText.getTag().toString().equals("thread")) return;
            editText.setTag("user");
            if (s.toString().equals("")||s.toString().equals("-")) return;
            if (gotoOn) {
                EquatorialCoordinates eqCoords = new EquatorialCoordinates(getDeclination(), getHourAngle());
                Message message = Message.obtain();
                message.obj = eqCoords;
                gotoHandler.sendMessage(message);
            }
            editText.setTag(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    static class GotoHandler extends Handler {
        private final WeakReference<MainActivity> bluetoothActivityWeakReference;

        GotoHandler(MainActivity activity) {
            bluetoothActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HourAngle hourAngle = (HourAngle) msg.obj;
            MainActivity mainActivity = bluetoothActivityWeakReference.get();
            hourAngle.setToView(mainActivity);
        }
    }

    static class BTHandler extends Handler {
        private final WeakReference<MainActivity> bActivity;

        BTHandler(MainActivity activity) {
            bActivity = new WeakReference<>(activity);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message message) {

            String s = (String) message.obj;

            MainActivity mainActivity = bActivity.get();

            // Do something with the message
            switch (s) {
                case "CONNECTED": {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Connected.", Toast.LENGTH_SHORT).show();
                    mainActivity.findViewById(R.id.joystickView).setEnabled(true);
                    mainActivity.findViewById(R.id.gotoButton).setEnabled(true);
                    mainActivity.connected = true;
                    break;
                }
                case "DISCONNECTED": {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Disconnected.", Toast.LENGTH_SHORT).show();
                    mainActivity.findViewById(R.id.joystickView).setEnabled(false);
                    mainActivity.findViewById(R.id.gotoButton).setEnabled(false);
                    mainActivity.connected = false;
                    if (joystickThread!=null) joystickThread.interrupt();
                    break;
                }
                case "CONNECTION FAILED": {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Connection failed!.", Toast.LENGTH_SHORT).show();
                    btt = null;
                    mainActivity.connected = false;
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }
}
