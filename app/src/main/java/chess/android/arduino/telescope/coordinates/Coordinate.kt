package chess.android.arduino.telescope.coordinates

import java.time.LocalTime

abstract class Coordinate internal constructor(hour: Int, minute: Int, second: Int) {
    var degree = 0
    private var timer: LocalTime

    val hour: Int
        get() = timer.hour

    val minute: Int
        get() = timer.minute

    val second: Int
        get() = timer.second

    fun plusHour() {
        timer = timer.plusHours(1)
    }

    fun plusDegree() {
        if (degree != 89) degree++
    }

    fun minusDegree() {
        if (degree != -89) degree--
    }

    fun minusHour() {
        timer = timer.plusHours(-1)
    }

    open fun plusMinute() {
        timer = timer.plusMinutes(1)
    }

    open fun minusMinute() {
        timer = timer.plusMinutes(-1)
    }

    fun plusSecond() {
        timer = timer.plusSeconds(1)
    }

    fun minusSecond() {
        timer = timer.plusSeconds(-1)
    }

    abstract fun toDegrees(): Double

    init {
        require(!(hour > 24 || hour < 0 || minute > 59 || minute < 0 || second > 59 || second < 0)) { String.format("Illegal time: %d, %d, %d", hour, minute, second) }
        timer = LocalTime.of(hour, minute, second)
    }
}