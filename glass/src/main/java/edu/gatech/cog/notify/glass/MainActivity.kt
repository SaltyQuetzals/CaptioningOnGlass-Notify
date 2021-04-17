package edu.gatech.cog.notify.glass

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import edu.gatech.cog.notify.glass.fragments.ConnectFragment
import edu.gatech.cog.notify.glass.fragments.NotifyDisplayFragment
import org.w3c.dom.Text



class MainActivity : FragmentActivity() {

    private lateinit var scrollView: TextView;
    private lateinit var tvContent: View;

    private lateinit var gestureDetector: GlassGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        scrollView = findViewById<View>(R.id.scrollView) as ScrollView //App crashes, nullPointer
//        tvContent = findViewById<View>(R.id.tvContent) as TextView

//        scrollView = findViewById<View>(R.id.tvContent) as TextView //App crashes
//        scrollView.setMovementMethod(ScrollingMovementMethod())


        // TODO: Add in flag to keep screen on?

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }

//        addTextToTextView()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.frameLayoutMain, NotifyDisplayFragment.newInstance())
            .commit()
    }



//    fun addTextToTextView() {
//        val strTemp = "TestlineOne\nTestlineTwo\n"
//
//        //append the new text to the bottom of the TextView
//        tvContent.append(strTemp)
//
//        //scroll chat all the way to the bottom of the text
//        //HOWEVER, this won't scroll all the way down !!!
//        //chat_ScrollView.fullScroll(View.FOCUS_DOWN);
//
//        //INSTEAD, scroll all the way down with:
//        scrollView.post(Runnable { scrollView.fullScroll(View.FOCUS_DOWN) })
//    }


}