package com.game.gamepad.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.game.gamepad.R
import com.game.gamepad.utils.BlueToothUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameButton(
    context: Context,
    private val viewGroup: ViewGroup,
    listener: RemoveListener,
    type :Int = 0,
    key: String = "",
    text: String = "",
    x: Float,
    y: Float,
    private var radius: Int
) : View.OnClickListener, View.OnTouchListener {

    companion object {
        val RING = 0
        val SQUARE = 1
    }

    private val TAG = "GameButton"
    private var key = key
    private var text = text
    private var type = type
    private var MOVESTATE = false//移动
    private var listener: RemoveListener

    //这里的attachtoroot是真的坑，直接添加到根视图下了
    private var layout: LinearLayout = LayoutInflater.from(context).inflate(
        if(type == RING) R.layout.game_button_layout_ring
        else R.layout.game_button_layout_square,
        viewGroup,
        false
    ) as LinearLayout
    private lateinit var button: Button
    private lateinit var close: Button

    init {
        layout.x = x
        layout.y = y
        this.listener = listener
        GlobalScope.launch(Dispatchers.Main) {
            viewGroup.addView(layout)
            button = layout.findViewById(R.id.btn)
            close = layout.findViewById(R.id.close)
            setText(text)
            //button.setOnClickListener(this)
            button.layoutParams.apply {
                height = radius * 2
                width = radius * 2
            }
            close.setOnClickListener(this@GameButton)
            button.setOnTouchListener(this@GameButton)
        }
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

    fun destroy(remove: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            viewGroup.removeView(layout)
        }
        if (remove)
            listener.remove(this)
    }

    private fun setText(text: String) {
        button.text = text
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            R.id.close -> {
                destroy(true)//删除这个按钮
            }
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        if (event == null || v == null) return false
        if (MOVESTATE) {
            this.layout.x = event.rawX - layout.width/2
            this.layout.y = event.rawY - layout.height / 2
            return false
        }
        if (v.id == R.id.btn) {
            //Log.e("SL","event action is ${event.action}")
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    sendMsg(true)
                }
                MotionEvent.ACTION_UP -> {
                    //Log.e("SL", "is cancel ${MotionEvent.ACTION_CANCEL == event.action}")
                    //ToastUtil.show("SL is cancel ${MotionEvent.ACTION_CANCEL == event.action}")
                    //button.isPressed = false
                    sendMsg(false)
                }
            }
        }
        return false
    }

    //避免多线程下按下和抬起事件乱序
    @Synchronized
    private fun sendMsg(state: Boolean) {
        Thread {
            if (BlueToothUtil.isConnected())
                BlueToothUtil.sendMsg(
                    "$key:${
                    if (state) "true"
                    else "false"
                    }"
                )
        }.run()
    }

    //返回这个按钮的属性，便于保存到本地，再恢复，假装序列化
    fun getBean(): String {
        return """
            {"height":${button.height},"key":"$key","text":"$text","type":$type,"width":${button.width},"x":${layout.x},"y":${layout.y},"r":${radius}}
        """.trimIndent()
    }

    interface RemoveListener {
        fun remove(button: GameButton)
    }
}