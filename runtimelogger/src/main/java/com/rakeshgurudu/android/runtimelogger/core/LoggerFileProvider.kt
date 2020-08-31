package com.rakeshgurudu.android.runtimelogger.core

import androidx.core.content.FileProvider

/**
 * Empty file provider to avoid build error "Manifest merger failed : Attribute provider#androidx.core.content.FileProvider@authorities"
 * https://stackoverflow.com/a/42989182/3090861
 */
class LoggerFileProvider : FileProvider()