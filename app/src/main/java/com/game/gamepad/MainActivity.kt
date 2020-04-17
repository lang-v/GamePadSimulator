package com.game.gamepad

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.daimajia.swipe.SwipeLayout
import com.game.gamepad.bluetooth.BlueTooth
import com.game.gamepad.bluetooth.BlueToothTool
import com.game.gamepad.bluetooth.BluetoothReceiver
import com.game.gamepad.permission.EasyRequest
import com.game.gamepad.widget.GameButton
import com.game.gamepad.widget.Logger
import com.inuker.bluetooth.library.utils.BluetoothUtils
import com.smarx.notchlib.NotchScreenManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

//todo
// 1.给按键配置做一个本地存储
// 2.控件大小可控，
// 3.制作摇杆控件
// 4.等待修复偶尔卡顿的问题
class MainActivity : Activity(), View.OnClickListener {
    private val tag = "MainActivity"
    private var lastTime: Long = 0L
    private val permissionManager = EasyRequest()
    private var isModifying = false
    private val gameButtonList = ArrayList<GameButton>()
    private val vibrator: Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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
        //registerBroadcast()
        requestPermission()
    }

//    private fun lanya() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        val deviceSet = bluetoothAdapter.bondedDevices
//        val array = ArrayList<String>()
//        deviceSet.forEach { d ->
//            array.add(d.name)
//        }
//        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, array)
//        deviceSpinner.adapter = adapter
//
//        connectDevice.setOnClickListener { _ ->
//            val index = deviceSpinner.selectedItemPosition
//            val device = bluetoothAdapter.getRemoteDevice(deviceSet.toList()[index].address)
//            bluetoothSocket =
//                device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
//            bluetoothSocket.connect()
//        }
//    }

    override fun onDestroy() {
        //unregisterBroadcast()
        super.onDestroy()
    }

    //发出蓝牙扫描的接受广播
    private fun registerBroadcast() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED) //状态改变
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) //行动扫描模式改变了
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED) //动作状态发生了变化
        //registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBroadcast() {
        //unregisterReceiver(bluetoothReceiver)
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
        //全局单例,初始化蓝牙并开启
//        BlueTooth.create(this)
//        BlueTooth.search()
//        BlueTooth.setListener(bleCallBack)
        //ManyBlue.blueStartScaner(3000)
        //deviceList = ManyBlue.getConnDeviceAll() as ArrayList<BluetoothDevice>
        //ManyBlue.blueWriteData("123",null)
        //if (ManyBlue.blueEnableState(this))
        //  ManyBlue.blueEnable(true)
        //ManyBlue.blueSupport(this)
        //ManyBlue.DEBUG = true
        //ManyBlue.dealtListener()
        BlueToothTool.setListener(object : BlueToothTool.ConnectListen {
            override fun connected(connected: Boolean) {
                GlobalScope.launch(Dispatchers.Main) {

                    Toast.makeText(
                        this@MainActivity,
                        if (connected) "连接成功"
                        else
                            "连接失败", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun toast(msg: String) {
                //Toast.makeText(this@MainActivity,msg,Toast.LENGTH_SHORT).show()
            }
        })
        BlueToothTool.enableBT()
        val deviceList = BlueToothTool.getDevices() as ArrayList<BluetoothDevice>
        val array = ArrayList<String>()
        deviceList.forEach { d -> array.add(d.name) }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, array)
        deviceSpinner.adapter = adapter
        connectDevice.setOnClickListener(this)

        homeTop.showMode = SwipeLayout.ShowMode.LayDown
        homeTop.setOnTouchListener { _, _ ->
            //如果上锁了就暂停处理消息
            lock.isActivated
        }
        addButton.setOnClickListener(this)
        lock.setOnClickListener(this)
        modifyConfig.setOnClickListener(this)
    }

    private fun createButton() {
        val key = keyValue.selectedItem.toString()
        if (key == "") return
        var xValue = 100f
        var yValue = 100f
        try {
            xValue = x.text.toString().toFloat()
            yValue = y.text.toString().toFloat()
        } catch (e: Exception) {
        }
        val gameButton = GameButton(home.context, home, vibrator, key, xValue, yValue)
        gameButtonList.add(gameButton)
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v) {
            //添加按钮
            addButton -> {
                createButton()
            }
            //锁定界面
            lock -> {
                lock.isActivated = !lock.isActivated
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
            connectDevice->{
                val index = deviceSpinner.selectedItemPosition
                BlueToothTool.preConnect(index)
                BlueToothTool.connect()
            }
        }
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime <= 1000) {//1s
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

}

