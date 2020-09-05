package com.rakeshgurudu.android.runtimelogger.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rakeshgurudu.android.runtimelogger.R

class FragmentSettings : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FragmentSettings()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
        val appStartupSwitch = rootView.findViewById<Switch>(R.id.appStartupSwitch)
        val appStartupSubtitle = rootView.findViewById<TextView>(R.id.appStartupSubtitle)
        val preferences = context?.getSharedPreferences(
            getString(R.string.runtime_logger_shared_prefs),
            Context.MODE_PRIVATE
        )!!
        val enabled = preferences.getBoolean(context?.getString(R.string.log_on_startup), false)
        appStartupSwitch.isChecked = enabled
        if (enabled) {
            appStartupSubtitle.text = getString(R.string.app_startup_logging_enabled)
        } else {
            appStartupSubtitle.text = getString(R.string.app_startup_logging_disabled)
        }
        appStartupSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit()
                ?.putBoolean(getString(R.string.log_on_startup), isChecked)
                ?.apply()
            if (isChecked) {
                appStartupSubtitle.text = getString(R.string.app_startup_logging_enabled)
            } else {
                appStartupSubtitle.text = getString(R.string.app_startup_logging_disabled)
            }
        }
        return rootView
    }
}