package chess.android.arduino.bluetooth;

import android.os.Handler;
import android.os.Message;



public class JoystickRunner implements Runnable {
    private Handler writeHandler;
    private Message message;

    JoystickRunner(Handler writeHandler, Message message) {
        this.writeHandler = writeHandler;
        this.message = message;
    }

    @Override
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
}
