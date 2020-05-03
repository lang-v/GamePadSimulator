package com.game.gamepad.activity

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.View
import com.game.gamepad.R
import com.game.gamepad.adapter.ConfigListAdapter
import com.game.gamepad.config.ConfigFactory
import com.game.gamepad.config.ConfigItem
import kotlinx.android.synthetic.main.choose_config_layout.*

class ChooseConfigDialog(context: Context, style:Int =0) : Dialog(context,style), View.OnClickListener {
    //上一次加载的配置
    private var selectedName = ConfigFactory.getDefault()
    private lateinit var adapter: ConfigListAdapter

    private lateinit var listenerChooseConfig: ChooseConfigDialogListener

    fun setListener(listenerChooseConfig: ChooseConfigDialogListener):ChooseConfigDialog{
        this.listenerChooseConfig = listenerChooseConfig
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ConfigDialog","onCreate")
        //设置界面布局
        setContentView(R.layout.choose_config_layout)
        val m = window!!.windowManager
        val d = m.defaultDisplay
        val p = window!!.attributes
        val size = Point()
        d.getSize(size)
        p.width = (size.x * 0.8).toInt()//是dialog的宽度为app界面的80%
        window!!.attributes = p

        val array = ArrayList<ConfigItem>()
        val arrayTmp = ConfigFactory.getFileList()
        for (s in arrayTmp) {
            array.add(ConfigItem(s, selectedName == s))
        }
        adapter = ConfigListAdapter(context,array,R.layout.choose_config_item,false)
        chooseConfigList.adapter = adapter
        adapter.notifyDataSetChanged()
        chooseConfigCancel.setOnClickListener(this)
        chooseConfigConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == null)return
        when(v.id){
            R.id.chooseConfigConfirm->{
                val newSelectedName = adapter.getSelected()[0].name
                listenerChooseConfig.onChooseDialogOver(newSelectedName!="",newSelectedName)
            }

            R.id.chooseConfigCancel->{
                listenerChooseConfig.onChooseDialogOver(false,"")
            }
        }
        dismiss()
    }

    interface ChooseConfigDialogListener{
        /**
         * @param changed 配置文件是否改变
         * @param name 新选中的配置文件名
         */
        fun onChooseDialogOver(changed:Boolean, name:String)
    }
}