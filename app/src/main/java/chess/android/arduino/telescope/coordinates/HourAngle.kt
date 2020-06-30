package chess.android.arduino.telescope.coordinates

class HourAngle(hour: Int, minute: Int, second: Int) : Coordinate(hour, minute, second) {
    override fun toDegrees(): Double {
        return ((hour * 3600.0 +
                minute * 60.0
                + second * 1.0) * 15.0
                / 3600.0)
    }
}