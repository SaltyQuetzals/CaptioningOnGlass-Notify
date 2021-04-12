package edu.gatech.cog.notify.glass.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator
import edu.gatech.cog.notify.common.GLASS_SOUND_SUCCESS
import edu.gatech.cog.notify.glass.Constants
import edu.gatech.cog.notify.glass.R
import edu.gatech.cog.notify.glass.SourceConnectionService
import edu.gatech.cog.notify.glass.databinding.FragmentConnectBinding

private val TAG = ConnectFragment::class.java.simpleName

class ConnectFragment : Fragment(R.layout.fragment_connect) {

<<<<<<< Updated upstream
    private var _binding: FragmentConnectBinding? = null
    private val binding get() = _binding!!

    private var deviceName = ""

    private var pairDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
                    if (it.name == deviceName) {
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                        requireActivity().unregisterReceiver(this)

                        startBluetoothService(it)
                    }
                }
            }
        }
    }

    private var deviceStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getBooleanExtra(Constants.EXTRA_DEVICE_IS_CONNECTED, false)
                ?.let { isConnected ->
                    if (isConnected) {
                        val audio =
                            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audio.playSoundEffect(GLASS_SOUND_SUCCESS)

                        requireActivity().getSharedPreferences(
                            Constants.SHARED_PREF,
                            Context.MODE_PRIVATE
                        )
                            .edit()?.apply {
                                putString(
                                    Constants.SHARED_PREF_DEVICE_NAME,
                                    intent.getStringExtra(Constants.EXTRA_DEVICE_NAME)
                                )
                                putString(
                                    Constants.SHARED_PREF_DEVICE_ADDRESS,
                                    intent.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS)
                                )
                                apply()
                            }

                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.frameLayoutMain,
                                NotifyDisplayFragment.newInstance()
                            )
                            .commit()
                    } else {
                        binding.tvConnectStatus.text =
                            "Failed to connect. Please quit both apps and try again."
                    }
                }

        }
    }
=======
//    private var _binding: FragmentConnectBinding? = null
//    private val binding get() = _binding!!
//
//    private var deviceName = ""
//
//    private var pairDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                BluetoothDevice.ACTION_FOUND -> {
//                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
//                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
//                        Log.i("$TAG/onReceive", "${it.name} ?= $deviceName")
//                        if (it.name == deviceName) {
//                            // We've got a match! This device name matches the one we're looking for.
//
//                            // Shut off discovery mode, we don't need it anymore
//                            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
//
//                            // Unregister this receiver, we won't be calling it from now on
//                            requireActivity().unregisterReceiver(this)
//
//                            // Let's connect!
//                            connectToDevice(it.name)
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private var deviceStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            intent?.getBooleanExtra(Constants.EXTRA_DEVICE_IS_CONNECTED, false)
//                ?.let { isConnected ->
//                    if (isConnected) {
//                        val audio =
//                            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//                        requireActivity().supportFragmentManager
//                            .beginTransaction()
//                            .replace(
//                                R.id.frameLayoutMain,
//                                NotifyDisplayFragment.newInstance()
//                            )
//                            .commit()
//                    } else {
//                        binding.tvConnectStatus.text =
//                            "Failed to connect. Please quit both apps and try again."
//                    }
//                }
//
//        }
//    }
>>>>>>> Stashed changes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.connect_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                requireActivity().getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
                    .edit()?.apply {
                        putString(
                            Constants.SHARED_PREF_DEVICE_NAME, ""
                        )
                        putString(
                            Constants.SHARED_PREF_DEVICE_ADDRESS, ""
                        )
                        apply()
                    }

                startQrCodeScanner()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        _binding = FragmentConnectBinding.bind(view)

<<<<<<< Updated upstream
        val sharedPref =
            requireActivity().getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
        val deviceName = sharedPref.getString(Constants.SHARED_PREF_DEVICE_NAME, "")!!
        val deviceAddress = sharedPref.getString(Constants.SHARED_PREF_DEVICE_ADDRESS, "")!!

        if (deviceName.isNotEmpty() && deviceAddress.isNotEmpty()) {
            val pairedDevice = checkPairedDevices(deviceName)
            pairedDevice?.let {
                startBluetoothService(it)
                return
            } ?: run {
                // Device is no longer in paired devices list
                sharedPref.edit().apply {
                    putString(Constants.SHARED_PREF_DEVICE_NAME, "")
                    putString(Constants.SHARED_PREF_DEVICE_ADDRESS, "")
                    apply()
                }
            }
        }

        startQrCodeScanner()
=======
//        startQrCodeScanner()
>>>>>>> Stashed changes
    }

    private fun startQrCodeScanner() {
        IntentIntegrator.forSupportFragment((this as Fragment))
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Open CoG-Notify on your phone and scan the QR code")
            .setBeepEnabled(false)
            .setBarcodeImageEnabled(false)
            .initiateScan()
    }

<<<<<<< Updated upstream
    private fun connectToDevice(deviceName: String) {
        checkPairedDevices(deviceName)?.let {
            startBluetoothService(it)
        } ?: run {
            pairDevice(deviceName)
        }
    }
=======
//    private fun connectToDevice(deviceName: String) {
//        checkPairedDevices(deviceName)?.let {
//            Log.i("$TAG/connectToDevice", "$deviceName seen before, going to startBluetoothService")
//            startBluetoothService(it)
//        } ?: run {
//
//            Log.i("$TAG/connectToDevice", "$deviceName never seen before, going to try and pair")
//            pairDevice(deviceName)
//        }
//    }
>>>>>>> Stashed changes

    private fun checkPairedDevices(deviceName: String): BluetoothDevice? {
        BluetoothAdapter.getDefaultAdapter()
            .bondedDevices.iterator()
            .forEach {
                if (it.name == deviceName) return it
            }
        return null
    }

<<<<<<< Updated upstream
    private fun pairDevice(deviceName: String) {
        val isDiscovering = BluetoothAdapter.getDefaultAdapter().startDiscovery()

        if (isDiscovering) {
            this.deviceName = deviceName
            requireActivity().registerReceiver(
                pairDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        } else {
            Log.e(TAG, "pairDevice startDiscovery false")
        }
    }

    private fun startBluetoothService(bluetoothDevice: BluetoothDevice) {
        requireActivity().registerReceiver(
            deviceStatusReceiver,
            IntentFilter(Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS)
        )
        requireActivity().startService(Intent(activity, SourceConnectionService::class.java).apply {
            putExtra(
                Constants.EXTRA_BLUETOOTH_DEVICE, bluetoothDevice
            )
        })
    }
=======
//    private fun pairDevice(deviceName: String) {
//        Log.i("$TAG/pairDevice", "Attempting to start discovering devices...")
//        val isDiscovering = BluetoothAdapter.getDefaultAdapter().startDiscovery()
//
//        if (isDiscovering) {
//            Log.i("$TAG/pairDevice", "In discovery mode")
//            this.deviceName = deviceName
//            requireActivity().registerReceiver(
//                pairDeviceReceiver,
//                IntentFilter(BluetoothDevice.ACTION_FOUND)
//            )
//        } else {
//            Log.e(TAG, "pairDevice startDiscovery false")
//        }
//    }

//    private fun startBluetoothService(bluetoothDevice: BluetoothDevice) {
//        Log.d(
//            TAG, "Attempting to connect to \"${bluetoothDevice.name}...\""
//        )
//        requireActivity().registerReceiver(
//            deviceStatusReceiver,
//            IntentFilter(Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS)
//        )
//        requireActivity().startService(Intent(activity, SourceConnectionService::class.java).apply {
//            putExtra(
//                Constants.EXTRA_BLUETOOTH_DEVICE, bluetoothDevice
//            )
//        })
//    }
>>>>>>> Stashed changes

    override fun onDestroyView() {
//        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

//        try {
//            requireActivity().unregisterReceiver(pairDeviceReceiver)
//        } catch (e: IllegalArgumentException) {
//            Log.e(TAG, "pairDeviceReceiver was never registered", e)
//        }
//        try {
//            requireActivity().unregisterReceiver(deviceStatusReceiver)
//        } catch (e: IllegalArgumentException) {
//            Log.e(TAG, "deviceStatusReceiver was never registered", e)
//        }
//        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

<<<<<<< Updated upstream
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.let { barcodeResult ->
            barcodeResult.contents?.let { barcodeContents ->
                requireActivity().runOnUiThread {
                    binding.tvConnectStatus.text =
                        "Attempting to connect to \"$barcodeContents...\""
                }
                connectToDevice(barcodeContents)
            } ?: run {
                requireActivity().runOnUiThread {
                    binding.tvConnectStatus.text = "Error while scanning QR code."
                }
            }
        }
    }
=======
    // Called when the QR Code scanner successfully scans.
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.let { barcodeResult ->
//            barcodeResult.contents?.let { barcodeContents ->
//                requireActivity().runOnUiThread {
//                    binding.tvConnectStatus.text =
//                        "Attempting to connect to \"$barcodeContents...\""
//                }
//                connectToDevice(barcodeContents)
//            } ?: run {
//                requireActivity().runOnUiThread {
//                    binding.tvConnectStatus.text = "Error while scanning QR code."
//                }
//            }
//        }
//    }
>>>>>>> Stashed changes

    companion object {
        fun newInstance() = ConnectFragment()
    }
}