package com.ronjunevaldoz.graphyn.plugins.io

import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual object FilePicker {
    actual fun pickFile(onResult: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.FILES_ONLY }
            val selected = chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
            onResult(if (selected) chooser.selectedFile.absolutePath else null)
        }
    }

    actual fun pickFolder(onResult: (String?) -> Unit) {
        SwingUtilities.invokeLater {
            val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
            val selected = chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
            onResult(if (selected) chooser.selectedFile.absolutePath else null)
        }
    }
}
