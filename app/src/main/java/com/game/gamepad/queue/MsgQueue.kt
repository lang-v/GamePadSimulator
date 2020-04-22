package com.game.gamepad.queue

import android.util.Log

class MsgQueue(private val count: Int,private val listener: QueueChangeListener?=null) {
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
    private val workArray: Array<() -> Unit> = Array(count+1) { {}}
    private var frontIndex = 0
    private var rearIndex = 0

    fun enQueue(job:()->Unit){
        if (full())throw MsgQueueFullException()
        workArray[rearIndex] = job
        rearIndex = (rearIndex+1)%count
        listener?.changed(ENQUEUE)
    }

    fun deQueue():()->Unit{
        if (empty()) throw MsgQueueEmptyException()
//        Log.e(TAG,"full frontindex=$frontIndex , reaarindex=$rearIndex")
        val job = workArray[frontIndex]
        frontIndex = (frontIndex+1)%count
        listener?.changed(DEQUEUE)
        return {
            try {
                job.invoke()
            }catch (e: MsgQueueTaskErrorException) {
                listener?.changed(TASKERROR)
            }
            listener?.changed(TASKFINISED)
        }
    }

    fun empty():Boolean{
//        Log.e(TAG,"empty frontindex=$frontIndex , reaarindex=$rearIndex")
        return frontIndex == rearIndex
    }

    fun full():Boolean{
//        Log.e(TAG,"full frontindex=$frontIndex , reaarindex=$rearIndex")
        return frontIndex == (rearIndex+1)%count
    }

    fun clearAll(){
        frontIndex = 0
        rearIndex = 0
    }

    interface QueueChangeListener{
        fun changed(state:Int)
    }

    class MsgQueueFullException:Exception("MsgQueueFullException : MsgQueue is full")
    class MsgQueueEmptyException:Exception("MsgQueueEmptyException : MsgQueue is empty")
    class MsgQueueTaskErrorException:Exception("MsgQueueTaskErrorException : when task running ,has some error")
}