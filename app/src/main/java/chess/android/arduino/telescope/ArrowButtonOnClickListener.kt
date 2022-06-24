package chess.android.arduino.telescope

import android.view.View
import chess.android.arduino.telescope.coordinates.Coordinate
import chess.android.arduino.telescope.coordinates.Declination
import chess.android.arduino.telescope.coordinates.HourAngle


class ArrowButtonOnClickListener(private val mainActivity: MainActivity) : View.OnClickListener {
    override fun onClick(v: View) {
        val coordinate: Coordinate
        val viewType: String
        val direction: String
        val valueType: String
        val tag: Array<String> = v.tag.toString().split("#").toTypedArray()
        viewType = tag[0]
        valueType = tag[1]
        direction = tag[2]
        coordinate = when {
            viewType.startsWith("H") -> {
                mainActivity.hourAngle
            }
            viewType.startsWith("D") -> {
                mainActivity.declination
            }
            else -> {
                throw IllegalArgumentException("Unknown viewTye $viewType")
            }
        }
        when (valueType) {
            "H" -> if (direction == "UP") {
                coordinate.plusHour()
            } else {
                coordinate.minusHour()
            }
            "M" -> if (direction == "UP") {
                coordinate.plusMinute()
            } else {
                coordinate.minusMinute()
            }
            "S" -> if (direction == "UP") {
                coordinate.plusSecond()
            } else {
                coordinate.minusSecond()
            }
            "D" -> if (direction == "UP") {
                coordinate.plusDegree()
            } else {
                coordinate.minusDegree()
            }
        }
        if (coordinate is HourAngle) {
            mainActivity.setHourAngle(coordinate, mainActivity.getString(R.string.user))
        } else {
            mainActivity.declination = coordinate as Declination
        }
    }

}