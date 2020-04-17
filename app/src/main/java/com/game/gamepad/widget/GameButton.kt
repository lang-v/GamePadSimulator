package com.game.gamepad.widget

import android.content.Context
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.game.gamepad.bluetooth.BlueTooth
import com.game.gamepad.R
import com.game.gamepad.bluetooth.BlueToothTool

//import io.javac.ManyBlue.ManyBlue


class GameButton(
    private val context: Context,
    private val viewGroup: ViewGroup,
    vibrator: Vibrator,
    key: String = "",
    x: Float,
    y: Float
) : View.OnClickListener, View.OnTouchListener {
    private val TAG = "GameButton"
    private var key = ""
    private var keyState = false//true 对应按下，false 对应抬起
    private var MOVESTATE = false//移动
    private var SHOWCLOSE = false//显示关闭按钮
    //这里的attachtoroot是真的坑，直接添加到根视图下了
    private var layout: LinearLayout = LayoutInflater.from(context).inflate(
        R.layout.game_button_layout_ring,
        viewGroup,
        false
    ) as LinearLayout
    private var button: Button
    private var close: Button
    //震动
    private var vibrator = vibrator

    init {
        layout.x = x
        layout.y = y
        viewGroup.addView(layout)
        button = layout.findViewById(R.id.btn)
        close = layout.findViewById(R.id.close)
        setText(key)
        //button.setOnClickListener(this)
        close.setOnClickListener(this)
        button.setOnTouchListener(this)
    }

    /**
     * 按钮进入可操作状态 移动、删除
     */
    fun setModifyState(state: Boolean) {
        MOVESTATE = state
        if (MOVESTATE) {
            if (close.visibility != View.VISIBLE)
                close.visibility = View.VISIBLE
        } else {
            if (close.visibility != View.INVISIBLE)
                close.visibility = View.INVISIBLE
        }
    }

    fun setText(text: String) {
        key = text
        button.text = key
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.close -> {
                viewGroup.removeView(layout)//删除这个按钮
            }
        }
    }

//    override fun onLongClick(v: View?): Boolean {
//        MOVESTATE = !MOVESTATE
//        Log.e("SL", "movestate:$MOVESTATE")
//        setShowClose(MOVESTATE)
//        //震动0.3s
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator.vibrate(VibrationEffect.createOneShot(300, 1))
//        } else {
//            val patter = longArrayOf(0, 300)
//            vibrator.vibrate(patter, 1)
//            Thread(Runnable {
//                Thread.sleep(300)
//                this.vibrator.cancel()
//            }).run()
//        }
//        return true
//    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || v == null) return false
        if (MOVESTATE) {
            this.layout.x = event.rawX-layout.width/2+close.width
            this.layout.y = event.rawY-layout.height/2
            return true
        }
        if (v.id == R.id.btn && (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP)){
            keyState = !keyState
//            ManyBlue.blueWriteData("$key:$keyState",ManyBlue.getConnTagAll())
            //if (BlueTooth.connected) BlueTooth.send(key,keyState)
            if (BlueToothTool.isConnected())BlueToothTool.sendMsg("$key:$keyState")
        }
        return false
    }
}