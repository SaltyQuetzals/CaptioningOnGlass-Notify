package edu.gatech.cog.notify.glass.fragments

import android.annotation.SuppressLint
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

    private var _binding: FragmentConnectBinding? = null
    private val binding get() = _binding!!

    private var deviceName = ""

    private var pairDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
                        Log.i("$TAG/onReceive", "${it.name} ?= $deviceName")
                        if (it.name == deviceName) {
                            // We've got a match! This device name matches the one we're looking for.

                            // Shut off discovery mode, we don't need it anymore
                            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

                            // Unregister this receiver, we won't be calling it from now on
                            requireActivity().unregisterReceiver(this)

                            // Let's connect!
                            connectToDevice(it.name)
                        }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentConnectBinding.bind(view)

        startQrCodeScanner()
    }

    private fun startQrCodeScanner() {
        IntentIntegrator.forSupportFragment((this as Fragment))
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Open CoG-Notify on your phone and scan the QR code")
            .setBeepEnabled(false)
            .setBarcodeImageEnabled(false)
            .initiateScan()
    }

    private fun connectToDevice(deviceName: String) {
        checkPairedDevices(deviceName)?.let {
            Log.i("$TAG/connectToDevice", "$deviceName seen before, going to startBluetoothService")
            startBluetoothService(it)
        } ?: run {

            Log.i("$TAG/connectToDevice", "$deviceName never seen before, going to try and pair")
            pairDevice(deviceName)
        }
    }

    private fun checkPairedDevices(deviceName: String): BluetoothDevice? {
        Log.i("$TAG/checkPairedDevices", "Previously paired device names:")
        BluetoothAdapter.getDefaultAdapter()
            .bondedDevices.iterator()
            .forEach {
                Log.i("$TAG/checkPairedDevices", it.name)
                if (it.name == deviceName) return it
            }
        return null
    }

    private fun pairDevice(deviceName: String) {
        Log.i("$TAG/pairDevice", "Attempting to start discovering devices...")
        val isDiscovering = BluetoothAdapter.getDefaultAdapter().startDiscovery()

        if (isDiscovering) {
            Log.i("$TAG/pairDevice", "In discovery mode")
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
        Log.d(
            TAG, "Attempting to connect to \"${bluetoothDevice.name}...\""
        )
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            requireActivity().unregisterReceiver(pairDeviceReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "pairDeviceReceiver was never registered", e)
        }
        try {
            requireActivity().unregisterReceiver(deviceStatusReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "deviceStatusReceiver was never registered", e)
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

    // Called when the QR Code scanner successfully scans.
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

    companion object {
        fun newInstance() = ConnectFragment()
    }
}