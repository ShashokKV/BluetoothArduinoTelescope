package chess.android.arduino.telescope

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import chess.android.arduino.telescope.coordinates.Declination
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates
import chess.android.arduino.telescope.coordinates.HourAngle
import chess.android.arduino.telescope.filters.MinMaxInputFilter
import chess.android.arduino.telescope.threads.BluetoothThread
import chess.android.arduino.telescope.threads.GotoThread
import io.github.controlwear.virtual.joystick.android.JoystickView
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.cos
import kotlin.math.sin


/**
 * Simple UI demonstrating how to connect to a Bluetooth device,
 * send and receive messages using Handlers, and update the UI.
 */
class MainActivity : Activity() {
    var connected = false
    private var gotoOn = false

    // Handler for writing messages to the Bluetooth connection
    private var btWriteHandler: Handler? = null
    var gotoHandler: Handler? = null

    /**
     * Launch the Bluetooth thread.
     */
    @SuppressLint("SetTextI18n")
    fun connectButtonPressed(@Suppress("UNUSED_PARAMETER") v: View?) {
        Log.v(TAG, "Connect button pressed.")

        // Initialize the Bluetooth thread, passing in a MAC address
        // and a Handler that will receive incoming messages
        if (btt == null) btt = BluetoothThread(address, BTHandler(this))

        // Get the handler that is used to send messages
        btWriteHandler = btt!!.writeHandler

        // Run the thread
        btt!!.start()
        Toast.makeText(applicationContext, "Connecting...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Kill the Bluetooth thread.
     */
    fun disconnectButtonPressed(@Suppress("UNUSED_PARAMETER") v: View?) {
        Log.v(TAG, "Disconnect button pressed.")
        if (btt != null) {
            btt!!.interrupt()
            btt = null
        }
        if (gotoThread != null) {
            gotoThread!!.interrupt()
            gotoThread = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    101
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState != null) {
            connected = savedInstanceState.getBoolean(CONNECTED_STATE)
            gotoOn = savedInstanceState.getBoolean(GOTO_STATE)
        }
        val connectButton = findViewById<View>(R.id.connectButton)
        val disconnectButton = findViewById<View>(R.id.disconnectButton)
        if (connected) {
            connectButton.isEnabled = false
            disconnectButton.isEnabled = true
        } else {
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
        }

        val enterCoordinatesButton = findViewById<View>(R.id.enterCoordinatesButton)
        enterCoordinatesButton.setOnClickListener {
            enterCoordinatesDialog()
        }

        if (btt != null) {
            btWriteHandler = btt!!.writeHandler
            btt!!.setReadHandler(BTHandler(this))
        }
        if (gotoThread != null) {
            gotoHandler = gotoThread!!.writeHandler
            gotoThread!!.setActivityHandler(GotoHandler(this))
            gotoThread!!.setBluetoothHandler(btt!!.writeHandler)
        }

        initCoordinateFields()
        val motorButton = findViewById<ToggleButton>(R.id.motorButton)
        motorButton.isEnabled = connected
        motorButton.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            val msg = Message.obtain()
            if (isChecked) {
                msg.obj = "E1"
            } else {
                msg.obj = "E0"
            }
            btWriteHandler!!.sendMessage(msg)
        }

        val gotoToggle = findViewById<ToggleButton>(R.id.gotoButton)
        gotoToggle.isEnabled = connected
        gotoToggle.isChecked = gotoOn
        gotoToggle.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                beginTrack()
            } else {
                if (gotoThread != null) gotoThread!!.interrupt()
                gotoThread = null
            }
            gotoOn = isChecked
        }

        val updatePositionButton = findViewById<Button>(R.id.updatePosButton)
        updatePositionButton.isEnabled = connected
        updatePositionButton.setOnClickListener {
            var msg = Message.obtain()
            msg.obj = "T"
            btWriteHandler!!.sendMessage(msg)

            val gpsTracker = GpsTracker(this@MainActivity)
            if (gpsTracker.canGetLocation()) {
                val latitude = gpsTracker.latitude
                val equatorialCoordinates = EquatorialCoordinates(this)
                msg = Message.obtain()
                msg.obj = equatorialCoordinates.getHorizontalCoordinates(latitude)
                btWriteHandler!!.sendMessage(msg)
                gpsTracker.stopUsingGPS()
                Toast.makeText(this@MainActivity, "Position updated", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "Can't get current location!", Toast.LENGTH_LONG)
                    .show()
            }
        }

        val joystick: JoystickView = findViewById(R.id.joystickView)
        joystick.isEnabled = connected
        joystick.setOnMoveListener { angle: Int, strength: Int ->
            val msg = Message.obtain()
            msg.obj = parseJoystickInput(angle, strength)
            btWriteHandler!!.sendMessage(msg)
        }
    }

    private fun parseJoystickInput(angle: Int, strength: Int): String {
        val x: Int = (strength * X_MAX * cos(Math.toRadians(angle.toDouble())) / 100).toInt()
        val y: Int = (strength * Y_MAX * sin(Math.toRadians(angle.toDouble())) / 100).toInt()
        return "X$x#Y$y"
    }

    private fun beginTrack() {
        if (gotoThread != null) gotoThread!!.interrupt()
        val gpsTracker = GpsTracker(this@MainActivity)
        val latitude: Double = if (gpsTracker.canGetLocation()) {
            gpsTracker.latitude
        } else {
            Toast.makeText(this@MainActivity, "Can't get current location!", Toast.LENGTH_LONG)
                .show()
            return
        }
        val equatorialCoordinates = EquatorialCoordinates(this)
        gotoThread =
            GotoThread(latitude, equatorialCoordinates, GotoHandler(this), btt!!.writeHandler)
        gotoThread!!.start()
        gotoHandler = gotoThread!!.writeHandler
        gpsTracker.stopUsingGPS()
    }

    private fun enterCoordinatesDialog() {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        alertDialog.setMessage("Enter coordinates")

        val input = EditText(this@MainActivity)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp
        alertDialog.setView(input)

        alertDialog.setPositiveButton("YES") { _, _ ->
            val coordinates = input.text.toString()
            try {
                parseCoordinates(coordinates)
            }catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        alertDialog.setNegativeButton("NO") { dialog, _ -> dialog.cancel() }

        alertDialog.show()
    }

    private fun parseCoordinates(coordinates: String) {
        val regex =
            "([0-9]{1,2})h([0-9]{1,2})m([0-9]{1,2})s/([+,-])([0-9]{1,2}).([0-9]{1,2}).([0-9]{1,2})."
        val pattern: Pattern = Pattern.compile(regex)
        val matcher: Matcher = pattern.matcher(coordinates)

        while (matcher.find()) {
            val hour = matcher.group(1)
            val minute = matcher.group(2)
            val second = matcher.group(3)

            if (hour == null || minute == null || second==null) {
                Toast.makeText(this, "Can't parse hour angle: $hour h $minute m $second s", Toast.LENGTH_LONG).show()
                return
            }
            val hourAngle = HourAngle(hour.toInt(), minute.toInt(), second.toInt())

            val sign = matcher.group(4)
            var angle = matcher.group(5)
            val angleMinute = matcher.group(6)
            val angleSecond = matcher.group(7)

            if (angle == null || angleMinute == null || angleSecond==null) {
                Toast.makeText(this, "Can't parse azimuth: $angle d $angleMinute m $angleSecond s", Toast.LENGTH_LONG).show()
                return
            }

            if (sign!=null && sign=="-") {
                angle = "-$angle"
            }

            val declination = Declination(angle.toInt(), angleMinute.toInt(), angleSecond.toInt())

            this.setHourAngle(hourAngle, getString(R.string.user))
            this.declination = declination

        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(CONNECTED_STATE, connected)
        savedInstanceState.putBoolean(GOTO_STATE, gotoOn)
        super.onSaveInstanceState(savedInstanceState)
    }

    val hourAngle: HourAngle
        get() {
            val hour: Int = findViewById<TextView>(R.id.hAngHour).text.toString().toInt()
            val minute: Int = findViewById<TextView>(R.id.hAngMinute).text.toString().toInt()
            val second: Int = findViewById<TextView>(R.id.hAngSecond).text.toString().toInt()
            return HourAngle(hour, minute, second)
        }

    fun setHourAngle(hourAngle: HourAngle, tag: String?) {
        setToTextView(findViewById(R.id.hAngHour), hourAngle.hour, tag)
        setToTextView(findViewById(R.id.hAngMinute), hourAngle.minute, tag)
        setToTextView(findViewById(R.id.hAngSecond), hourAngle.second, tag)
    }

    var declination: Declination
        get() {
            val degree: Int = getFromTextView(findViewById(R.id.declDegree))
            val minute: Int = getFromTextView(findViewById(R.id.declMinute))
            val second: Int = getFromTextView(findViewById(R.id.declSecond))
            return Declination(degree, minute, second)
        }
        set(declination) {
            setToTextView(findViewById(R.id.declDegree), declination.degree)
            setToTextView(findViewById(R.id.declMinute), declination.minute)
            setToTextView(findViewById(R.id.declSecond), declination.second)
        }

    private fun getFromTextView(textView: TextView): Int {
        val text = textView.text.toString()
        return if (text.isEmpty()) 0 else text.toInt()
    }

    private fun setToTextView(textView: TextView, value: Int) {
        this.setToTextView(textView, value, null)
    }

    private fun setToTextView(textView: TextView, value: Int, tag: String?) {
        //no tag - not changing by user
        if (textView.tag == null || textView.tag.toString() != getString(R.string.user)) {
            val oldValue = textView.text.toString()
            val newValue: String = String.format(Locale.ENGLISH, "%d", value)
            if (oldValue != newValue) {

                //is changing by thread
                textView.tag = tag
                textView.text = newValue
                textView.tag = null
            }
        }
    }

    private fun initCoordinateFields() {
        var editText = findViewById<EditText>(R.id.declDegree)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(MinMaxInputFilter(-89, 89))
        findViewById<View>(R.id.buttonDeclDegreeUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonDeclDegreeDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        editText = findViewById(R.id.hAngHour)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(MinMaxInputFilter(0, 23))
        findViewById<View>(R.id.buttonHourUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonHourDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        val minSecInputFilter = MinMaxInputFilter(0, 59)
        editText = findViewById(R.id.hAngMinute)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(minSecInputFilter)
        findViewById<View>(R.id.buttonMinuteUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonMinuteDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        editText = findViewById(R.id.hAngSecond)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(minSecInputFilter)
        findViewById<View>(R.id.buttonSecondUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonSecondDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        editText = findViewById(R.id.declMinute)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(minSecInputFilter)
        findViewById<View>(R.id.buttonDeclMinuteUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonDeclMinuteDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        editText = findViewById(R.id.declSecond)
        editText.addTextChangedListener(EditTextWatcher(editText, this))
        editText.filters = arrayOf<InputFilter>(minSecInputFilter)
        findViewById<View>(R.id.buttonDeclSecondUp).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
        findViewById<View>(R.id.buttonDeclSecondDown).setOnClickListener(
            ArrowButtonOnClickListener(this)
        )
    }

    internal inner class EditTextWatcher(
        private val editText: EditText,
        private val mainActivity: MainActivity
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (editText.tag != null && editText.tag.toString() == "thread") return
            editText.tag = "user"
            if (s.toString() == "" || s.toString() == "-") return
            if (gotoOn) {
                val eqCoords = EquatorialCoordinates(mainActivity)
                val message = Message.obtain()
                message.obj = eqCoords
                gotoHandler!!.sendMessage(message)
            }
            editText.tag = null
        }

        override fun afterTextChanged(s: Editable) {}

    }

    companion object {
        // Tag for logging
        private const val TAG = "MainActivity"

        // MAC address of remote Bluetooth device
        // Replace this with the address of your own module
        private const val address = "98:D3:81:FD:57:31"
        private const val CONNECTED_STATE = "connectedState"
        private const val GOTO_STATE = "gotoState"
        private const val X_MAX = 10
        private const val Y_MAX = 10

        // The thread that does all the work
        var btt: BluetoothThread? = null
        var gotoThread: GotoThread? = null
    }
}