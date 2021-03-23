package edu.gatech.cog.notify.glass

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import edu.gatech.cog.notify.glass.fragments.ConnectFragment

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO: Add in flag to keep screen on?
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, ConnectFragment.newInstance())
            .commit()
    }
}