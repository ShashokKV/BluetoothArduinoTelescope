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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;
import java.util.Locale;

import chess.android.arduino.telescope.coordinates.Coordinate;
import chess.android.arduino.telescope.coordinates.Declination;
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates;
import chess.android.arduino.telescope.coordinates.HourAngle;
import chess.android.arduino.telescope.filters.MinMaxInputFilter;
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
        if (btt == null) btt = new BluetoothThread(address, new BTHandler(this));

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

        if (btt != null) {
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
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(CONNECTED_STATE);
            gotoOn = savedInstanceState.getBoolean(GOTO_STATE);
        }

        View connectButton = findViewById(R.id.connectButton);
        View disconnectButton = findViewById(R.id.disconnectButton);

        if (connected) {
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
        } else {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
        }

        if (joystickThread != null) joystickThread.interrupt();

        if (btt != null) {
            btWriteHandler = btt.getWriteHandler();
            btt.setReadHandler(new BTHandler(this));
        }

        if (gotoThread != null) {
            gotoHandler = gotoThread.getWriteHandler();
            gotoThread.setActivityHandler(new GotoHandler(this));
            gotoThread.setBluetoothHandler(btt.getWriteHandler());
        }

        initCoordinateFields();

        ToggleButton gotoToggle = findViewById(R.id.gotoButton);
        gotoToggle.setEnabled(connected);
        gotoToggle.setChecked(gotoOn);
        gotoToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                beginTrack();
            } else {
                if (gotoThread != null) gotoThread.interrupt();
                gotoThread = null;
            }
            gotoOn = isChecked;
        });

        JoystickView joystick = findViewById(R.id.joystickView);
        joystick.setEnabled(connected);
        joystick.setOnMoveListener((angle, strength) -> {
            Message msg = Message.obtain();
            msg.obj = JoystickThread.parseJoystickInput(angle, strength);
            if (joystickThread != null) joystickThread.interrupt();
            if (angle != DEFAULT_ANGLE && strength != DEFAULT_STRENGTH) {
                joystickThread = new JoystickThread(btWriteHandler, msg);
                joystickThread.start();
            } else {
                btWriteHandler.sendMessage(msg);
            }
        });

    }

    private void beginTrack() {
        if (gotoThread != null) gotoThread.interrupt();
        GpsTracker gpsTracker = new GpsTracker(MainActivity.this);
        double latitude;
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
        } else {
            Toast.makeText(MainActivity.this, "Can't get current location!", Toast.LENGTH_LONG).show();
            return;
        }
        EquatorialCoordinates equatorialCoordinates = new EquatorialCoordinates(this);
        gotoThread = new GotoThread(latitude, equatorialCoordinates, new GotoHandler(this), new StatusHandler(this), btt.getWriteHandler());
        gotoThread.start();
        gotoHandler = gotoThread.getWriteHandler();
        gpsTracker.stopUsingGPS();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(CONNECTED_STATE, connected);
        savedInstanceState.putBoolean(GOTO_STATE, gotoOn);
        super.onSaveInstanceState(savedInstanceState);
    }

    public HourAngle getHourAngle() {
        int hour, minute, second;
        hour = Integer.parseInt(this.<TextView>findViewById(R.id.hAngHour).getText().toString());
        minute = Integer.parseInt(this.<TextView>findViewById(R.id.hAngMinute).getText().toString());
        second = Integer.parseInt(this.<TextView>findViewById(R.id.hAngSecond).getText().toString());
        return new HourAngle(hour, minute, second);
    }

    public void setHourAngle(HourAngle hourAngle, String tag) {
        setToTextView(findViewById(R.id.hAngHour), hourAngle.getHour(), tag);
        setToTextView(findViewById(R.id.hAngMinute), hourAngle.getMinute(), tag);
        setToTextView(findViewById(R.id.hAngSecond), hourAngle.getSecond(), tag);
    }

    public Declination getDeclination() {
        int degree, minute, second;
        findViewById(R.id.declDegree);
        degree = getFromTextView(findViewById(R.id.declDegree));
        minute = getFromTextView(findViewById(R.id.declMinute));
        second = getFromTextView(findViewById(R.id.declSecond));
        return new Declination(degree, minute, second);
    }

    public void setDeclination(Declination declination) {
        setToTextView(findViewById(R.id.declDegree), declination.getDegree());
        setToTextView(findViewById(R.id.declMinute), declination.getMinute());
        setToTextView(findViewById(R.id.declSecond), declination.getSecond());
    }

    private int getFromTextView(TextView textView) {
        String text = textView.getText().toString();
        if (text.isEmpty()) return 0;
        return Integer.parseInt(text);
    }

    private void setToTextView(TextView textView, int value) {
        this.setToTextView(textView,value, null);
    }

    private void setToTextView(TextView textView, int value, String tag) {
        //no tag - not changing by user
        if (textView.getTag()==null||!textView.getTag().toString().equals("user")) {
            String oldValue = textView.getText().toString();
            String newValue = String.format(Locale.ENGLISH, "%d", value);
            if (!oldValue.equals(newValue)) {

                //is changing by thread
                textView.setTag(tag);
                textView.setText(newValue);
                textView.setTag(null);
            }
        }
    }

    private void initCoordinateFields() {
        EditText editText = findViewById(R.id.declDegree);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{new MinMaxInputFilter(-89, 89)});
        findViewById(R.id.buttonDeclDegreeUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonDeclDegreeDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );

        editText = findViewById(R.id.hAngHour);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{new MinMaxInputFilter(0, 23)});
        findViewById(R.id.buttonHourUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonHourDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );

        MinMaxInputFilter minSecInputFilter = new MinMaxInputFilter(0, 59);
        editText = findViewById(R.id.hAngMinute);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{minSecInputFilter});
        findViewById(R.id.buttonMinuteUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonMinuteDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );

        editText = findViewById(R.id.hAngSecond);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{minSecInputFilter});
        findViewById(R.id.buttonSecondUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonSecondDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );

        editText = findViewById(R.id.declMinute);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{minSecInputFilter});
        findViewById(R.id.buttonDeclMinuteUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonDeclMinuteDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );

        editText = findViewById(R.id.declSecond);
        editText.addTextChangedListener(new EditTextWatcher(editText, this));
        editText.setFilters(new InputFilter[]{minSecInputFilter});
        findViewById(R.id.buttonDeclSecondUp).setOnClickListener(
                new ArrowButtonOnClickListener(this));
        findViewById(R.id.buttonDeclSecondDown).setOnClickListener(
                new ArrowButtonOnClickListener(this)
        );
    }

    class EditTextWatcher implements TextWatcher {
        private EditText editText;
        private MainActivity mainActivity;

        EditTextWatcher(EditText editText, MainActivity mainActivity) {
            this.mainActivity = mainActivity;
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (editText.getTag() != null && editText.getTag().toString().equals("thread")) return;
            editText.setTag("user");
            if (s.toString().equals("") || s.toString().equals("-")) return;
            if (gotoOn) {
                EquatorialCoordinates eqCoords = new EquatorialCoordinates(mainActivity);
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
        private final WeakReference<MainActivity> mainActivityWeakReference;

        GotoHandler(MainActivity activity) {
            mainActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            HourAngle hourAngle = (HourAngle) msg.obj;
            MainActivity mainActivity = mainActivityWeakReference.get();
            mainActivity.setHourAngle(hourAngle, "thread");
        }
    }

    static class StatusHandler extends Handler {
        private final WeakReference<MainActivity> mainActivityWeakReference;

        StatusHandler(MainActivity mainActivity) {
            mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mainActivityWeakReference.get();
            mainActivity.<TextView>findViewById(R.id.statusView).setText(msg.obj.toString());
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
                    mainActivity.findViewById(R.id.connectButton).setEnabled(false);
                    mainActivity.findViewById(R.id.disconnectButton).setEnabled(true);
                    mainActivity.connected = true;
                    break;
                }
                case "DISCONNECTED": {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Disconnected.", Toast.LENGTH_SHORT).show();
                    mainActivity.findViewById(R.id.joystickView).setEnabled(false);
                    mainActivity.findViewById(R.id.gotoButton).setEnabled(false);
                    mainActivity.findViewById(R.id.connectButton).setEnabled(true);
                    mainActivity.findViewById(R.id.disconnectButton).setEnabled(false);
                    mainActivity.connected = false;
                    if (joystickThread != null) joystickThread.interrupt();
                    break;
                }
                case "CONNECTION FAILED": {
                    Toast.makeText(mainActivity.getApplicationContext(),
                            "Connection failed!.", Toast.LENGTH_SHORT).show();
                    btt = null;
                    mainActivity.findViewById(R.id.joystickView).setEnabled(false);
                    mainActivity.findViewById(R.id.gotoButton).setEnabled(false);
                    mainActivity.findViewById(R.id.connectButton).setEnabled(true);
                    mainActivity.findViewById(R.id.disconnectButton).setEnabled(false);
                    mainActivity.connected = false;
                    if (joystickThread != null) joystickThread.interrupt();
                    joystickThread=null;
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    class ArrowButtonOnClickListener implements View.OnClickListener {
        private MainActivity mainActivity;

        ArrowButtonOnClickListener(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void onClick(View v) {
            Coordinate coordinate;
            String viewType;
            String direction;
            String valueType;
            String[] tag = v.getTag().toString().split("#");
            viewType = tag[0];
            valueType = tag[1];
            direction = tag[2];
            if (viewType.startsWith("H")) {
                coordinate = mainActivity.getHourAngle();
            } else if (viewType.startsWith("D")) {
                coordinate = mainActivity.getDeclination();
            } else {
                throw new IllegalArgumentException("Unknown viewTye " + viewType);
            }
            switch (valueType) {
                case "H":
                    if (direction.equals("UP")) {
                        coordinate.plusHour();
                    } else {
                        coordinate.minusHour();
                    }
                    break;
                case "M":
                    if (direction.equals("UP")) {
                        coordinate.plusMinute();
                    } else {
                        coordinate.minusMinute();
                    }
                    break;
                case "S":
                    if (direction.equals("UP")) {
                        coordinate.plusSecond();
                    } else {
                        coordinate.minusSecond();
                    }
                    break;
                case "D":
                    if (direction.equals("UP")) {
                        coordinate.plusDegree();
                    } else {
                        coordinate.minusDegree();
                    }
                    break;
            }

            if (coordinate instanceof HourAngle) {
                mainActivity.setHourAngle((HourAngle) coordinate, "user");
            } else {
                mainActivity.setDeclination((Declination) coordinate);
            }
        }
    }
}
