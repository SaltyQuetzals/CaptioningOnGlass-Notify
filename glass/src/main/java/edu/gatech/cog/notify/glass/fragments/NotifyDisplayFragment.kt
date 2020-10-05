package edu.gatech.cog.notify.glass.fragments

import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import edu.gatech.cog.notify.common.GLASS_SOUND_TAP
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.glass.Constants
import edu.gatech.cog.notify.glass.R
import kotlinx.android.synthetic.main.fragment_notify_display.*

private val TAG = NotifyDisplayFragment::class.java.simpleName

class NotifyDisplayFragment : Fragment() {

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

                activity.runOnUiThread {
                    tvContent.text = it.text
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity.registerReceiver(
            notificationReceiver,
            IntentFilter(Constants.INTENT_FILTER_NOTIFICATION)
        )

        return inflater.inflate(R.layout.fragment_notify_display, container, false)!!
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            activity.unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "notificationReceiver not registered", e)
        }
    }

    companion object {
        fun newInstance() = NotifyDisplayFragment()
    }
}