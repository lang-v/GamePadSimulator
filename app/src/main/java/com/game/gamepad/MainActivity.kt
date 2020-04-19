package com.game.gamepad

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.game.gamepad.bluetooth.BlueToothTool
import com.game.gamepad.utils.EasyRequest
import com.game.gamepad.utils.ToastUtil
import com.game.gamepad.widget.ConfigFactory
import com.game.gamepad.widget.GameButton
import com.smarx.notchlib.NotchScreenManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import java.lang.Exception
import kotlin.collections.ArrayList

//todo
// 1.给按键配置做一个本地存储
// 2.控件大小可控，
// 3.制作摇杆控件
// 4.等待修复偶尔卡顿的问题
// 5.美化菜单界面
class MainActivity : Activity(), View.OnClickListener, BlueToothTool.BluetoothListen {
    private val tag = "MainActivity"
    //记录连续按两次退出
    private var lastTime: Long = 0L
    //管理权限
    private val permissionManager = EasyRequest()
    //用来标记当前所有按钮是否可以修改。
    private var isModifying = false
    //所有button的集合 指当前配置的button
    private val gameButtonList = ArrayList<GameButton>()
    //蓝牙设备
    private val deviceList = ArrayList<BluetoothDevice>()
    //蓝牙名称集合
    private val deviceNameList = ArrayList<String>()
    //蓝牙列表适配器
    private val deviceAdapter by lazy {
        ArrayAdapter<String>(this, R.layout.simple_spinner_item, deviceNameList)
    }
    //震动
    private val vibrator: Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    //刷新设备列表
    private val FRESHDEVEICEADAPTER = 1
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                FRESHDEVEICEADAPTER -> {
                    //deviceSpinner.adapter = deviceAdapter
                    deviceAdapter.notifyDataSetChanged()
                }
            }
            super.handleMessage(msg)
        }
    }
    private val removeListener = object : GameButton.RemoveListener {
        override fun remove(button: GameButton) {
            //home.removeView(button.getLayou())
            gameButtonList.remove(button)
        }
    }

    private val bluetoothStateChangedReceive = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent!!.action
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_ON -> {
                            Thread {
                                loadDevice()
                            }.run()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            BlueToothTool.disConnect()
                            Toast.makeText(this@MainActivity, "蓝牙关闭", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    ToastUtil.show("扫描完毕")
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (!deviceList.contains(device)) {
                        deviceList.add(device)
                        deviceNameList.add(device.name)
                        val msg = Message.obtain()
                        msg.what = FRESHDEVEICEADAPTER
                        handler.sendMessage(msg)
                    }
                }
            }
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                //蓝牙连接被切断
                val device: BluetoothDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val name = device.name;
                Toast.makeText(this@MainActivity, name + "的连接被断开", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //f第三方库，适配异形屏
        NotchScreenManager.getInstance().setDisplayInNotch(this)
        //设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        requestPermission()
        registerBroadcast()
    }

    override fun onDestroy() {
        BlueToothTool.disConnect()
        unregisterBroadcast()
        super.onDestroy()
    }

    //发出蓝牙扫描的接受广播
    private fun registerBroadcast() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //动作状态发生了变化
        registerReceiver(bluetoothStateChangedReceive, filter)
    }


    private fun unregisterBroadcast() {
        unregisterReceiver(bluetoothStateChangedReceive)
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionManager.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                permissionManager.request(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    1
                )
            } else {
                if (permissionManager.checkPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    permissionManager.request(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        2
                    )
                } else {
                    init()
                }
            }
        } else {
            init()
        }
    }

    private fun init() {
        BlueToothTool.setListener(this)
        //deviceSpinner.adapter = deviceAdapter
        //初始化蓝牙
        BlueToothTool.init()
        //初始化toast工具
        ToastUtil.init(this)
        if (BlueToothTool.isEnable()) {
            val devices = BlueToothTool.getDevices()
            devices.forEach { d ->
                deviceList.add(d)
                deviceNameList.add(d.name)
            }
        }
        //用于保存配置
        ConfigFactory.init(this)
        deviceSpinner.adapter = deviceAdapter
        deviceAdapter.notifyDataSetChanged()
        //val adapter = ArrayAdapter<String>(this, R.layout.simple_spinner_item, array)
        //连接和断开连接
        connectDevice.setOnClickListener(this)
        disconnectDevice.setOnClickListener(this)
        //添加按钮
        addButton.setOnClickListener(this)
        //选择配置
        chooseConfig.setOnClickListener(this)
        //保存配置
        saveConfig.setOnClickListener(this)
        //返回到主页面
        back.setOnClickListener(this)
        //设置页面
        setting.setOnClickListener(this)
        //修改配置
        modifyConfig.setOnClickListener(this)
        //刷新设备
        reFresh.setOnClickListener(this)
        //自动选择配置
        //chooseConfig.callOnClick()
    }

    private fun loadDevice() {
        val devices = BlueToothTool.getDevices()
        BlueToothTool.search()
        devices.forEach { d ->
            deviceList.add(d)
            deviceNameList.add(d.name)
        }
        val msg = Message.obtain()
        msg.what = FRESHDEVEICEADAPTER
        handler.sendMessage(msg)
    }

    private fun createButton() {
        val key = keyValue.selectedItem.toString()
        if (key == "") return
        var xValue = 500f
        var yValue = 500f
        var radiusValue = 100
        try {
            val xText = x.text.toString()
            val yText = y.text.toString()
            val rText = radius.text.toString()
            if (xText != "")
                xValue = xText.toFloat()
            if (yText != "")
                yValue = yText.toFloat()
            if (rText != "")
                radiusValue = rText.toInt()
        } catch (e: Exception) {
            Toast.makeText(this, "参数有误", Toast.LENGTH_SHORT).show()
            return
        }
        val gameButton =
            GameButton(home.context, home, removeListener, key, xValue, yValue, radiusValue)
        gameButtonList.add(gameButton)
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v) {
            //添加按钮
            addButton -> {
                createButton()
            }
            //打开设置
            setting -> {
                settingLayout.visibility = View.VISIBLE
            }
            //设置界面的返回按钮
            back -> {
                settingLayout.visibility = View.GONE
            }
            //对按钮进行操作
            modifyConfig -> {
                if (gameButtonList.isNotEmpty()) {
                    isModifying = !isModifying
                    val tmp = isModifying
                    gameButtonList.forEach { btn ->
                        btn.setModifyState(tmp)
                    }
                }
            }
            //加载设置
            chooseConfig -> {
                Thread {
                    for (index in gameButtonList.indices) {
                        gameButtonList[index].destroy(false)
                    }
                    gameButtonList.clear()
                    val list = ConfigFactory.loadConfig(removeListener, home)
                    for (gameButton in list) {
                        gameButtonList.add(gameButton)
                    }
                }.run()
            }
            //保存设置
            saveConfig -> {
                Thread {
                    var json = ""
                    for ((index, gameButton) in gameButtonList.withIndex()) {
                        json += gameButton.getBean()
                        if (index != gameButtonList.size - 1)
                            json += ","
                    }
                    ConfigFactory.save(
                        json =
                        "{\"name\":\"default\",\"desc\":\"nothing\",\"buttons\":[$json]}"
                    )
                }.run()
            }
            //连接设备
            connectDevice -> {
                val index = deviceSpinner.selectedItemPosition
                if (index == -1) return
                BlueToothTool.preConnect(index)
                BlueToothTool.connect()
            }
            //断开连接
            disconnectDevice -> {
                BlueToothTool.disConnect()
            }
            //刷新设备
            reFresh -> {
                BlueToothTool.search()
            }
        }
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime <= 1500) {//1s
            return super.onBackPressed()
        } else {
            lastTime = currentTime
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1, 2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {
                    Toast.makeText(this, "未授权,请手动授权", Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(this)
                        .setTitle("请求授权")
                        .setMessage("是否手动授权")
                        .setPositiveButton("是") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("否") { _, _ -> finish() }
                        .show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            if (!permissionManager.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || !permissionManager.checkPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                requestPermission()
            } else {
                finish()
            }
        }
    }

    override fun connected(connected: Boolean) {
        runOnUiThread {
            if (connected) {
                vibrator.vibrate(300)
                connectState.isActivated = true
                Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
                connectDevice.isEnabled = false
                disconnectDevice.isEnabled = true
                //开启蓝牙活跃
                Thread {
                    BlueToothTool.positive()
                }.run()
            } else {
                connectState.isActivated = false
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
                connectDevice.isEnabled = true
                disconnectDevice.isEnabled = false
            }
        }
    }
}

