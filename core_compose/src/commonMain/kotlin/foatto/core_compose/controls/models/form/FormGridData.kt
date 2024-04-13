package foatto.core_compose.controls.models.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp

internal class FormGridData(
    val cellType: FormCellTypeClient,

    val id: Int,
    val cellName: String,

//    val style: StyleScope.() -> Unit,
//    val styleAdd: StyleScope.() -> Unit,

//    val isHidden: Boolean = false, - теперь hidden-cells в отдельном списке
    val isVisible: MutableState<Boolean> = mutableStateOf(true),
    val error: String = "",

    aText: String = "",
    aBool: Boolean = false,
    aAlDateTime: List<String> = emptyList(),
    aCombo: Int = 0,

    val isPassword: Boolean = false,

    val colCount: Int = 20,
    val rowCount: Int = 1,
    val isReadOnly: Boolean = false,

    val alSwitchText: List<String> = emptyList(),
    val alComboData: List<FormComboData> = emptyList(),

    val fileId: Int = 0,
    var alFileData: SnapshotStateList<FormFileData> = mutableStateListOf(),
) {
    var alSubId: MutableList<Int>? = null

    var text: MutableState<String> = mutableStateOf(aText)
    val oldText: String = aText

    var bool: MutableState<Boolean> = mutableStateOf(aBool)
    val oldBool: Boolean = aBool

    val alDateTime: List<MutableState<String>> = aAlDateTime.map { mutableStateOf(it) }
    val oldAlDateTime: List<String> = aAlDateTime.map { it }

    val combo: MutableState<Int> = mutableStateOf(aCombo)
    val oldCombo: Int = aCombo

    var selectorSetURL: String = ""
    var selectorClearURL: String = ""

    val hmFileAdd = mutableMapOf<Int, String>()
    val alFileRemovedId = mutableListOf<Int>()

    var componentWidth by mutableStateOf(0.dp)
}
