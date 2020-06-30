package chess.android.arduino.telescope.threads

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Message
import android.util.Log
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

class BluetoothThread(// MAC address of remote Bluetooth device
        private val address: String, handler: Handler) : Thread() {

    // Bluetooth socket of active connection
    private lateinit var socket: BluetoothSocket

    // Streams that we read from and write to
    private var outStream: OutputStream? = null

    // Handlers used to pass data between threads
    private var readHandler: Handler

    /**
     * Return the write handler for this connection. Messages received by this
     * handler will be written to the Bluetooth socket.
     */
    val writeHandler: Handler

    fun setReadHandler(readHandler: Handler) {
        this.readHandler = readHandler
    }

    /**
     * Connect to a remote Bluetooth socket, or throw an exception if it fails.
     */
    @Throws(Exception::class)
    private fun connect() {
        Log.i(TAG, "Attempting connection to $address...")

        // Get this device's Bluetooth adapter
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            throw Exception("Bluetooth adapter not found or not enabled!")
        }

        // Find the remote device
        val remoteDevice = adapter.getRemoteDevice(address)

        // Create a socket with the remote device using this protocol
        socket = remoteDevice.createRfcommSocketToServiceRecord(uuid)

        // Make sure Bluetooth adapter is not in discovery mode
        adapter.cancelDiscovery()

        // Connect to the socket
        socket.connect()

        // Get input and output streams from the socket
        outStream = socket.outputStream
        Log.i(TAG, "Connected successfully to $address.")
    }

    /**
     * Disconnect the streams and socket.
     */
    private fun disconnect() {
        if (outStream != null) {
            try {
                outStream!!.flush()
                outStream!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Write data to the socket.
     */
    private fun write(s: String) {
        var str = s
        try {
            // Add the delimiter
            str += DELIMITER

            // Convert to bytes and write
            outStream!!.write(str.toByteArray())
            Log.i(TAG, "[SENT] $str")
        } catch (e: Exception) {
            Log.e(TAG, "Write failed!", e)
        }
    }

    /**
     * Pass a message to the read handler.
     */
    private fun sendToReadHandler(s: String) {
        val msg = Message.obtain()
        msg.obj = s
        readHandler.sendMessage(msg)
        Log.i(TAG, "[RECV] $s")
    }

    /**
     * Entry point when thread.start() is called.
     */
    override fun run() {
        // Attempt to connect and exit the thread if it failed
        try {
            connect()
            sendToReadHandler("CONNECTED")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect!", e)
            sendToReadHandler("CONNECTION FAILED")
            disconnect()
            return
        }
        while (!this.isInterrupted) {
            // Make sure things haven't gone wrong
            if (outStream == null) {
                Log.e(TAG, "Lost bluetooth connection!")
                break
            }
        }

        // If thread is interrupted, close connections
        disconnect()
        sendToReadHandler("DISCONNECTED")
    }

    internal class WriteHandler(btThread: BluetoothThread) : Handler() {
        private val btThread: WeakReference<BluetoothThread> = WeakReference(btThread)
        override fun handleMessage(message: Message) {
            val bluetoothThread = btThread.get()
            bluetoothThread!!.write(message.obj as String)
        }
    }

    companion object {
        // Tag for logging
        private const val TAG = "BluetoothThread"

        // Delimiter used to separate messages
        private const val DELIMITER = '#'

        // UUID that specifies a protocol for generic bluetooth serial communication
        private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * Constructor, takes in the MAC address of the remote Bluetooth device
     * and a Handler for received messages.
     *
     */
    init {
        readHandler = handler
        writeHandler = WriteHandler(this)
    }
}