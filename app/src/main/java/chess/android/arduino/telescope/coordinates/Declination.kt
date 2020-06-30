package chess.android.arduino.telescope.coordinates

class Declination(degree: Int, minute: Int, second: Int) : Coordinate(0, minute, second) {
    override fun plusMinute() {
        super.plusMinute()
        if (minute == 0) {
            plusDegree()
        }
    }

    override fun minusMinute() {
        super.minusMinute()
        if (minute == 59) {
            minusDegree()
        }
    }

    override fun toDegrees(): Double {
        return (degree * 3600.0 + minute * 60.0 + second) / 3600.0
    }

    init {
        this.degree = degree
    }
}