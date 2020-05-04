package com.game.gamepad.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SnackbarUtil {
    private lateinit var view: View
    fun init(view: View){
        SnackbarUtil.view = view
    }
    fun show(msg:String){
        GlobalScope.launch(Dispatchers.Main) {
            val bar = Snackbar.make(view,msg,Snackbar.LENGTH_SHORT)
            bar.setAction("确认") { bar.dismiss() }.show()
        }
    }
}