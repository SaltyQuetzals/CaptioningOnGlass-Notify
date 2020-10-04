package edu.gatech.cog.notify.glass

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import edu.gatech.cog.notify.glass.fragments.ConnectFragment
import org.greenrobot.eventbus.EventBus

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: Add in flag to keep screen on

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        fragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, ConnectFragment.newInstance())
            .commit()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        EventBus.getDefault().post(KeyCode(keyCode))
        return super.onKeyDown(keyCode, event)
    }
}