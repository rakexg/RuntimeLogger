@file:Suppress("UNUSED_PARAMETER")

package com.rakeshgurudu.android.runtimelogger.core

import android.content.Context

/**
 * No-op class to be used in release build
 */
object RuntimeLogger {

    @Suppress("unused")
    fun startLogging(context: Context) {
    }

    @Suppress("unused")
    fun endLogging(context: Context) {

    }
}