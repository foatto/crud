package foatto.core_compose.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import foatto.core_compose.controls.models.TitleData

abstract class AbstractControl(
    protected val tabId: Int,
) {

    protected val alTitleData: SnapshotStateList<TitleData> = mutableStateListOf()

    @Composable
    abstract fun Body()

    abstract fun start()

}
