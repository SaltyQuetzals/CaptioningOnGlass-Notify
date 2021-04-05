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
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.zxing.BarcodeFormat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.journeyapps.barcodescanner.BarcodeEncoder
import edu.gatech.cog.notify.common.cogNotifyUUID
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.phone.R
import edu.gatech.cog.notify.phone.databinding.FragmentHomeBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

private val TAG = HomeFragment::class.java.simpleName

data class Caption(
    @SerializedName("text") val text: String,
    @SerializedName("id") val id: String,
    @SerializedName("duration") val duration: Long
)

class HomeFragment : Fragment(R.layout.fragment_home), SerialInputOutputManager.Listener {

    private lateinit var serialPort: UsbSerialPort
    private lateinit var fragmentContext: Context
    private lateinit var connectedThread: ConnectedThread
    private val lookingAtSubject = BehaviorSubject.create<String>()
    private lateinit var captionsObservable: Observable<Caption>
    private lateinit var isLookingAtSpeakerObservable: Observable<Boolean>

    private fun loadJSONFromAsset(context: Context, assetName: String): String {
        return context.assets.open(assetName).bufferedReader().use { it.readText() }
    }

    private fun initializeCaptionsObservable() {
        val gson = Gson()
        val captionsJSONString = loadJSONFromAsset(fragmentContext, "captions.json")
        val captions = gson.fromJson(captionsJSONString, Array<Caption>::class.java)

        captionsObservable = captions.withIndex().toObservable().concatMap { (i, caption) ->
            if (i == 0) {
                // Don't add any delay to the first caption, just emit it
                Observable.just(caption)
            } else {
                // Delay the emission of this caption by the previous caption's duration
                Observable.just(caption).delay(captions[i - 1].duration, TimeUnit.MILLISECONDS)
            }
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectedThread = ConnectedThread()
        connectedThread.start()

        initializeCaptionsObservable()

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

                captionsObservable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ caption ->
                        Snackbar.make(view, caption.text, caption.duration.toInt()).show()
                        connectedThread.write(GlassNotification(caption.text, false))
                    }, { e -> Log.e(TAG, e.message.toString()) })
            }


        }


        // Let's find the Android/Arduino device.
        val manager =
            fragmentContext.getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(fragmentContext, "No USB drivers available", Toast.LENGTH_LONG).show()
            return
        }
        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device)
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            Toast.makeText(
                fragmentContext,
                "Need to request permission for device",
                Toast.LENGTH_SHORT
            ).show()
        }

        serialPort = driver.ports[0]
        serialPort.open(connection)
        serialPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        val usbIoManager = SerialInputOutputManager(serialPort)
        Executors.newSingleThreadExecutor().submit(usbIoManager)

        // This observable will emit values if the
        isLookingAtSpeakerObservable =
            lookingAtSubject.zipWith(
                captionsObservable,
                { lookingAt: String, caption: Caption -> Pair(lookingAt, caption) })
                .map { (lookingAt, caption) -> lookingAt == caption.id }
                .distinctUntilChanged()
    }

    override fun onNewData(data: ByteArray) {
        val dataAsString = String(data)
        lookingAtSubject.onNext(
            if (dataAsString == "none") {
                null
            } else {
                dataAsString
            }
        )
    }

    override fun onRunError(e: Exception?) {
        TODO("Not yet implemented")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
    }

    override fun onDestroy() {
        super.onDestroy()
        connectedThread.cancel()
        serialPort.close()
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
}
