package com.game.gamepad.activity

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.View
import com.game.gamepad.R
import kotlinx.android.synthetic.main.save_config_layout.*

class SaveConfigDialog(context: Context): Dialog(context), View.OnClickListener {
    private lateinit var listener: SaveConfigDialogListener

    fun setListener(listener: SaveConfigDialogListener):SaveConfigDialog{
        this.listener = listener
        return this
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.save_config_layout)
        val m = window!!.windowManager
        val d = m.defaultDisplay
        val p = window!!.attributes
        val size = Point()
        d.getSize(size)
        p.width = (size.x * 0.8).toInt()//是dialog的宽度为app界面的80%
        window!!.attributes = p

        saveConfigCancel.setOnClickListener(this)
        saveConfigConfirm.setOnClickListener(this)
    }

    interface SaveConfigDialogListener{
        fun onSaveDialogOver(name:String)
    }

    override fun onClick(v: View?) {
        if (v == null)return
        when(v.id){
            R.id.saveConfigCancel->{ }

            R.id.saveConfigConfirm->{
                var name = saveConfigEditName.text.toString()
                if (name == "")name = "default.json"
                if (!name.endsWith(".json"))name+=".json"
                //没有后缀就添加后缀，为了后面方便管理
                listener.onSaveDialogOver(name)
            }
        }
        dismiss()

    }
}