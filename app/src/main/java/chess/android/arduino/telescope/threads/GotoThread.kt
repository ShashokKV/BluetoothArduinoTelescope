package chess.android.arduino.telescope.threads

import android.os.Handler
import android.os.Message
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates
import chess.android.arduino.telescope.coordinates.HourAngle
import java.lang.ref.WeakReference

class GotoThread(latitude: Double, equatorialCoordinates: EquatorialCoordinates, private var activityHandler: Handler, private var btHandler: Handler) : Thread() {
    private var hourAngle: HourAngle
    private var equatorialCoordinates: EquatorialCoordinates
    val writeHandler: Handler
    private val latitude: Double
    fun setActivityHandler(activityHandler: Handler) {
        this.activityHandler = activityHandler
    }

    override fun run() {
        var timeElapsed: Long
        while (!this.isInterrupted) {
            timeElapsed = System.currentTimeMillis()
            val message = Message.obtain()
            hourAngle = equatorialCoordinates.hourAngle
            message.obj = hourAngle
            activityHandler.sendMessage(message)
            val horizontalCoords = equatorialCoordinates.getHorizontalCoordinates(latitude)
            val btCoordinatesMessage = Message.obtain()
            btCoordinatesMessage.obj = horizontalCoords
            btHandler.sendMessage(btCoordinatesMessage)
            hourAngle.plusSecond()
            equatorialCoordinates.hourAngle = hourAngle
            try {
                sleep(1000 - (System.currentTimeMillis() - timeElapsed))
            } catch (e: InterruptedException) {
                break
            }
        }
        val btCoordinatesMessage = Message.obtain()
        btCoordinatesMessage.obj = "S#"
        btHandler.sendMessage(btCoordinatesMessage)
    }

    private fun setEquatorialCoordinates(equatorialCoordinates: EquatorialCoordinates) {
        this.equatorialCoordinates = equatorialCoordinates
    }

    fun setBluetoothHandler(btHandler: Handler) {
        this.btHandler = btHandler
    }

    class WriteHandler internal constructor(gotoThread: GotoThread) : Handler() {
        private val gotoThreadWeakReference: WeakReference<GotoThread> = WeakReference(gotoThread)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val gotoThread = gotoThreadWeakReference.get()
            gotoThread!!.setEquatorialCoordinates(msg.obj as EquatorialCoordinates)
        }

    }

    init {
        writeHandler = WriteHandler(this)
        this.equatorialCoordinates = equatorialCoordinates
        hourAngle = equatorialCoordinates.hourAngle
        this.latitude = latitude
    }
}