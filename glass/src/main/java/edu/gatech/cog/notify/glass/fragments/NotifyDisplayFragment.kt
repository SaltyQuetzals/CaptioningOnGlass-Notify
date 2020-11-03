package edu.gatech.cog.notify.glass.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import edu.gatech.cog.notify.common.GLASS_SOUND_TAP
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.glass.Constants
import edu.gatech.cog.notify.glass.GlassGesture
import edu.gatech.cog.notify.glass.R
import edu.gatech.cog.notify.glass.databinding.FragmentNotifyDisplayBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

private val TAG = NotifyDisplayFragment::class.java.simpleName

class NotifyDisplayFragment : Fragment(R.layout.fragment_notify_display) {

    private var _binding: FragmentNotifyDisplayBinding? = null
    private val binding get() = _binding!!

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<GlassNotification>(Constants.MESSAGE)?.let {
                Log.v(TAG, "GlassNotification\n${it.text}: ${it.isVibrate}")

                if (it.isVibrate) {
                    // Glass XE doesn't have a vibrator... but the bone conduction speaker works decently well as one!
                    // TODO: Add in a rumble track (?) and play using AudioManager
                    (context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
                        playSoundEffect(GLASS_SOUND_TAP)
                    }
                }

                requireActivity().runOnUiThread {
                    binding.tvContent.text = it.text
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotifyDisplayBinding.bind(view)

        requireActivity().registerReceiver(
            notificationReceiver,
            IntentFilter(Constants.INTENT_FILTER_NOTIFICATION)
        )
    }

    @Subscribe
    private fun onGesture(glassGesture: GlassGesture) {
        Log.v(TAG, glassGesture.gesture.name)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)

        try {
            requireActivity().unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "notificationReceiver not registered", e)
        }
    }

    companion object {
        fun newInstance() = NotifyDisplayFragment()
    }
}