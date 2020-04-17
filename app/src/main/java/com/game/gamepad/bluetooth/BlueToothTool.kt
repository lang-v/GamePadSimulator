package com.game.gamepad.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


object BlueToothTool {
    private var bluetoothAddress = "" //要连接的地址
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    //连接的设备位置
    private var connectDeviceIndex: Int = 0
    //返回连接信息，是否与pc对接成功
    private lateinit var connectListen: ConnectListen
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    //和pc端一致
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun setListener(listen: ConnectListen){
        connectListen = listen
    }
    fun enableBT(){
        if (!bluetoothAdapter.isEnabled)
            bluetoothAdapter.enable()
        if (!bluetoothAdapter.isDiscovering)
            bluetoothAdapter.startDiscovery()
    }

    fun preConnect(index: Int) {
        connectDeviceIndex = index
        bluetoothSocket = bluetoothAdapter.bondedDevices.toList()[index].createRfcommSocketToServiceRecord(uuid)
    }

    fun connect() {
        try {
            if (bluetoothSocket != null)
                bluetoothSocket!!.connect()
            if (isConnected()) {
                //这里开启线程接受消息，避免错过
                receiveMsg()//和pc对接
                sendMsg("GamePadAndroid")
            }
        }catch (e:Exception){
            e.printStackTrace()
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
     * Charset is UTF_8
     */
    fun receiveMsg() {
        Thread {
            val receiveCommand = "GamePadPC"
            if (bluetoothSocket!!.isConnected) {
                try {
                    if (inputStream == null)
                        inputStream = bluetoothSocket!!.inputStream
                    val byteArray = ByteArray(1024)
                    inputStream!!.read(byteArray, 0, byteArray.size)
                    val msg = byteArray.toString(Charsets.UTF_8)
                    connectListen.connected(msg==receiveCommand)
                } catch (e: Exception) {
                }

            }
        }.run()
    }

    /**
     * msg charset is UTF_8
     */
    fun sendMsg(msg: String) {
        Thread {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) return@Thread
            try {
                if (outputStream != null)
                    outputStream = bluetoothSocket!!.outputStream
                outputStream!!.write(msg.toByteArray(Charsets.UTF_8))
                outputStream!!.flush()
            } catch (e: Exception) {
            }
        }.run()
    }

    fun disConnect() {
        if (bluetoothSocket != null) {
            bluetoothSocket!!.close()
            if (outputStream != null)
                outputStream!!.close()
        }
    }

    interface ConnectListen {
        fun connected(connected: Boolean)
    }
}