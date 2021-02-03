@file:Suppress("UNUSED_PARAMETER")

package com.rakeshgurudu.android.runtimelogger.core

import android.content.Context

/**
 * No-op class to be used in release build
 */
object RuntimeLogger {

    class Builder {
        fun filePrefix(filePrefix: String): Builder {
            return this
        }

        fun logOnAppStartup(enable: Boolean): Builder {
            return this
        }

        fun build(context: Context) {
        }
    }

    @Suppress("unused")
    fun startLogging(context: Context) {
    }

    @Suppress("unused")
    fun endLogging(context: Context) {
    }
}