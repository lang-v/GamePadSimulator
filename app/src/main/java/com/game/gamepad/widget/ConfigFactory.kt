package com.game.gamepad.widget

import android.content.Context
import android.content.SharedPreferences
import android.view.ViewGroup
import com.game.gamepad.utils.ToastUtil
import com.google.gson.Gson

object ConfigFactory{
    private lateinit var context:Context
    private lateinit var sharedPreferences: SharedPreferences
    fun init(context: Context){
        this.context = context
        sharedPreferences = context.getSharedPreferences("config",Context.MODE_PRIVATE)
    }

    fun loadConfig(listener: GameButton.RemoveListener,viewGroup: ViewGroup, configName:String="default"):ArrayList<GameButton>{
        val json = get(configName) ?: return ArrayList()
//        Log.e("SL","loadJson : $json")
        val gson = Gson()
        val configBean: ConfigBean?
        try {
            configBean = gson.fromJson<ConfigBean>(json,ConfigBean::class.java)
        }catch (e:Exception){
            e.printStackTrace()
            ToastUtil.show("载入配置失败")
            return ArrayList()
        }
        val buttons = ArrayList<GameButton>()
        configBean.buttons.forEach { b->
            buttons.add(GameButton(context,viewGroup,listener,b.key,b.x,b.y,b.r))
        }
        return buttons
    }

    fun save(configName:String = "default",json:String){
//        Log.e("SL","saveJson : $json")
        val editor = sharedPreferences.edit()
        editor.putString(configName,json)
        editor.apply()
    }

    fun get(configName: String= "default"):String?{
        return sharedPreferences.getString(configName,null)
    }
}