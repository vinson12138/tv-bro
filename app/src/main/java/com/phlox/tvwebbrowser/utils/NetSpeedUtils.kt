package com.phlox.tvwebbrowser.utils

import android.net.TrafficStats
import com.phlox.tvwebbrowser.TVBro
import java.util.Timer
import java.util.TimerTask

object NetSpeedUtils {

    var netSpeedCallback: NetSpeedCallback? = null

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    private var lastTotalReceiveBytes: Long = 0
    private var lastTotalTransferBytes: Long = 0

    /**
     * 根据应用uid获取设备启动以来，该应用接收到的总字节数
     *
     * @param uid 应用的uid
     */
    fun getTotalReceiveBytes(): Long {
        var receiveBytes: Long = TrafficStats.UNSUPPORTED.toLong()
        TVBro.instance.applicationContext?.run {
            receiveBytes = TrafficStats.getUidRxBytes(applicationInfo.uid)
        }
        // 当获取不到时，会返回TrafficStats.UNSUPPORTED
        return if (receiveBytes == TrafficStats.UNSUPPORTED.toLong()) 0 else receiveBytes / 1024
    }

    /**
     * 根据应用uid获取设备启动以来，该应用传输的总字节数
     *
     * @param uid 应用的uid
     */
    fun getTotalTransferBytes(): Long {
        var transferBytes: Long = TrafficStats.UNSUPPORTED.toLong()
        TVBro.instance.applicationContext?.run {
            transferBytes = TrafficStats.getUidTxBytes(applicationInfo.uid)
        }
        // 当获取不到时，会返回TrafficStats.UNSUPPORTED
        return if (transferBytes == TrafficStats.UNSUPPORTED.toLong()) 0 else transferBytes / 1024
    }

    // 通过Timer每隔1秒计算网速
    private fun calculateNetSpeed() {
        TVBro.instance.applicationContext?.run {
            val nowTotalReceiveBytes = getTotalReceiveBytes()
            val nowTotalTransferBytes = getTotalTransferBytes()

            val downloadSpeed = nowTotalReceiveBytes - lastTotalReceiveBytes
            val uploadSpeed = nowTotalTransferBytes - lastTotalTransferBytes

            lastTotalReceiveBytes = nowTotalReceiveBytes
            lastTotalTransferBytes = nowTotalTransferBytes

            val down = if(downloadSpeed > 1024) {
                "${downloadSpeed/1024}Mb/s"
            } else {
                "${downloadSpeed}Kb/s"
            }
            val upload = if(uploadSpeed > 1024) {
                "${uploadSpeed/1024}Mb/s"
            } else {
                "${uploadSpeed}Kb/s"
            }
            netSpeedCallback?.onNetSpeedChange("$down", "$upload")
        }
    }

    fun startMeasuringNetSpeed() {
        if (timer == null && timerTask == null) {
            timer = Timer()
            timerTask = object : TimerTask() {
                override fun run() {
                    calculateNetSpeed()
                }
            }
            timer?.run { timerTask?.let { schedule(it, 0L, 1000L) } }
        }
    }

    fun stopMeasuringNetSpeed() {
        timerTask?.cancel()
        timerTask = null
        timer?.cancel()
        timer = null
    }

    interface NetSpeedCallback {
        fun onNetSpeedChange(downloadSpeed: String, uploadSpeed: String)
    }
}