@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.darkrockstudios.libraries.mpfilepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual data class PlatformFile(
    val file: File,
)

@Composable
actual fun FilePicker(
    show: Boolean,
    initialDirectory: String?,
    fileExtensions: List<String>,
    title: String?,
    onFileSelected: FileSelected,
) {
    LaunchedEffect(show) {
        if (show) {
            val fileFilter = if (fileExtensions.isNotEmpty()) {
                fileExtensions.joinToString(",")
            } else {
                ""
            }
            val initialDir = initialDirectory ?: System.getProperty("user.dir")

            val fileChooser = JFileChooser()
            fileChooser.fileFilter = FileNameExtensionFilter("", fileFilter)
            fileChooser.currentDirectory = File(initialDir)
            fileChooser.isMultiSelectionEnabled = false
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

            val file = if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile
            } else {
                null
            }
            if (file != null) {
                val platformFile = PlatformFile(file)
                onFileSelected(platformFile)
            } else {
                onFileSelected(null)
            }
        }
    }
}

@Composable
actual fun MultipleFilePicker(
    show: Boolean,
    initialDirectory: String?,
    fileExtensions: List<String>,
    title: String?,
    onFileSelected: FilesSelected
) {
    LaunchedEffect(show) {
        if (show) {
            val fileFilter = if (fileExtensions.isNotEmpty()) {
                fileExtensions.joinToString(",")
            } else {
                ""
            }
            val initialDir = initialDirectory ?: System.getProperty("user.dir")

            val fileChooser = JFileChooser()
            fileChooser.fileFilter = FileNameExtensionFilter("", fileFilter)
            fileChooser.currentDirectory = File(initialDir)
            fileChooser.isMultiSelectionEnabled = true
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

            val files = if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFiles
            } else {
                null
            }
            if (files != null) {
                onFileSelected(files.map { PlatformFile(it) })
            } else {
                onFileSelected(null)
            }
        }
    }
}

@Composable
actual fun DirectoryPicker(
    show: Boolean,
    initialDirectory: String?,
    title: String?,
    onFileSelected: (String?) -> Unit,
) {
    LaunchedEffect(show) {
        if (show) {
            val initialDir = initialDirectory ?: System.getProperty("user.dir")

            val fileChooser = JFileChooser()
            fileChooser.currentDirectory = File(initialDir)
            fileChooser.isMultiSelectionEnabled = false
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

            val file = if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile
            } else {
                null
            }

            if (file != null) {
                onFileSelected(file.absolutePath)
            } else {
                onFileSelected(null)
            }
        }
    }
}