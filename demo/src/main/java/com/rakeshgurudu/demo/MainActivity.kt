package com.rakeshgurudu.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.rakeshgurudu.android.runtimelogger.core.RuntimeLogger

class MainActivity : AppCompatActivity() {
    private var stopLogging: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RuntimeLogger.Builder()
            .filePrefix("demo")
            .logOnAppStartup(false)
            .build(this@MainActivity)

        Thread(Runnable {
            var count = 0
            while (!stopLogging) {

                Log.e("MainActivity", "onCreate: ${++count}")
                Thread.sleep(500)
            }
        }).start()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        RuntimeLogger.endLogging(this@MainActivity)
        stopLogging = true
    }
}