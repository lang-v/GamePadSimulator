package com.game.gamepad.widget

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import com.game.gamepad.R


class GameButton(
    private val context: Context,
    private val viewGroup: RelativeLayout,
    vibrator: Vibrator,
    key: String = "",
    x: Float,
    y: Float
) : View.OnClickListener,
    View.OnLongClickListener, View.OnTouchListener {
    private val TAG = "GameButton"
    private var key = ""
    private var keyState = false//true 对应按下，false 对应抬起
    private var MOVESTATE = false//移动
    private var SHOWCLOSE = false//显示关闭按钮
    //这里的attachtoroot是真的坑，直接添加到根视图下了
    private var layout: RelativeLayout = LayoutInflater.from(context).inflate(
        R.layout.game_button_layout_ring,
        viewGroup,
        false
    ) as RelativeLayout
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
        button.setOnClickListener(this)
        close.setOnClickListener(this)
    }

    /**
     * 用于显示关闭按钮
     */
    fun setShowClose(state: Boolean) {
        SHOWCLOSE = state
        if (SHOWCLOSE) {
            if (close.visibility != View.VISIBLE)
                close.visibility = View.VISIBLE
        } else {
            if (close.visibility != View.INVISIBLE)
                close.visibility = View.INVISIBLE
        }
    }

    /**
     * 用于移动按钮的相关设置
     */
    fun setMove(state: Boolean) {
        MOVESTATE = state
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
            R.id.btn -> {
                keyState = !keyState
                if (keyState) {
                    //todo 发送蓝牙消息 按下此键
                } else {
                    //todo 发送蓝牙消息 抬起此键盘
                }
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        MOVESTATE = !MOVESTATE
        Log.e("SL", "movestate:$MOVESTATE")
        setShowClose(MOVESTATE)
        //震动0.3s
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300, 1))
        } else {
            val patter = longArrayOf(0, 300)
            vibrator.vibrate(patter, 1)
            Thread(Runnable {
                Thread.sleep(300)
                this.vibrator.cancel()
            }).run()
        }
        return true
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || v == null) return false
        if (MOVESTATE && event.action == MotionEvent.ACTION_MOVE) {
            this.layout.x = event.rawX
            this.layout.y = event.rawY
            Log.e("SL", "move x =${event.rawX} y =${event.rawY}}")
        }
        return true
    }
}