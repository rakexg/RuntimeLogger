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
    var logDirectoryPath: String = ""
    private val TAG = RuntimeLogger::class.java.simpleName

    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())

    fun startLogging(context: Context) {
        startTime = System.currentTimeMillis()
        fileName = dateFormat.format(startTime)
        logDirectoryPath = context.getExternalFilesDir(null)?.absolutePath + "/runtimelogger"
        stopLogging = false
        val pid = android.os.Process.myPid()
        Runtime.getRuntime().exec("logcat -P '$pid'")
        val thread = Thread(Runnable {
            logData()
        })
        thread.name = "Runtime Logger"
        thread.start()
        createNotification(context)
        Log.e(TAG, "started logging for session $fileName for process $pid")
    }

    private fun logData(): String {
        try {
            val logCatCommand = StringBuilder()
            val pid = android.os.Process.myPid()
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
                if (lineCount % MAX_LINES == 0 || broadCastSaveLog) {
                    broadCastSaveLog = false
                    saveLog(log)
                    log.delete(0, log.length) // clear
                }
            }
            if (log.isNotEmpty()) {
                saveLog(log)
            }
            return log.toString()
        } catch (ioe: IOException) {
            return "Error retrieving logcat info"
        }
    }

    fun endLogging(context: Context) {
        //TODO: handle case where endLogging is called before start
        stopSession()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        Log.e(TAG, "Exiting")
    }

    private fun stopSession() {
        if (startTime != 0L) {
            Log.e("RuntimeLogger", "endLogging")
            stopLogging = true
            startTime = 0
            val logCatCommand = StringBuilder()
            logCatCommand.append("logcat -c")
            Runtime.getRuntime().exec(logCatCommand.toString())
            Log.e(TAG, "stopped logging for session $fileName and exiting")
        }
    }

    private fun saveLog(
        logString: CharSequence?
    ): Boolean {
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
        Log.e(TAG, "saved logging session for $fileName")
        return true
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

    class LoggingBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("MyBroadcastReceiver", "onReceive: ")
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