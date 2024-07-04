package foatto.compose.control.model.form.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.compose.control.model.form.FormFileDataClient
import foatto.core.model.response.system.form.cells.FormFileCell

class FormFileCellClient(
    id: Int,
    cellName: String,
    align: Arrangement.Horizontal,
    isVisible: MutableState<Boolean> = mutableStateOf(true),
    val data: FormFileCell,
) : FormBaseCellClient(
    id = id,
    cellName = cellName,
    align = align,
    isVisible = isVisible,
) {
    val files: SnapshotStateList<FormFileDataClient> = mutableStateListOf()

    val addFiles = mutableMapOf<Long, String>()
    val fileRemovedIds = mutableListOf<Long>()
}
