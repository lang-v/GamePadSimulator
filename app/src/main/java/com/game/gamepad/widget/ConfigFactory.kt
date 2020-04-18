package com.game.gamepad.widget

import android.content.Context
import android.content.SharedPreferences
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import com.google.gson.Gson

object ConfigFactory{
    private lateinit var context:Context
    private lateinit var sharedPreferences: SharedPreferences
    fun init(context: Context){
        this.context = context
        sharedPreferences = context.getSharedPreferences("config",Context.MODE_PRIVATE)
    }

    fun loadConfig(vibrator: Vibrator, viewGroup: ViewGroup, configName:String="default"):ArrayList<GameButton>{
        val json = get(configName) ?: return ArrayList()
        Log.e("SL","loadJson : $json")
        val gson = Gson()
        val configBean = gson.fromJson<ConfigBean>(json,ConfigBean::class.java)
        val buttons = ArrayList<GameButton>()
        configBean.buttons.forEach { b->
            buttons.add(GameButton(context,viewGroup,vibrator,b.key,b.x,b.y,b.r))
        }
        return buttons
    }

    fun save(configName:String = "default",json:String){
        Log.e("SL","saveJson : $json")
        val editor = sharedPreferences.edit()
        editor.putString(configName,json)
        editor.apply()
    }

    fun get(configName: String= "default"):String?{
        return sharedPreferences.getString(configName,null)
    }
}