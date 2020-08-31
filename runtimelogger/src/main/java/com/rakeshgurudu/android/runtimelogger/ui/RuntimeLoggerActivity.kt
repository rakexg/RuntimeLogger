package com.rakeshgurudu.android.runtimelogger.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.rakeshgurudu.android.runtimelogger.R

class RuntimeLoggerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_logger)
        setSupportActionBar(findViewById(R.id.toolbar))
        var appName = applicationInfo.nonLocalizedLabel
        if (appName.isNullOrBlank()) {
            appName = getString(applicationInfo.labelRes)
        }
        title = "$appName Runtime Logger"
        launchFragment(FragmentLogList.newInstance(), R.id.fragment_container, false)
    }

    private fun launchFragment(fragment: Fragment, containerID: Int, addToBackStack: Boolean) {
        val manager = supportFragmentManager
        val ft = manager.beginTransaction()
        val fragmentTag = fragment::class.java.simpleName
        if (addToBackStack) {
            ft.add(containerID, fragment, fragmentTag)
            ft.addToBackStack(fragmentTag)
        } else {
            ft.replace(containerID, fragment, fragmentTag)
        }
        if (!isFinishing) {
            ft.commit()
        }
        //setCurrentFragment(fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.logger_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_all -> {
                val fragmentLogList =
                    supportFragmentManager.findFragmentByTag(FragmentLogList::class.java.simpleName) as FragmentLogList?
                fragmentLogList?.deleteAllFiles()
                return true
            }
        }
        return false
    }
}