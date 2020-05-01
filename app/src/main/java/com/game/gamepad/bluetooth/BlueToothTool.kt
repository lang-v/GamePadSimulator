package com.game.gamepad.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.game.gamepad.queue.MsgQueue
import com.game.gamepad.utils.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ThreadPoolExecutor


object BlueToothTool {
    private const val TAG = "BlueTooth"
    private var sendMsgThreadRunning = false//记录发送线程的运行状态
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    //连接的设备位置
    private var connectDeviceIndex: Int = 0
    //返回连接信息，是否与pc对接成功
    private lateinit var blueToothtListener: BluetoothListener
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectThread: ConnectThread? = null
    //和pc端一致
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//    private val msgQueueListener: MsgQueue.QueueChangeListener =object :
//        MsgQueue.QueueChangeListener{
//        override fun changed(state: Int) {
//            when(state){
//                //入队
//                MsgQueue.ENQUEUE->{
////                    if (sendMsgThreadRunning)return
////                    sendMsgThreadRunning = true
////                    Thread {
////                        //msgQueue.deQueue().invoke()
////                    }.run()
//                }
//                //出队
//                MsgQueue.DEQUEUE-> {
//                }
//                //出队任务完成
//                MsgQueue.TASKFINISED->{
//                    //不为空继续执行
//                    if (!msgQueue.empty()) {
//                        Thread {
//                            //msgQueue.deQueue().invoke()
//                        }.run()
//                    }
//                    else{
//                        sendMsgThreadRunning = false //重新设置标记位等待入队
//                    }
//                }
//                //任务异常中止
//                MsgQueue.TASKERROR->{
//                    //所有job出队
//                    msgQueue.clearAll()
//                }
//            }
//        }
//    }
    private val msgQueue= MsgQueue()

    //用于发送消息的线程
    private var msgThread :MsgThread? = null


    fun setListener(listener: BluetoothListener) {
        blueToothtListener = listener
    }

    //开启蓝牙
    private fun enableBT() {
        if (!bluetoothAdapter.isEnabled)
            bluetoothAdapter.enable()
    }

    //因为是手机连接电脑所以手机不需要开启蓝牙可见,省电
    fun search() {
        bluetoothAdapter.startDiscovery()
    }

    fun init() {
        enableBT()
        if (isEnable()) {
            search()
            //list.addAll(getDevices())
        }
    }

    fun isEnable(): Boolean {
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

//        bluetoothAdapter.cancelDiscovery()
//        bluetoothSocket = getDevices()[connectDeviceIndex].createRfcommSocketToServiceRecord(uuid)
//        return
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
            Log.e("SL", "对接失败")
            blueToothtListener.connected(false)
        }
    }

    fun isConnected(): Boolean {
        if (bluetoothSocket == null) return false
        return bluetoothSocket!!.isConnected
    }

    //获取已配对的设备
    fun getDevices(): List<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices.toList()
    }

    /**
     * 这个函数的作用是为了避免连接通道内长时间没有消息引起卡顿
     */
    private var lastTime = 0L
    fun positive() {
        GlobalScope.launch {
            while (isConnected()) {
                Log.d("positive","on")
                if (System.currentTimeMillis() - lastTime > 500) {
                    Log.d("positive","send_")
                    sendMsg("_")
                }
                delay(1000)
            }
        }
    }
    /**
     * Charset is UTF_8
     */
    private fun receiveMsg() {
        GlobalScope.launch(Dispatchers.Main) {
            val receiveCommand = "GamePadPC"
            if (bluetoothSocket!!.isConnected) {
                try {
                    if (inputStream == null)
                        inputStream = bluetoothSocket!!.inputStream
                    val byteArray = ByteArray(200)
                    inputStream!!.read(byteArray, 0, byteArray.size)
                    val msg = byteArray.toString(Charsets.UTF_8).substring(0, 9)
                    Log.e("SL", "receiveMsg 对接 $msg ${msg == receiveCommand}")
                    blueToothtListener.connected(msg == receiveCommand)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("SL", "receiveMsg 对接失败")
                    blueToothtListener.connected(false)
                }
            }
        }
    }

    fun sendMsg(msg: String) {
        //入队
        msgQueue.enQueue(msg)
        if (msgThread == null) msgThread = MsgThread()
        if (!msgThread!!.getRunning()) {
            msgThread!!.start()
        }
    }

    /**
     * msg charset is UTF_8
     */
    //@Synchronized
    fun privateSendMsg(msg: String) {
//        if(msg != " _")Log.e("BTMSG",msg)
        lastTime = System.currentTimeMillis()
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            ToastUtil.show("未连接")
            return
        }
        try {
            if (outputStream==null)
                outputStream = bluetoothSocket!!.outputStream
            outputStream!!.write((msg+"_").toByteArray(Charsets.UTF_8))
            //outputStream!!.flush()
            //blueToothtListen.toast("消息发送成功")
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.show("消息发送失败")
            if (outputStream != null)
                outputStream!!.close()
            outputStream = null
            disConnect()
            throw MsgQueue.MsgQueueTaskErrorException()
            //connectListen.connected(false)
        }
    }

    fun disConnect() {
        msgThread?.setRunning(false)
        if (bluetoothSocket != null) {
            if (outputStream != null) {
                outputStream!!.close()
                outputStream = null
            }
            if (inputStream != null) {
                inputStream!!.close()
                inputStream = null
            }
            bluetoothSocket!!.close()
            bluetoothSocket = null
            if (connectThread != null) {
                connectThread!!.cancel()
                connectThread = null
            }
        }
        blueToothtListener.connected(false)
    }

    interface BluetoothListener {
        fun connected(connected: Boolean)
    }

    private class MsgThread :Thread(){
        private var running = false
        fun setRunning(state:Boolean)= run { running = state }
        fun getRunning():Boolean{
            return running
        }
        override fun run() {
            running = true
            while (isConnected() && running) {
                if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                    //ToastUtil.show("未连接")
                    return
                }
                try {
                    if (outputStream == null)
                        outputStream = bluetoothSocket!!.outputStream
                    while (running){
                        if (msgQueue.empty())continue
                        val msg = msgQueue.deQueue()
                        lastTime = System.currentTimeMillis()
                        Log.d("BTMSG",msg)
                        outputStream!!.write(msg.toByteArray(Charsets.UTF_8))
                        //sleep(20)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ToastUtil.show("消息发送失败")
                    if (outputStream != null)
                        outputStream!!.close()
                    outputStream = null
                    disConnect()
                    //connectListen.connected(false)
                }
            }
        }
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
                        "createRfcommSocket", Int::class.javaPrimitiveType
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
                    Log.e("SL", "连接失败")
                    blueToothtListener.connected(false)
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