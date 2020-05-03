package com.game.gamepad.activity

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.View
import com.game.gamepad.R
import com.game.gamepad.adapter.ConfigListAdapter
import com.game.gamepad.config.ConfigFactory
import com.game.gamepad.config.ConfigItem
import kotlinx.android.synthetic.main.delete_config_layout.*

class DeleteConfigDialog(context: Context):Dialog(context), View.OnClickListener {
    private lateinit var adapter:ConfigListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delete_config_layout)
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
            array.add(ConfigItem(s, false))
        }
        //多选
        adapter = ConfigListAdapter(context, array, R.layout.choose_config_item,true)
        deleteConfigList.adapter = adapter
        adapter.notifyDataSetChanged()

        deleteConfigCancel.setOnClickListener(this)
        deleteConfigConfirm.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == null)return
        when(v.id){
            R.id.deleteConfigCancel->{ }
            R.id.deleteConfigConfirm->{
                val arrayList = ArrayList<String>()
                val tmp = adapter.getSelected()
                var msg = ""
                tmp.forEach{
                    arrayList.add(it.name)
                    msg+=it.name + "\n"
                }
                AlertDialog.Builder(context)
                    .setTitle("确认删除以下配置吗？")
                    .setMessage(msg)
                    .setPositiveButton("确认删除")
                    { _, _ -> ConfigFactory.deleteConfig(arrayList) }
                    .setNegativeButton("取消")
                    {_,_->dismiss()}
                    .show()
            }
        }
        dismiss()
    }
}