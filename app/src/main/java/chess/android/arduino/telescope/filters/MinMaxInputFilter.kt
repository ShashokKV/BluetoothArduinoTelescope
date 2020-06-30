package chess.android.arduino.telescope.filters

import android.text.InputFilter
import android.text.Spanned

class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        try {
            val sourceStr = source.toString()
            if (sourceStr == "-") {
                return if (min < 0) {
                    null
                } else {
                    ""
                }
            }
            val input = (dest.toString() + sourceStr).toInt()
            if (isInRange(min, max, input)) return null
        } catch (ignored: NumberFormatException) {
        }
        return ""
    }

    private fun isInRange(min: Int, max: Int, input: Int): Boolean {
        return if (max > min) input in min..max else input in max..min
    }

}