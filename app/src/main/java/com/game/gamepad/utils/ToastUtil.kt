package com.game.gamepad.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ToastUtil {
    private lateinit var context: Context
    fun init(context: Context){
        ToastUtil.context = context
    }
    fun show(msg:String){
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
        }
    }
}