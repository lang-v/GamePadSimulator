package com.game.gamepad.queue

import java.util.concurrent.locks.ReentrantLock

/**
 * job -> string
 * 直接将短期内的消息合并为一条直接出队
 * 大大提升了消息的传输速度
 * 原因是为了保证消息的绝对有序，很多方法都上了同步锁
 * 每次只能一个消息出队，当多个键同时按下和松开时卡顿特别严重
 */

class MsgQueue(private val listener: QueueChangeListener?=null) {
    companion object{
        //入队时
        val ENQUEUE = 0
        //出队时
        val DEQUEUE = 1
        //当出队的任务执行完毕时,抛出异常则不会返回
        val TASKFINISED = 2
        //当执行的任务出错时，只有当抛出MsgQueueTaskErrorException才会执行
        val TASKERROR = 3
    }
    private val TAG = "MsgQueue"
    private var firstMsg = ""
    private val lock = ReentrantLock()

    fun enQueue(msg:String){
        while (lock.isLocked){}//阻塞
        synchronized(this) {
            firstMsg += (msg + "_")
        }
        listener?.changed(ENQUEUE)
    }

    fun deQueue():String {
//        Log.e(TAG,"full frontindex=$frontIndex , reaarindex=$rearIndex")
        listener?.changed(DEQUEUE)
        val msg = firstMsg
        lock.lock()
        firstMsg = ""
        lock.unlock()
        return msg
    }

    fun empty():Boolean{
//        Log.e(TAG,"empty frontindex=$frontIndex , reaarindex=$rearIndex")
        return firstMsg == ""
    }

    fun full():Boolean{
//        Log.e(TAG,"full frontindex=$frontIndex , reaarindex=$rearIndex")
        return false
    }

    fun clearAll(){
        firstMsg = ""
    }

    interface QueueChangeListener{
        fun changed(state:Int)
    }

    class MsgQueueFullException:Exception("MsgQueueFullException : MsgQueue is full")
    class MsgQueueEmptyException:Exception("MsgQueueEmptyException : MsgQueue is empty")
    class MsgQueueTaskErrorException:Exception("MsgQueueTaskErrorException : when task running ,has some error")
}