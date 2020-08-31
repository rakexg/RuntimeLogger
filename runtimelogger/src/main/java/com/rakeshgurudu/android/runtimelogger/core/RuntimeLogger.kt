package com.rakeshgurudu.android.runtimelogger.core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object RuntimeLogger {
    private var fileName: String = ""
    private const val BUFFER = 0x1000 // 4K
    private const val MAX_LINES = 200
    private var stopLogging: Boolean = false
    private var startTime = 0L
    private lateinit var appCo: Context
    var logDirectoryPath: String = ""

    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())

    fun startLogging(context: Context) {
        Log.e("RuntimeLogger", "startLogging")
        appCo = context
        startTime = System.currentTimeMillis()
        fileName = dateFormat.format(startTime)
        logDirectoryPath = appCo.getExternalFilesDir(null)?.absolutePath + "/runtimelogger"
        stopLogging = false
        val pid = android.os.Process.myPid()
        Runtime.getRuntime().exec("logcat -P '$pid'")
        val thread = Thread(Runnable {
            logData()
        })
        thread.name = "Runtime Logger"
        thread.start()
    }

    private fun logData(): String {
        try {
            val logCatCommand = StringBuilder()
            val pid = android.os.Process.myPid()
            Log.e("RuntimeLogger", "logData pid: $pid")
            logCatCommand.append("logcat --pid=$pid")
            val process = Runtime.getRuntime().exec(logCatCommand.toString())//.waitFor()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            val log = StringBuilder()
            val separator = System.getProperty("line.separator")
            var line: String?
            var lineCount = 0
            var stopReading = false
            while (bufferedReader.readLine().also { line = it } != null && !stopReading) {
                lineCount = ++lineCount
                log.append("$lineCount ")
                log.append(line)
                log.append(separator)
                if (stopLogging) {
                    stopReading = true
                }
                if (lineCount % MAX_LINES == 0) {
                    saveLog(log)
                    log.delete(0, log.length) // clear
                }
            }
            if (log.isNotEmpty()) {
                saveLog(log)
                log.delete(0, log.length) // clear
            }
            return log.toString()
        } catch (ioe: IOException) {
            return "Error retrieving logcat info"
        }
    }

    fun endLogging() {
        //TODO: handle case where endLogging is called before start
        if (startTime != 0L) {
            Log.e("RuntimeLogger", "endLogging")
            stopLogging = true
            startTime = 0
        }
    }

    private fun saveLog(
        logString: CharSequence?
    ): Boolean {
        val logDir = File(logDirectoryPath)//File(appCo.getExternalFilesDir(null), "runtimelogger")

        if (!logDir.exists()) {
            logDir.mkdir()
        }

        val newFile = File(logDir, "$fileName.txt")
        try {
            if (!newFile.exists()) {
                newFile.createNewFile()
            }
        } catch (ex: IOException) {
            return false
        }
        var out: PrintStream? = null
        try {
            out = PrintStream(
                BufferedOutputStream(
                    FileOutputStream(newFile, true),
                    BUFFER
                )
            )

            if (logString != null) {
                out.print(logString)
            }
        } catch (ex: FileNotFoundException) {
            return false
        } finally {
            out?.close()
        }
        return true
    }
}