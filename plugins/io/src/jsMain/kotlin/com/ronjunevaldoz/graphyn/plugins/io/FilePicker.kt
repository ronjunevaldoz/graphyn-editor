package com.ronjunevaldoz.graphyn.plugins.io

actual object FilePicker {
    actual fun pickFile(onResult: (String?) -> Unit) = Unit
    actual fun pickFolder(onResult: (String?) -> Unit) = Unit
}
