package foatto.core_compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import foatto.core_compose.models.TabInfo

@Composable
fun MainBody(
) {
    val isTabPanelVisible by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val alTabInfo = remember { mutableListOf<TabInfo>() }

    Column(
        modifier = Modifier
            .fillMaxWidth(1f)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TabPanel(
            isTabPanelVisible = isTabPanelVisible,
            selectedTabIndex = selectedTabIndex,
            alTabInfo = alTabInfo,
            onTabClick = { index ->
                selectedTabIndex = index
            },
            onTabCloseClick = { index ->
                //!!! close tab
            },
        )
    }
}

/*
    //--- Tab non-state variables ---

    var lastTabId: Int = 0
*/