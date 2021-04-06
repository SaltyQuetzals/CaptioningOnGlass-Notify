package edu.gatech.cog.notify.phone.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import edu.gatech.cog.notify.common.cogNotifyUUID
import edu.gatech.cog.notify.common.models.GlassNotification
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicBoolean

internal class ConnectedThread : Thread() {
    private val TAG = "ConnectedThread"
    private var isRunning = AtomicBoolean(true)
    private var bluetoothSocket: BluetoothSocket? = null

    override fun run() {

        bluetoothSocket = establishConnection()
        Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")

        while (bluetoothSocket?.isConnected == true && isRunning.get()) {
            try {
                val objectInputStream = ObjectInputStream(bluetoothSocket?.inputStream)

                // TODO: Handle data
            } catch (e: IOException) {
                Log.e(TAG, "run()", e)

                // Attempt to reconnect
                bluetoothSocket = establishConnection()
                Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")
            }
        }

        cancel()
    }

    @SuppressLint("MissingPermission")
    private fun establishConnection(): BluetoothSocket? {
        val serverSocket = BluetoothAdapter.getDefaultAdapter()
            .listenUsingRfcommWithServiceRecord("edu.gatech.cog.notify", cogNotifyUUID)
        Log.d(TAG, "Looking for client socket...")
        val socket = try {
            serverSocket.accept()
        } catch (e: IOException) {
            Log.e(TAG, "socket.connect() failed", e)
            return null
        }
        serverSocket.close()
        Log.d(TAG, "Connected to ${socket.remoteDevice.name}")
        return socket
    }

    fun write(echoNotification: GlassNotification) {
        Log.v("$TAG/write", "bluetoothSocket: $bluetoothSocket")
        val objectOutputStream = ObjectOutputStream(bluetoothSocket?.outputStream)
        objectOutputStream.writeObject(echoNotification)
    }

    fun cancel() {
        try {
            isRunning.set(false)
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "cancel", e)
        }
    }
}