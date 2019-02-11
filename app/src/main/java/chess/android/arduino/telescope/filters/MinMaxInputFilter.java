package chess.android.arduino.telescope.filters;

import android.text.InputFilter;
import android.text.Spanned;

public class MinMaxInputFilter implements InputFilter {
    private int min, max;

    public MinMaxInputFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            String sourceStr = source.toString();
            if (sourceStr.equals("-")) {
                if (min<0) {
                    return null;
                }else{
                    return "";
                }
            }
            int input = Integer.parseInt(dest.toString() + sourceStr);
            if (isInRange(min, max, input))
                return null;
        } catch (NumberFormatException ignored) { }
        return "";
    }

    private boolean isInRange(int min, int max, int input) {
       return max > min ? input >= min && input <= max : input >= max && input <= min;
    }
}
