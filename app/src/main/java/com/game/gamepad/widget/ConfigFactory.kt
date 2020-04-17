package com.game.gamepad.widget

import android.content.Context
import android.os.Vibrator
import android.view.ViewGroup
import com.google.gson.Gson

object ConfigFactory{
    fun loadFromString(context: Context,vibrator: Vibrator,viewGroup: ViewGroup,json:String):ArrayList<GameButton>{
        val gson = Gson()
        val configBean = gson.fromJson<ConfigBean>(json,ConfigBean::class.java)
        val buttons = ArrayList<GameButton>()
        configBean.buttons.forEach { b->
            buttons.add(GameButton(context,viewGroup,vibrator,b.key,b.x,b.y))
        }
        return buttons
    }
}