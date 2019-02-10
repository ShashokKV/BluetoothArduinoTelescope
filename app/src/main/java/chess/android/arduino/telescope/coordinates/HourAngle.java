package chess.android.arduino.telescope.coordinates;

import android.app.Activity;
import android.widget.TextView;

import java.time.LocalTime;
import java.util.Locale;

import chess.android.arduino.telescope.R;

public class HourAngle  {
    private LocalTime timer;

    public HourAngle(int hour, int minute, int second) {
        if (hour>24 || hour<0 || minute>59 || minute<0 || second>59 || second<0) {
            throw new IllegalArgumentException(String.format("Illegal time: %d, %d, %d", hour, minute, second));
        }
        timer = LocalTime.of(hour, minute, second);
    }

    public void  plusSecond() {
        timer = timer.plusSeconds(1);
    }

    public double toDegrees() {
        return (((timer.getHour()*3600.0d)+(timer.getMinute()*60.0d)+(timer.getSecond()*1.0d))*15.0d)/3600.0d;
    }

    public void setToView(Activity activity) {
        TextView hourTextView = activity.findViewById(R.id.hAngHour);
        TextView minuteTextView = activity.findViewById(R.id.hAngMinute);
        TextView secondsTextView = activity.findViewById(R.id.hAngSecond);
        setToTextView(hourTextView, timer.getHour());
        setToTextView(minuteTextView, timer.getMinute());
        setToTextView(secondsTextView, timer.getSecond());
    }

    private void setToTextView(TextView textView, int value) {
        //no tag - not changing by user
        if (textView.getTag()==null || !textView.getTag().toString().equals("user")) {
            String oldValue = textView.getText().toString();
            String newValue = String.format(Locale.ENGLISH, "%d", value);
            if (!oldValue.equals(newValue)) {
                //is changing by thread
                textView.setTag("thread");
                textView.setText(newValue);
                textView.setTag(null);
            }
        }
    }
}
