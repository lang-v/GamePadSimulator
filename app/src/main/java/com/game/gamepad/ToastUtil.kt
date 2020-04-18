package com.game.gamepad

import android.content.Context
import android.widget.Toast

object ToastUtil {
    private lateinit var context: Context
    fun init(context: Context){
        this.context = context
    }
    fun show(msg:String){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
    }
}