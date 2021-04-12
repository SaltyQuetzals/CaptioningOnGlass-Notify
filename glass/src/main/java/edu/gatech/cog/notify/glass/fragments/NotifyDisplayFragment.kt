package edu.gatech.cog.notify.glass.fragments

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import edu.gatech.cog.notify.common.GLASS_SOUND_TAP
import edu.gatech.cog.notify.common.models.GlassNotification
import edu.gatech.cog.notify.glass.R
import edu.gatech.cog.notify.glass.databinding.FragmentNotifyDisplayBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.toObservable
import java.util.concurrent.TimeUnit

private val TAG = NotifyDisplayFragment::class.java.simpleName

class NotifyDisplayFragment : Fragment(R.layout.fragment_notify_display) {

    private var _binding: FragmentNotifyDisplayBinding? = null
    private val binding get() = _binding!!
    private lateinit var chunkedCaptionObservable: Observable<String>
    private lateinit var chunkedCaptionSubscribable: Disposable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotifyDisplayBinding.bind(view)
        this.onReceiveNotification(
            GlassNotification(
                "All right. Let's get started. You heard what the judge said. One man is " +
                        "dead, and now we must decide if another man will live or die. " +
                        "Premeditated murder is the most serious charge in our courts. If we " +
                        "vote guilty, we send him to the electric chair. That's mandatory.",
                false,
                17417
            )
        )
    }

    @Subscribe
    fun onReceiveNotification(glassNotification: GlassNotification) {
        // Reset the TextView content.
        requireActivity().runOnUiThread {
            binding.tvContent.text = ""
        }
        if (this::chunkedCaptionSubscribable.isInitialized && !chunkedCaptionSubscribable.isDisposed) {
            // We're about to replace the observable, so we need to clean up the disposable attached to it
            chunkedCaptionSubscribable.dispose()
        }
        Log.v(TAG, "GlassNotification\n${glassNotification.text}: ${glassNotification.isVibrate}")

        if (glassNotification.isVibrate) {
            // Glass XE doesn't have a vibrator... but the bone conduction speaker works decently well as one!
            // TODO: Add in a rumble track (?) and play using AudioManager
            (context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
                playSoundEffect(GLASS_SOUND_TAP)
            }
        }

        val chunks = glassNotification.text.split(" ").chunked(4) { it.joinToString(" ") }
        chunkedCaptionObservable =
            chunks.withIndex().toObservable().concatMap { (i, chunk) ->
                if (i == 0) {
                    Observable.just(chunk)
                } else {
                    Observable.just(" $chunk")
                        .delay(glassNotification.duration / chunks.size, TimeUnit.MILLISECONDS)
                }
            }

        chunkedCaptionSubscribable =
            chunkedCaptionObservable.observeOn(AndroidSchedulers.mainThread()).subscribe {
                binding.tvContent.append(it)
            }

    }

    private fun writeToPhone(data: Any) {
        EventBus.getDefault().post(data)
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
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    companion object {
        fun newInstance() = NotifyDisplayFragment()
    }
}