package chess.android.arduino.telescope.coordinates

import chess.android.arduino.telescope.MainActivity
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

class EquatorialCoordinates private constructor(private val declination: Declination, var hourAngle: HourAngle) {

    constructor(activity: MainActivity) : this(activity.declination, activity.hourAngle)

    fun getHorizontalCoordinates(latitude: Double): String {
        val hourAngle: Coordinate = this.hourAngle
        val declination: Coordinate = this.declination
        val lat = Math.toRadians(latitude)
        val decl = Math.toRadians(declination.toDegrees())
        val hour = Math.toRadians(hourAngle.toDegrees())
        var alt = asin(sin(decl) * sin(lat) + cos(decl) * cos(lat) * cos(hour))
        val a = round(Math.toDegrees(acos((sin(decl) - sin(alt) * sin(lat)) / (cos(alt) * cos(lat)))))
        alt = round(Math.toDegrees(alt))
        val az: Double = if (sin(hour) < 0) {
            a
        } else {
            360 - a
        }
        return "Z$az#A$alt#"
    }

    private fun round(value: Double): Double {
        var bd = BigDecimal((value))
        bd = bd.setScale(ROUND_PLACES, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    companion object {
        private const val ROUND_PLACES = 5
    }

}