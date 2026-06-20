package com.ronjunevaldoz.graphyn.plugins.io

/** Platform-specific file and folder picker. No-op on platforms that don't support a native dialog. */
expect object FilePicker {
    fun pickFile(onResult: (String?) -> Unit)
    fun pickFolder(onResult: (String?) -> Unit)
}
