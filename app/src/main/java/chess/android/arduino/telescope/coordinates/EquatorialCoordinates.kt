package chess.android.arduino.telescope.coordinates

import chess.android.arduino.telescope.MainActivity

class EquatorialCoordinates private constructor(val declination: Declination, var hourAngle: HourAngle) {

    constructor(activity: MainActivity) : this(activity.declination, activity.hourAngle)

}