package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.compose.control.model.form.FormFileDataClient
import foatto.core.model.response.form.cells.FormFileCell

class FormFileCellClient(
    cellName: String,
    minWidth: Int,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    focusId: Int,
    val data: FormFileCell,
) : FormBaseCellClient(
    cellName = cellName,
    minWidth = minWidth,
    align = align,
    isVisible = isVisible,
    focusId = focusId,
) {
    val files: SnapshotStateList<FormFileDataClient> = mutableStateListOf()

    val addFiles = mutableMapOf<Int, String>()
    val fileRemovedIds = mutableListOf<Int>()
}
