package com.rakeshgurudu.android.runtimelogger.core

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rakeshgurudu.android.runtimelogger.R
import com.rakeshgurudu.android.runtimelogger.ui.RuntimeLoggerActivity
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

const val NOTIFICATION_ID = 3108
const val CHANNEL_ID = "runtime_logger"
const val CHANNEL_NAME = "Runtime Logger"
const val NOTIFICATION_ACTION_START = "start_log"
const val NOTIFICATION_ACTION_STOP = "stop_log"
const val NOTIFICATION_ACTION_SAVE = "save_log"

object RuntimeLogger {
    private var broadCastSaveLog: Boolean = false
    private var fileName: String = ""
    private const val BUFFER = 0x1000 // 4K
    private const val MAX_LINES = 200
    private var stopLogging: Boolean = false
    private var startTime = 0L
    private val separator = System.getProperty("line.separator")!!
    private val pid = android.os.Process.myPid()
    private val tid = android.os.Process.myTid()
    private val TAG = RuntimeLogger::class.java.simpleName
    var logDirectoryPath: String = ""

    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())

    @SuppressLint("ConstantLocale")
    private val logDateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * Regex to match the logs received. Format of the logs is as below:
     * MM-dd HH:mm:ss.SSS ProcessId ThreadId LogLevel TAG: log message
     * eg. 09-04 18:05:15.563 4868 4868 I MultiDex: VM with version 2.1.0 has multidex support
     */
    private val logRegexPattern =
        Regex("(\\d+)\\s+(\\d+-\\d+\\s+\\d+:\\d+:\\d+.\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(.)\\s+(.+?):\\s+(.+)")

    /**
     * Call this method to start saving logs. The logs will be stored in app specific directory
     * of external storage. It is recommended to increase the device log buffer size from
     * Settings -> Developer Options -> Logger Buffer sizes, select 4M or 16M.
     */
    fun startLogging(context: Context) {
        startTime = System.currentTimeMillis()
        fileName = dateFormat.format(startTime)
        logDirectoryPath = context.getExternalFilesDir(null)?.absolutePath + "/runtimelogger"
        stopLogging = false
        val thread = Thread(Runnable {
            logBuffer(false, 0, StringBuilder())
        })
        thread.name = "Runtime Logger"
        thread.start()
        createNotification(context)
    }

    /**
     * Reads log messages by executing "logcat" command in a process and saves in a file. The logs are
     * saved in file when MAX_LINES are reached. When the log buffer is full the process ends with errorStream "read: unexpected EOF!".
     * So to continue logging, the process is again started with the timestamp of the last received log.
     * The log buffer size of the device can be found by command "adb logcat -g" and it can be increased from
     * Settings -> Developer Options -> Logger Buffer sizes.
     */
    private fun logBuffer(
        lastStopReading: Boolean,
        lastLineCount: Int,
        logBuilder: StringBuilder,
        logTimed: String = ""
    ) {
        try {
            val lineBuilder = StringBuilder()
            var stopReading = lastStopReading
            var lineCount = lastLineCount
            val process =
                if (logTimed.isNotEmpty()) {
                    //arrayOf("logcat", "-T", "09-03 12:35:10.982")
                    Runtime.getRuntime().exec(arrayOf("logcat", "-T", logTimed))
                } else {
                    Runtime.getRuntime().exec(arrayOf("logcat", "--pid=$pid"))//.waitFor()
                }
            val bufferedReader = process.inputStream.bufferedReader()
            var endOfBuffer = false
            while (bufferedReader.readLine().also {
                    if (it != null) {
                        lineBuilder.delete(0, lineBuilder.length)
                        lineBuilder.append(it)
                    } else {
                        endOfBuffer = true
                    }
                } != null && !stopReading) {
                /*if (log.contains(line)) {
                    continue
                }*/
                lineCount = ++lineCount
                logBuilder.append("$lineCount ")
                logBuilder.append(lineBuilder)
                logBuilder.append(separator)
                if (stopLogging) {
                    appendLibraryLogs(
                        ++lineCount,
                        logBuilder,
                        "stopped logging for session $fileName and exiting"
                    )
                    stopReading = true
                }
                if (lineCount % MAX_LINES == 0 || broadCastSaveLog) {
                    broadCastSaveLog = false
                    saveLog(++lineCount, logBuilder)
                }
            }
            destroyProcess(process)
            if (endOfBuffer && !stopReading) {
                //The lineBuilder received does not contain the line number so adding a dummy line number 123
                val lastLineTime =
                    logRegexPattern.find("123 $lineBuilder")?.destructured?.component2()
                if (lastLineTime != null) {
                    appendLibraryLogs(
                        ++lineCount,
                        logBuilder,
                        "starting new process with time $lastLineTime"
                    )
                    if (logBuilder.isNotEmpty()) {
                        saveLog(++lineCount, logBuilder)
                    }
                    logBuffer(stopReading, lineCount, logBuilder, lastLineTime)
                } else {
                    val currentTime = System.currentTimeMillis()
                    appendLibraryLogs(
                        ++lineCount,
                        logBuilder,
                        "starting new process with current time $currentTime"
                    )
                    logBuffer(stopReading, lineCount, logBuilder, logDateFormat.format(currentTime))
                }
            } else {
                if (logBuilder.isNotEmpty()) {
                    saveLog(++lineCount, logBuilder)
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "logData Exception: ", ex)
        }
    }

    /**
     * Call this method to end saving logs
     */
    fun endLogging(context: Context) {
        stopSession()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun stopSession() {
        if (startTime != 0L) {
            stopLogging = true
            startTime = 0
        }
    }

    /**
     * Appends this library logs to the log StringBuilder. These logs will be visible in the saved
     * log file
     */
    private fun appendLibraryLogs(lineCount: Int, log: StringBuilder, line: String?) {
        log.append("$lineCount")
        log.append(" ${logDateFormat.format(System.currentTimeMillis())} $pid $tid I $TAG:")
        log.append(line)
        log.append(separator)
    }

    /**
     * Saves logs in a file with filename as the time when {@link #startLogging(Context)} was called.
     */
    private fun saveLog(lineCount: Int, log: StringBuilder): Boolean {
        val logDir = File(logDirectoryPath)
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
        appendLibraryLogs(lineCount, log, "saving logging session for $fileName")
        var out: PrintStream? = null
        try {
            out = PrintStream(
                BufferedOutputStream(
                    FileOutputStream(newFile, true),
                    BUFFER
                )
            )
            out.print(log)
        } catch (ex: Exception) {
            return false
        } finally {
            out?.close()
            log.delete(0, log.length) //clear
        }
        return true
    }

    private fun destroyProcess(process: Process) {
        process.destroy()
    }


    private fun createNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val notifyManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifyManager.createNotificationChannel(mChannel)
        }
        val intent = Intent(context, RuntimeLoggerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val clickIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_log_format)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(clickIntent)

        if (stopLogging) {
            mBuilder.setContentText(
                String.format(
                    context.getString(R.string.last_session),
                    fileName
                )
            )
            val startIntent = Intent(context, LoggingBroadcastReceiver::class.java).apply {
                action = NOTIFICATION_ACTION_START
            }
            mBuilder.addAction(
                0,
                context.getString(R.string.start_session),
                PendingIntent.getBroadcast(context, 0, startIntent, 0)
            )
        } else {
            mBuilder.setContentText(
                String.format(
                    context.getString(R.string.current_session),
                    fileName
                )
            )
            val stopIntent = Intent(context, LoggingBroadcastReceiver::class.java).apply {
                action = NOTIFICATION_ACTION_STOP
            }
            mBuilder.addAction(
                0,
                context.getString(R.string.stop_session),
                PendingIntent.getBroadcast(context, 0, stopIntent, 0)
            )
            val saveIntent = Intent(context, LoggingBroadcastReceiver::class.java).apply {
                action = NOTIFICATION_ACTION_SAVE
            }
            mBuilder.addAction(
                0,
                context.getString(R.string.save_session),
                PendingIntent.getBroadcast(context, 0, saveIntent, 0)
            )
        }

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, mBuilder.build())
        }
    }

    /**
     * Receives broadcast messages from notification buttons. The logging can be started, saved and
     * stopped from the ongoing notification.
     */
    class LoggingBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e(TAG, "onReceive: ${intent?.action}")
            when (intent?.action) {
                NOTIFICATION_ACTION_START -> {
                    startLogging(context!!)
                }
                NOTIFICATION_ACTION_STOP -> {
                    stopSession()
                    createNotification(context!!)
                }
                NOTIFICATION_ACTION_SAVE -> {
                    broadCastSaveLog = true
                }
            }
        }
    }
}