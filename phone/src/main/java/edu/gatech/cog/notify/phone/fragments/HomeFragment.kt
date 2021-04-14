package edu.gatech.cog.notify.phone.fragments

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.zxing.BarcodeFormat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.journeyapps.barcodescanner.BarcodeEncoder
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.phone.Constants.ACTION_USB_PERMISSION
import edu.gatech.cog.notify.phone.R
import edu.gatech.cog.notify.phone.databinding.FragmentHomeBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables.combineLatest
import io.reactivex.rxjava3.kotlin.toObservable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val TAG = HomeFragment::class.java.simpleName

data class Caption(
    @SerializedName("text") val text: String,
    @SerializedName("id") val id: String,
    @SerializedName("duration") val duration: Long
)


private val usbReceiver = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_USB_PERMISSION == intent.action) {
            synchronized(this) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    device?.apply {
                        //call method to set up device communication
                    }
                } else {
                    Log.d(TAG, "permission denied for device $device")
                }
            }
        }
    }
}


class HomeFragment : Fragment(R.layout.fragment_home), SerialInputOutputManager.Listener {

    private lateinit var serialPort: UsbSerialPort
    private lateinit var fragmentContext: Context
    private lateinit var connectedThread: ConnectedThread
    private val lookingAtSubject = BehaviorSubject.create<String>()
    private lateinit var captionsObservable: Observable<Caption>
    private lateinit var isLookingAtSpeakerObservable: Observable<Boolean>
    private lateinit var shouldSendMessageToGlassObservable: Observable<Pair<Boolean, Caption>>
    private lateinit var shouldSendMessageToGlassDisposable: Disposable
    private lateinit var device: UsbDevice


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


        // Let's find the Android/Arduino device.
        val manager =
            fragmentContext.getSystemService(Context.USB_SERVICE) as UsbManager

        // This block of code basically tries to do two things in order:
        // 1. Retrieve the already-connected USB device
        // 2. Request permission for the USB device.
        device =
            activity?.intent?.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: run {
                val permissionIntent = PendingIntent.getBroadcast(
                    fragmentContext, 0, Intent(
                        ACTION_USB_PERMISSION
                    ), 0
                )
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                activity?.registerReceiver(usbReceiver, filter)
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
                val driver = availableDrivers[0]
                manager.requestPermission(driver.device, permissionIntent)
                driver.device
            }
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        val connection = manager.openDevice(device)

        serialPort = driver.ports[0]
        serialPort.open(connection)
        serialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        serialPort.dtr = true
        serialPort.rts = true
        Log.d(TAG, "Serial port opened!")

        val usbIoManager = SerialInputOutputManager(serialPort, this)
        usbIoManager.readBufferSize = 100
        Executors.newSingleThreadExecutor().submit(usbIoManager)

        isLookingAtSpeakerObservable = combineLatest(
            // Don't emit for duplicates or garbage values
            lookingAtSubject.distinctUntilChanged()
                .filter { lookingAt -> lookingAt != "" },
            captionsObservable
        )
            .map { (lookingAt, caption) ->
                Log.d("$TAG/isLookingAtSpeaker$", lookingAt)
                lookingAt == caption.id
            }

        shouldSendMessageToGlassObservable =
            combineLatest(isLookingAtSpeakerObservable, captionsObservable)

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
                Log.d(TAG, "Send button clicked!")
                requireActivity().runOnUiThread {
                    shouldSendMessageToGlassDisposable =
                        shouldSendMessageToGlassObservable
                            .subscribe({ (isLookingAtSpeaker, caption) ->
                                Log.d(
                                    "$TAG/sendMessageToGlass",
                                    "isLookingAtSpeaker: $isLookingAtSpeaker"
                                )
                                Log.d("$TAG/sendMessageToGlass", "speakerId: ${caption.id}")
                                Log.d("$TAG/sendMessageToGlass", "text: ${caption.text}")
                                connectedThread.write(
                                    GlassNotification(
                                        if (isLookingAtSpeaker) {
                                            caption.text
                                        } else {
                                            ""
                                        }, false, caption.duration
                                    )
                                )
                            },
                                { e -> Log.e(TAG, e.message.toString()) }
                            )
                }
            }
        }
    }

    override fun onNewData(data: ByteArray) {
        val splitData = String(data).trim().split("\n")
        if (splitData.isEmpty() || splitData[0] == "") {
            return
        }

        // https://stackoverflow.com/a/48531720
        val mostCommonElement =
            splitData.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
        lookingAtSubject.onNext(
            when (mostCommonElement) {
                "0" -> "juror-a"
                "1" -> "juror-b"
                "2" -> "juror-c"
                "3" -> "jury-foreman"
                "X" -> "X"
                else -> ""
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
        shouldSendMessageToGlassDisposable.dispose()
    }

}
