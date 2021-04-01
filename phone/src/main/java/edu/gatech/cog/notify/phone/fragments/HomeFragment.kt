package edu.gatech.cog.notify.phone.fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import edu.gatech.cog.notify.common.cogNotifyUUID
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.phone.R
import edu.gatech.cog.notify.phone.databinding.FragmentHomeBinding
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicBoolean


private val TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var connectedThread: ConnectedThread

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectedThread = ConnectedThread()
        connectedThread.start()

        val manager =
            this.activity?.applicationContext?.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = manager.deviceList
        Snackbar.make(view, "Listing attached devices...", Snackbar.LENGTH_SHORT).show()
        Log.d(TAG, "")
        deviceList.values.forEach { device ->
            Toast.makeText(this.activity?.applicationContext, device.deviceName, Toast.LENGTH_SHORT)
                .show()
        }

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
