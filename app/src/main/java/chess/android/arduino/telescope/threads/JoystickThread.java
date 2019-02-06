package chess.android.arduino.telescope.threads;

import android.os.Handler;
import android.os.Message;



public class JoystickThread extends Thread {
    private Handler writeHandler;
    private Message message;
    private static final int Y_MAX = 100;
    private static final int X_MAX = 100;

    public JoystickThread(Handler writeHandler, Message message) {
        this.writeHandler = writeHandler;
        this.message = message;
    }

    public void run() {
        try {
            Message message = new Message();
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    writeHandler.sendMessage(this.message);
                }catch (IllegalStateException exc) {
                    exc.printStackTrace();
                }
                message.copyFrom(this.message);
                this.message = message;
                Thread.sleep(100);
            }
        } catch (InterruptedException ignored) {
        }
    }

    public static String parseJoystickInput(int angle, int strength) {
        int x, y;
        x = Double.valueOf((strength * X_MAX * Math.cos(Math.toRadians(angle))) / 100).intValue();
        y = Double.valueOf((strength * Y_MAX * Math.sin(Math.toRadians(angle))) / 100).intValue();

        return "X" + x + "#" + "Y" + y;
    }
}
