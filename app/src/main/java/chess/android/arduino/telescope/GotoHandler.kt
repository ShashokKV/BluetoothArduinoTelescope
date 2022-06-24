package chess.android.arduino.telescope

import android.os.Handler
import android.os.Message
import chess.android.arduino.telescope.coordinates.HourAngle
import java.lang.ref.WeakReference

class GotoHandler(activity: MainActivity) : Handler() {
    private val mainActivityWeakReference: WeakReference<MainActivity> = WeakReference(activity)
    override fun handleMessage(msg: Message) {
        val hourAngle = msg.obj as HourAngle
        val mainActivity = mainActivityWeakReference.get()
        mainActivity!!.setHourAngle(hourAngle, mainActivity.getString(R.string.thread))
    }
}