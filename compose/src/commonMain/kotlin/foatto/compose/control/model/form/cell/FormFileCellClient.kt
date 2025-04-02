package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormFileData

class FormFileCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormFileCell,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
) {
    val files: SnapshotStateList<FormFileData> = mutableStateListOf()

    val addFiles = mutableMapOf<Int, String>()
    val fileRemovedIds = mutableListOf<Int>()
}
