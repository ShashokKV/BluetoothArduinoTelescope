package chess.android.arduino.telescope.threads

import android.os.Handler
import android.os.Message
import chess.android.arduino.telescope.coordinates.Coordinate
import chess.android.arduino.telescope.coordinates.EquatorialCoordinates
import chess.android.arduino.telescope.coordinates.HourAngle
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

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
            val horizontalCoords = getHorizontalCoordinates(equatorialCoordinates)
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

    private fun getHorizontalCoordinates(equatorialCoordinates: EquatorialCoordinates): String {
        val hourAngle: Coordinate = equatorialCoordinates.hourAngle
        val declination: Coordinate = equatorialCoordinates.declination
        val lat = Math.toRadians(latitude)
        val decl = Math.toRadians(declination.toDegrees())
        val hour = Math.toRadians(hourAngle.toDegrees())
        var alt = asin(sin(decl) * sin(lat) + cos(decl) * cos(lat) * cos(hour))
        val a = round(Math.toDegrees(acos((sin(decl) - sin(alt) * sin(lat)) / (cos(alt) * cos(lat)))))
        alt = round(Math.toDegrees(alt))
        val az: Double
        az = if (sin(hour) < 0) {
            a
        } else {
            360 - a
        }
        return "Z$az#A$alt#"
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

    private fun round(value: Double): Double {
        var bd = BigDecimal((value))
        bd = bd.setScale(ROUND_PLACES, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    companion object {
        private const val ROUND_PLACES = 5
    }

    init {
        writeHandler = WriteHandler(this)
        this.equatorialCoordinates = equatorialCoordinates
        hourAngle = equatorialCoordinates.hourAngle
        this.latitude = latitude
    }
}