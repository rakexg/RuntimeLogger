package com.rakeshgurudu.android.runtimelogger.ui

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.rakeshgurudu.android.runtimelogger.R
import com.rakeshgurudu.android.runtimelogger.core.Utils

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
        val filePrefixWrapper = rootView.findViewById<RelativeLayout>(R.id.filePrefixWrapper)
        val filePrefix = rootView.findViewById<TextView>(R.id.filePrefix)
        val preferences = context?.getSharedPreferences(
                getString(R.string.runtime_logger_shared_prefs),
                Context.MODE_PRIVATE
        )!!
        val enabled =
                preferences.getBoolean(context?.getString(R.string.pref_key_log_on_startup), false)
        appStartupSwitch.isChecked = enabled
        if (enabled) {
            appStartupSubtitle.text = getString(R.string.app_startup_logging_enabled)
        } else {
            appStartupSubtitle.text = getString(R.string.app_startup_logging_disabled)
        }
        appStartupSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit()
                    ?.putBoolean(getString(R.string.pref_key_log_on_startup), isChecked)
                    ?.apply()
            if (isChecked) {
                appStartupSubtitle.text = getString(R.string.app_startup_logging_enabled)
            } else {
                appStartupSubtitle.text = getString(R.string.app_startup_logging_disabled)
            }
        }
        setPrefixText(filePrefix, preferences.getString(getString(R.string.filename_no_prefix), ""))

        filePrefixWrapper.setOnClickListener {
            val editText = EditText(context)
            editText.inputType = InputType.TYPE_CLASS_TEXT
            Utils.createSingleInputDialog(
                    context,
                    editText,
                    getString(R.string.settings_title_file_prefix),
                    "",
                    DialogInterface.OnClickListener { dialog, _ ->
                        var input = editText.text.toString()
                        if (input.isNotBlank() && input.length > 5) {
                            Toast.makeText(context, "Max 5 characters allowed", Toast.LENGTH_SHORT)
                                    .show()
                            return@OnClickListener
                        }
                        setPrefixText(filePrefix, input)
                        input += " "//Adding space character at the end
                        preferences.edit().putString(getString(R.string.pref_key_file_prefix), input)
                                .apply()
                        dialog.dismiss()
                    }, DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })?.show()
        }
        return rootView
    }

    private fun setPrefixText(
            filePrefix: TextView,
            text: String?
    ) {
        if (text.isNullOrBlank()) {
            filePrefix.text = getString(R.string.filename_no_prefix)
        } else {
            filePrefix.text = text
        }
    }
}