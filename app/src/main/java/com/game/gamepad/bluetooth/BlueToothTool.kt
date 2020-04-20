package com.game.gamepad.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.game.gamepad.utils.ToastUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


object BlueToothTool {
    private val TAG = "BlueTooth"
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    //连接的设备位置
    private var connectDeviceIndex: Int = 0
    //返回连接信息，是否与pc对接成功
    private lateinit var blueToothtListen: BluetoothListen
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectThread: ConnectThread? = null
    //和pc端一致
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    //线程等待队列
    private val workQueue = LinkedBlockingDeque<Runnable>(30)
    //线程池
    private val threadPool = ThreadPoolExecutor(
        8,
        16,
        5000,
        TimeUnit.MILLISECONDS,
        workQueue
    ) { _, _ ->
        ToastUtil.show("发生未知错误")
    }

    fun setListener(listen: BluetoothListen) {
        blueToothtListen = listen
    }

    //开启蓝牙
    private fun enableBT() {
        if (!bluetoothAdapter.isEnabled)
            bluetoothAdapter.enable()
    }

    //因为是手机连接电脑所以手机不需要开启蓝牙可见
    fun search(){
        bluetoothAdapter.startDiscovery()
    }

    fun init(){
        enableBT()
        if (isEnable()){
            search()
            //list.addAll(getDevices())
        }
    }

    fun isEnable():Boolean{
        return bluetoothAdapter.state == BluetoothAdapter.STATE_ON
    }

    fun preConnect(index: Int) {
        bluetoothAdapter.state
        connectDeviceIndex = index
        bluetoothSocket =
            bluetoothAdapter.bondedDevices.toList()[index].createRfcommSocketToServiceRecord(uuid)
    }

    fun connect() {
//        Log.e("SL","run")
        connectThread = ConnectThread(getDevices()[connectDeviceIndex], bluetoothSocket)
        connectThread!!.start()
    }

    //对接
    fun docking() {
        try {
            if (isConnected()) {
                sendMsg("GamePadAndroid")
                receiveMsg()//和pc对接
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SL","对接失败")
            blueToothtListen.connected(false)
        }
    }

    fun isConnected(): Boolean {
        if (bluetoothSocket == null) return false
        return bluetoothSocket!!.isConnected
    }

    fun getDevices(): List<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices.toList()
    }

    /**
     * 这个函数的作用是为了避免连接长时间没有消息引起卡顿
     */
    private var lastTime = 0L
    fun positive(){
        GlobalScope.launch{
            while (isConnected()) {
                if (System.currentTimeMillis()- lastTime > 500)
                    sendMsg("_")
                delay(500)
            }
        }
    }

    /**
     * Charset is UTF_8
     */
    private fun receiveMsg() {
        threadPool.execute {
            val receiveCommand = "GamePadPC"
            if (bluetoothSocket!!.isConnected) {
                try {
                    if (inputStream == null)
                        inputStream = bluetoothSocket!!.inputStream
                    val byteArray = ByteArray(200)
                    inputStream!!.read(byteArray, 0, byteArray.size)
                    val msg = byteArray.toString(Charsets.UTF_8).substring(0,9)
                    Log.e("SL","receiveMsg 对接 $msg ${msg == receiveCommand}")
                    blueToothtListen.connected(msg == receiveCommand)
                } catch (e: Exception) {
                    Log.e("SL","receiveMsg 对接失败")
                    blueToothtListen.connected(false)
                }
            }
        }
    }

    //多一层调用，使用线程池来管理
    fun sendMsg(msg:String){
        threadPool.execute { privateSendMsg(msg) }
    }

    /**
     * msg charset is UTF_8
     */
    @Synchronized
    private fun privateSendMsg(msg: String) {
        lastTime = System.currentTimeMillis()
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            ToastUtil.show("未连接")
            return
        }
        try {
            outputStream = bluetoothSocket!!.outputStream
            outputStream!!.write(msg.toByteArray(Charsets.UTF_8))
            outputStream!!.flush()
            //blueToothtListen.toast("消息发送成功")
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.show("消息发送失败")
            disConnect()
            //connectListen.connected(false)
        }
    }

    fun disConnect() {
        if (bluetoothSocket != null) {
            if (outputStream != null) {
                outputStream!!.close()
                outputStream = null
            }
            if (inputStream != null) {
                inputStream!!.close()
                inputStream=null
            }
            bluetoothSocket!!.close()
            bluetoothSocket = null
            if (connectThread != null) {
                connectThread!!.cancel()
                connectThread = null
            }
        }
        blueToothtListen.connected(false)
    }

    interface BluetoothListen {
        fun connected(connected: Boolean)
    }

    private class ConnectThread(
        private val mmDevice: BluetoothDevice,
        private var mmSocket: BluetoothSocket?
    ) : Thread() {
        private val mSocketType: String? = null
        override fun run() { // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery()
            // Make a connection to the BluetoothSocket
            try { // This is a blocking call and will only return on a
// successful connection or an exception
                mmSocket!!.connect()
                //blueToothtListen.connected(true)
                docking()
            } catch (e: IOException) {
                try {
                    Log.i(TAG, "Trying fallback...")
                    mmSocket = mmDevice.javaClass.getMethod(
                        "createRfcommSocket", *arrayOf<Class<*>?>(
                            Int::class.javaPrimitiveType
                        )
                    ).invoke(mmDevice, 1) as BluetoothSocket
                    mmSocket!!.connect()
                    //blueToothtListen.connected(true)
                    docking()
                    Log.i(TAG, "Connected")
                } catch (e2: Exception) {
                    Log.e(TAG, "Couldn't establish Bluetooth connection!")
                    try {
                        mmSocket!!.close()
                    } catch (e3: IOException) {
                        Log.e(
                            TAG,
                            "unable to close() $mSocketType socket during connection failure",
                            e3
                        )
                    }
                    Log.e("SL","连接失败")
                    blueToothtListen.connected(false)
                    return
                }
            }
            // Reset the ConnectThread because we're done
            synchronized(this) { connectThread = null }
            // Start the connected thread
            //preConnect(connectDeviceIndex)
            //connect()
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
            }
        }
    }
}