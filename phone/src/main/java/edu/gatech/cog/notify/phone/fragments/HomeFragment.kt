package edu.gatech.cog.notify.phone.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import edu.gatech.cog.notify.common.cogNotifyUUID
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.phone.R
import edu.gatech.cog.notify.phone.databinding.FragmentHomeBinding
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicBoolean


private val TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var connectedThread: ConnectedThread

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectedThread = ConnectedThread()
        connectedThread.start()

        FragmentHomeBinding.bind(view).apply {
            ivQrCode.setImageBitmap(
                BarcodeEncoder().encodeBitmap(
                    BluetoothAdapter.getDefaultAdapter().name,
                    BarcodeFormat.QR_CODE,
                    800,
                    800
                )
            )

            btnNotify.setOnClickListener {
                val notifyText = etMessage.text.toString()
                val isVibrate = toggleVibrate.isChecked
                Log.v(TAG, "$notifyText, $isVibrate")
                connectedThread.write(GlassNotification(notifyText, isVibrate))

                etMessage.setText("")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectedThread.cancel()
    }

    inner class ConnectedThread : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        private var bluetoothSocket: BluetoothSocket? = null

        override fun run() {
            val buffer = ByteArray(1024)

            bluetoothSocket = establishConnection()
            Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")

            while (bluetoothSocket?.isConnected == true && isRunning.get()) {
                try {
                    bluetoothSocket?.inputStream?.let { inputStream ->
                        val bytes = inputStream.read(buffer)
                        val incomingMessage = String(buffer, 0, bytes)
                        Log.v(TAG, "incomingMessage: $incomingMessage")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "run()", e)

                    // Attempt to reconnect
                    bluetoothSocket = establishConnection()
                    Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")
                }
            }

            cancel()
        }

        private fun establishConnection(): BluetoothSocket? {
            val serverSocket = BluetoothAdapter.getDefaultAdapter()
                .listenUsingRfcommWithServiceRecord("edu.gatech.cog.notify", cogNotifyUUID)
            val socket = try {
                serverSocket.accept()
            } catch (e: IOException) {
                Log.e(TAG, "socket.connect() failed", e)
                return null
            }
            serverSocket.close()

            return socket
        }

        fun write(echoNotification: GlassNotification) {
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
}
