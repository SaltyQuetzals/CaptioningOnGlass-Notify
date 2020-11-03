package edu.gatech.cog.notify.glass

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import edu.gatech.cog.notify.common.cogNotifyUUID
import edu.gatech.cog.notify.common.models.GlassNotification
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicBoolean

private val TAG = SourceConnectionService::class.java.simpleName

class SourceConnectionService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private var createSocketThread: ConnectedThread? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createSocketThread?.broadcastDeviceStatus()
            ?: (intent?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)?.let {
                bluetoothDevice = it

                createSocketThread = ConnectedThread(bluetoothDevice).apply {
                    start()
                }
            } ?: run {
                Log.e(TAG, "No BluetoothDevice found in extras")
            })
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
        createSocketThread?.cancel()
    }

    @Subscribe
    private fun onWriteToPhone(data: Any) {
        createSocketThread?.write(data)
    }

    inner class ConnectedThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        private lateinit var bluetoothSocket: BluetoothSocket

        override fun run() {
            var isConnected = init()

            while (isConnected && isRunning.get()) {
                try {
                    val objectInputStream = ObjectInputStream(bluetoothSocket.inputStream)
                    val glassNotification = objectInputStream.readObject() as GlassNotification
                    EventBus.getDefault().post(glassNotification)
                } catch (e: IOException) {
                    Log.e(TAG, "Potential socket disconnect. Attempting to reconnect", e)
                    // Attempt to reconnect
                    isConnected = init()
                }
            }

            cancel()
        }

        fun write(data: Any) {
            val objectOutputStream = ObjectOutputStream(bluetoothSocket.outputStream)
            objectOutputStream.writeObject(data)
        }

        private fun init(): Boolean {
            val isConnected: Boolean = establishConnection()?.let {
                bluetoothSocket = it
                Log.v(TAG, "init: connected to ${bluetoothSocket.remoteDevice?.name}")
                bluetoothSocket.isConnected
            } ?: run {
                false
            }

            broadcastDeviceStatus()

            return isConnected
        }

        private fun establishConnection(): BluetoothSocket? {
            try {
                val socket = bluetoothDevice.createRfcommSocketToServiceRecord(cogNotifyUUID)

                socket.connect()
                Log.v(
                    TAG,
                    "Connection status to ${socket.remoteDevice.name}: ${socket.isConnected}"
                )
                return socket
            } catch (e: IOException) {
                Log.v(TAG, "Failed to accept", e)
            }

            return null
        }

        fun cancel() {
            try {
                bluetoothSocket.close()
                isRunning.set(false)
                Log.v(TAG, "Cancel")
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "cancel", e)
            }
        }

        fun broadcastDeviceStatus() {
            Intent().also { statusIntent ->
                statusIntent.action = Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS
                statusIntent.putExtra(
                    Constants.EXTRA_DEVICE_IS_CONNECTED,
                    bluetoothSocket.isConnected
                )
                if (bluetoothSocket.isConnected) {
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_NAME,
                        bluetoothSocket.remoteDevice?.name
                    )
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_ADDRESS,
                        bluetoothSocket.remoteDevice?.address
                    )
                }
                sendBroadcast(statusIntent)
            }
        }
    }
}
