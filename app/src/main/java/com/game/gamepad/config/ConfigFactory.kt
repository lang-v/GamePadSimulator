package com.game.gamepad.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.ViewGroup
import com.game.gamepad.utils.SnackbarUtil
import com.game.gamepad.widget.GameButton
import com.google.gson.Gson
import java.io.*

object ConfigFactory{
    private lateinit var context:Context
    private lateinit var sharedPreferences: SharedPreferences
    fun init(context: Context){
        ConfigFactory.context = context
        sharedPreferences = context.getSharedPreferences("config",Context.MODE_PRIVATE)
    }

    fun loadConfig(listener: GameButton.RemoveListener, viewGroup: ViewGroup, configName:String="default"):ArrayList<GameButton>{
        val json = get(configName) ?: return ArrayList()
//        Log.e("SL","loadJson : $json")
        val gson = Gson()
        val configBean: ConfigBean?
        try {
            configBean = gson.fromJson(String(json.toByteArray(),Charsets.UTF_8), ConfigBean::class.java)
        }catch (e:Exception){
            e.printStackTrace()
            SnackbarUtil.show("载入配置失败")
            return ArrayList()
        }
        val buttons = ArrayList<GameButton>()
        configBean.buttons.forEach { b->
            buttons.add(
                GameButton(
                    context,
                    viewGroup,
                    listener,
                    b.type,
                    b.key,
                    b.text,
                    b.x,
                    b.y,
                    b.r
                )
            )
        }
        return buttons
    }

    fun save(configName:String = "default.json",json:String){
//        Log.e("SL","saveJson : $json")
        //val editor = sharedPreferences.edit()
        //editor.putString(configName,json)
        //editor.apply()
        saveToFile(json, configName)
    }

    fun get(configName: String= "default.json"):String?{
        var json = loadInFile(configName)
        if (json == "")return null
        return json
        //return sharedPreferences.getString(configName,null)
    }

    //保存到config目录下
    private fun saveToFile(json: String,name:String){
        var file:FileOutputStream? = null
        try {
            file = context.openFileOutput(name,Context.MODE_PRIVATE)
            file.write(json.toByteArray())
            file.flush()
        }catch (e:Exception){
            e.printStackTrace()
            SnackbarUtil.show("保存配置失败，请检查读写权限")
        }finally {
            file?.close()
        }
    }

    //获取上次加载的是哪个配置
    fun getDefault():String{
        return sharedPreferences.getString("default","default.json")!!
    }

    //记录加载的配置，便于下次启动自动加载
    private fun saveDefault(name: String){
        val edit = sharedPreferences.edit()
        edit.putString("default",name)
        edit.apply()
    }

    fun getFileList():ArrayList<String> {
        //使用openFileOutput 打开的目录就是这个
        val str = "/data/data/com.game.gamepad/files"
        val path = File(str)
        val files = path.listFiles()
        val array = ArrayList<String>()
        if (files == null)return array
        var fileName:String
        for (i in files.indices){
            fileName = files[i].name
            //只要以json结尾的
            if (fileName.endsWith(".json"))
                array.add(fileName)
        }
        return array
    }

    private fun loadInFile(name:String):String{
        var json = ""
        var file:FileInputStream?=null
        var fileReader:InputStreamReader?=null
        try {
            file = context.openFileInput(name)
            fileReader = InputStreamReader(file,"UTF-8")
            val tmp = fileReader.readLines()
                for (s in tmp) {
                    json += s
                }
            //保存加载记录
            saveDefault(name)
        }catch (e:Exception){
            e.printStackTrace()
            SnackbarUtil.show("读取配置失败，请检查读写权限")
            json = ""
        }finally {
            fileReader?.close()
            file?.close()
        }
        return json
    }

    fun deleteConfig(configs:ArrayList<String>){
        val path = context.filesDir
        for (config in configs) {
            val file = File(path,config)
            if (file.exists()) {
                try {
                    file.delete()
                    Log.e("ConfigFactory","成功删除 <$config>")
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

}