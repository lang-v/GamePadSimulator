package com.game.gamepad

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import com.game.gamepad.widget.GameButton
import com.smarx.notchlib.NotchScreenManager
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : Activity() {
    private val tag = "sl"
    private val gameButtonList = ArrayList<GameButton>()
    private val vibrator: Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置全屏
        NotchScreenManager.getInstance().setDisplayInNotch(this)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {

        homeTop.showMode = SwipeLayout.ShowMode.LayDown
        addButton.setOnClickListener { _ ->
            createButton()
        }
        var isShow = false
        modifconfig.setOnClickListener { _ ->
            if (gameButtonList.isNotEmpty()) {
                isShow = !isShow
                val tmp = isShow
                gameButtonList.forEach { btn ->
                    btn.setShowClose(tmp)
                }
            }
        }
    }

    private fun createButton() {
        val key = keyValue.selectedItem.toString()
        if (key == "") return
        var xValue: Float = 0f
        var yValue: Float = 0f
        try {
            xValue = x.text.toString().toFloat()
            yValue = y.text.toString().toFloat()
        }catch (e:Exception){ }
        val gameButton = GameButton(home.context, home, vibrator, key, xValue, yValue)
        gameButtonList.add(gameButton)
    }
}
