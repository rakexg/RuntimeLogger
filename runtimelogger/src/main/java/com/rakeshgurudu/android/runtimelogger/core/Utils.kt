package com.rakeshgurudu.android.runtimelogger.core

import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog

class Utils {
    companion object {

        /**
         * Dialog with edit text from paramater included to enter data. The dialog dismiss is to be handled manually, this
         * allows to handle cases in which dialog is not to be dismissed based on some validation.
         *
         * @param context          context to use
         * @param input            the EditText to use, inputType needs to be set and sent
         * @param title            title to use for dialog
         * @param message          message to use for dialog
         * @param positiveListener listener for positive button click
         * @param negativeListener listener for negative button click
         * @return created dialog
         */
        fun createSingleInputDialog(
            context: Context?,
            input: EditText?,
            title: String?,
            message: String?,
            positiveListener: DialogInterface.OnClickListener?,
            negativeListener: DialogInterface.OnClickListener?
        ): AlertDialog? {
            if (context == null || input == null) {
                return null
            } //dialog click listener, edit text focus
            val builder = AlertDialog.Builder(context)
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title)
            }
            if (positiveListener != null) {
                builder.setPositiveButton("Ok", null)
            }
            if (!TextUtils.isEmpty(message)) {
                builder.setMessage(message)
            }
            if (negativeListener != null) {
                builder.setNegativeButton("Cancel", null)
            }
            val container = FrameLayout(context)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = 60
            params.rightMargin = 60
            input.layoutParams = params
            container.addView(input)
            builder.setView(container)
            val alertDialog = builder.create()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            alertDialog.setOnShowListener { dialog: DialogInterface? ->
                if (positiveListener != null) {
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener {
                            positiveListener.onClick(
                                dialog,
                                DialogInterface.BUTTON_POSITIVE
                            )
                        }
                }
                if (negativeListener != null) {
                    alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setOnClickListener {
                            negativeListener.onClick(
                                dialog,
                                DialogInterface.BUTTON_NEGATIVE
                            )
                        }
                }
                input.requestFocus()
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
            alertDialog.setOnDismissListener {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
            return alertDialog
        }
    }
}