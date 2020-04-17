package com.game.gamepad.permission
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat

@TargetApi(Build.VERSION_CODES.M)
class EasyRequest {
    private val TAG = "EasyRequest"
    /**
     * 单次申请一次权限
     */
    fun request(activity: Activity, permission: String, requestCode: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            AlertDialog.Builder(activity)
                .setTitle("请授权")
                .setMessage("没有此权限，程序将无法正常运行")
                .setNegativeButton("取消") { _, _ ->
                    //退出
                    Log.e(TAG,"user refused this permission $permission")
                    activity.finish()
                }
                .setPositiveButton("确定") { _, _ ->
                    requestPermissions(
                        activity,
                        Array(1) { permission },
                        requestCode
                    )
                }
                .show()
        } else {
            //第一次授权
            Log.i(TAG,"first request ：$permission")
            requestPermissions(activity, Array(1) { permission }, requestCode)
        }
    }

    /**
     * @return 未获取权限返回true
     */
    fun checkPermission(activity:Activity,permissions: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                activity,
                permissions
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }
}