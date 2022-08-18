import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

//!!! try 'val' instead 'var' ?
var alTabInfo = mutableStateListOf<TabInfo>()
var currentTabIndex = mutableStateOf(0)

class TabInfo(
    val id: Int,
    var arrText: Array<String>, // try List<String> in Web, may don't work
    var tooltip: String,
)