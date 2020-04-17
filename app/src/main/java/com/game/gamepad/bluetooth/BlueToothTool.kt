package com.game.gamepad.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


object BlueToothTool {
    private val TAG = "BlueTooth"
    private var bluetoothAddress = "" //要连接的地址
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    //连接的设备位置
    private var connectDeviceIndex: Int = 0
    //返回连接信息，是否与pc对接成功
    private lateinit var connectListen: ConnectListen
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectThread: ConnectThread? = null
    //和pc端一致
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun setListener(listen: ConnectListen) {
        connectListen = listen
    }

    fun enableBT() {
        if (!bluetoothAdapter.isEnabled)
            bluetoothAdapter.enable()
        if (!bluetoothAdapter.isDiscovering)
            bluetoothAdapter.startDiscovery()
    }

    fun preConnect(index: Int) {
        connectDeviceIndex = index
        bluetoothSocket =
            bluetoothAdapter.bondedDevices.toList()[index].createRfcommSocketToServiceRecord(uuid)
    }

    fun connect() {
        if (connectThread != null) return
        Log.e("SL","run")
        connectThread = ConnectThread(getDevices()[connectDeviceIndex], bluetoothSocket)
        connectThread!!.start()
    }

    //对接
    fun docking() {
        try {
            if (isConnected()) {
                sendMsg("GamePadAndroid")
                //receiveMsg()//和pc对接
            }
        } catch (e: Exception) {
            e.printStackTrace()
            connectListen.connected(false)
        }
    }

    fun isConnected(): Boolean {
        if (bluetoothSocket == null) return false
        return true
        //return bluetoothSocket!!.isConnected
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
                    connectListen.connected(msg == receiveCommand)
                } catch (e: Exception) {
                    connectListen.connected(false)
                }

            }
        }.run()
    }

    /**
     * msg charset is UTF_8
     */
    fun sendMsg(msg: String) {
        Thread {
            if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
                connectListen.toast("未连接")
                return@Thread
            }
            try {
                if (outputStream != null)
                    outputStream = bluetoothSocket!!.outputStream
                outputStream!!.write(msg.toByteArray(Charsets.UTF_8))
                outputStream!!.flush()
                connectListen.toast("消息发送成功")
            } catch (e: Exception) {
                e.printStackTrace()
                connectListen.toast("消息发送失败")
                //connectListen.connected(false)
            }
        }.run()
    }

    fun disConnect() {
        if (bluetoothSocket != null) {
            if (outputStream != null)
                outputStream!!.close()
            if (inputStream != null)
                inputStream!!.close()
            bluetoothSocket!!.close()
            if (connectThread != null)
                connectThread!!.cancel()
        }
    }

    interface ConnectListen {
        fun connected(connected: Boolean)
        fun toast(msg:String)
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
                connectListen.connected(true)
                //docking()
            } catch (e: IOException) {
                try {
                    Log.i(TAG, "Trying fallback...")
                    mmSocket = mmDevice.javaClass.getMethod(
                        "createRfcommSocket", *arrayOf<Class<*>?>(
                            Int::class.javaPrimitiveType
                        )
                    ).invoke(mmDevice, 1) as BluetoothSocket
                    mmSocket!!.connect()
                    connectListen.connected(true)
                    //docking()
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
                    connectListen.connected(false)
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