package com.game.gamepad.bluetooth;

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.inuker.bluetooth.library.utils.BluetoothUtils.registerReceiver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class BluetoothReceiver : BroadcastReceiver() {
    private lateinit var adapter: BluetoothAdapter
    private lateinit var device: BluetoothDevice

    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    /**
     * 发送数据给服务端
     */
    fun sendData(msg: String) {
        try {
            outputStream.write(msg.toByteArray(charset("UTF-8")))
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_FOUND) { //获取蓝牙设备
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                adapter!!.cancelDiscovery()
                Log.i("bluetoothInfo", "扫描到的蓝牙设备名称" + device.name)
                try { //这里的UUID必须跟服务端的UUID一样
                    socket =
                        device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    socket.connect()
                    Log.i("bluetoothInfo", "连接服务器成功")
                    outputStream = socket.getOutputStream()
                    inputStream = socket.getInputStream()
                    Thread(Runnable {
                        try {
                            val b = ByteArray(1024)
                            var n: Int
                            while (inputStream.read(b).also { n = it } != -1) {
                                val s = String(b, 0, n, Charsets.UTF_8)
                                Log.i("bluetoothInfo", "客户端收到服务器的数据了$s")
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }).start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } //状态改变时
        else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == intent.action) {
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (device.bondState) {
                BluetoothDevice.BOND_BONDING -> Log.i("bluetoothInfo", "正在配对......")
                BluetoothDevice.BOND_BONDED -> Log.i("bluetoothInfo", "完成配对")
                BluetoothDevice.BOND_NONE -> Log.i("bluetoothInfo", "取消配对")
                else -> {
                }
            }
        }
    }
}