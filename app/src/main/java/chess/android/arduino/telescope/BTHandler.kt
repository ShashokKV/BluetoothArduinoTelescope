package chess.android.arduino.telescope

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import java.lang.ref.WeakReference

class BTHandler(activity: MainActivity) : Handler() {
    private val bActivity: WeakReference<MainActivity> = WeakReference(activity)

    @SuppressLint("SetTextI18n")
    override fun handleMessage(message: Message) {
        val s = message.obj as String
        val mainActivity = bActivity.get()
        when (s) {
            "CONNECTED" -> {
                Toast.makeText(mainActivity!!.applicationContext,
                    "Connected.", Toast.LENGTH_SHORT).show()
                mainActivity.findViewById<View>(R.id.joystickView).isEnabled = true
                mainActivity.findViewById<View>(R.id.gotoButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.motorButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.calibrateButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.updatePosButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.connectButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.disconnectButton).isEnabled = true
                mainActivity.connected = true
            }
            "DISCONNECTED" -> {
                Toast.makeText(mainActivity!!.applicationContext,
                    "Disconnected.", Toast.LENGTH_SHORT).show()
                mainActivity.findViewById<View>(R.id.joystickView).isEnabled = false
                mainActivity.findViewById<View>(R.id.gotoButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.motorButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.calibrateButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.updatePosButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.connectButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.disconnectButton).isEnabled = false
                mainActivity.connected = false
            }
            "CONNECTION FAILED" -> {
                Toast.makeText(mainActivity!!.applicationContext,
                    "Connection failed!.", Toast.LENGTH_SHORT).show()
                MainActivity.btt = null
                mainActivity.findViewById<View>(R.id.joystickView).isEnabled = false
                mainActivity.findViewById<View>(R.id.gotoButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.motorButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.calibrateButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.updatePosButton).isEnabled = false
                mainActivity.findViewById<View>(R.id.connectButton).isEnabled = true
                mainActivity.findViewById<View>(R.id.disconnectButton).isEnabled = false
                mainActivity.connected = false
            }
            else -> {
            }
        }
    }

}