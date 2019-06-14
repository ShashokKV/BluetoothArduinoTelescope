package chess.android.arduino.telescope.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BluetoothThread extends Thread {

    // Tag for logging
    private static final String TAG = "BluetoothThread";

    // Delimiter used to separate messages
    private static final char DELIMITER = '#';

    // UUID that specifies a protocol for generic bluetooth serial communication
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC address of remote Bluetooth device
    private final String address;

    // Bluetooth socket of active connection
    private BluetoothSocket socket;

    // Streams that we read from and write to
    private OutputStream outStream;

    // Handlers used to pass data between threads
    private Handler readHandler;
    private final Handler writeHandler;

    /**
     * Constructor, takes in the MAC address of the remote Bluetooth device
     * and a Handler for received messages.
     *
     */
    public BluetoothThread(String address, Handler handler) {

        this.address = address.toUpperCase();
        this.readHandler = handler;

        writeHandler = new WriteHandler(this);
    }

    /**
     * Return the write handler for this connection. Messages received by this
     * handler will be written to the Bluetooth socket.
     */
    public Handler getWriteHandler() {
        return writeHandler;
    }

    public void setReadHandler(Handler readHandler) {
        this.readHandler = readHandler;
    }

    /**
     * Connect to a remote Bluetooth socket, or throw an exception if it fails.
     */
    private void connect() throws Exception {

        Log.i(TAG, "Attempting connection to " + address + "...");

        // Get this device's Bluetooth adapter
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if ((adapter == null) || (!adapter.isEnabled())){
            throw new Exception("Bluetooth adapter not found or not enabled!");
        }

        // Find the remote device
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(address);

        // Create a socket with the remote device using this protocol
        socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);

        // Make sure Bluetooth adapter is not in discovery mode
        adapter.cancelDiscovery();

        // Connect to the socket
        socket.connect();

        // Get input and output streams from the socket
        outStream = socket.getOutputStream();

        Log.i(TAG, "Connected successfully to " + address + ".");
    }

    /**
     * Disconnect the streams and socket.
     */
    private void disconnect() {

        if (outStream != null) {
            try {outStream.flush();outStream.close();} catch (Exception e) { e.printStackTrace(); }
        }

        if (socket != null) {
            try {socket.close();} catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Write data to the socket.
     */
    private void write(String s) {
        try {
            // Add the delimiter
            s += DELIMITER;

            // Convert to bytes and write
            outStream.write(s.getBytes());
            Log.i(TAG, "[SENT] " + s);

        } catch (Exception e) {
            Log.e(TAG, "Write failed!", e);
        }
    }

    /**
     * Pass a message to the read handler.
     */
    private void sendToReadHandler(String s) {

        Message msg = Message.obtain();
        msg.obj = s;
        readHandler.sendMessage(msg);
        Log.i(TAG, "[RECV] " + s);
    }

    /**
     * Entry point when thread.start() is called.
     */
    public void run() {
        // Attempt to connect and exit the thread if it failed
        try {
            connect();
            sendToReadHandler("CONNECTED");
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect!", e);
            sendToReadHandler("CONNECTION FAILED");
            disconnect();
            return;
        }

        while (!this.isInterrupted()) {
            // Make sure things haven't gone wrong
            if (outStream == null) {
                Log.e(TAG, "Lost bluetooth connection!");
                break;
            }
        }

        // If thread is interrupted, close connections
        disconnect();
        sendToReadHandler("DISCONNECTED");
    }

    static class WriteHandler extends Handler {
        private final WeakReference<BluetoothThread> btThread;

        WriteHandler(BluetoothThread btThread) {
            this.btThread = new WeakReference<>(btThread);
        }

        @Override
        public void handleMessage(Message message) {
            BluetoothThread bluetoothThread = btThread.get();
            bluetoothThread.write((String) message.obj);
        }
    }
}
