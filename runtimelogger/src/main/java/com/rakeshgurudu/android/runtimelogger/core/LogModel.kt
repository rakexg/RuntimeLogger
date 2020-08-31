package com.rakeshgurudu.android.runtimelogger.core

data class LogModel(
    var id: Int,
    var fileName: String = "",
    var filePath: String = "",
    var fileSize: Double,
    var lastModified: Long
)