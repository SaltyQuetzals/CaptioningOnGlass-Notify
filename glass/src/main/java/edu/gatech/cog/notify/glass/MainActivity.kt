package edu.gatech.cog.notify.glass

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import edu.gatech.cog.notify.glass.fragments.ConnectFragment
import org.greenrobot.eventbus.EventBus

class MainActivity : FragmentActivity() {

    private lateinit var gestureDetector: GlassGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: Add in flag to keep screen on?

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        gestureDetector =
            GlassGestureDetector(this, object : GlassGestureDetector.OnGestureListener {
                override fun onGesture(gesture: GlassGestureDetector.Gesture?): Boolean {
                    val isHandled = when (gesture) {
                        GlassGestureDetector.Gesture.TAP -> true
                        else -> false
                    }

                    if (isHandled) {
                        EventBus.getDefault().post(GlassGesture(gesture!!))
                    }

                    return isHandled
                }
            })

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, ConnectFragment.newInstance())
            .commit()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event?.let {
            return gestureDetector.onTouchEvent(it)
        }
        return super.onGenericMotionEvent(event)
    }
}