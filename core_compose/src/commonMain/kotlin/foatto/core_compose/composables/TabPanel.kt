package foatto.core_compose.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import foatto.core_compose.models.TabInfo

@Composable
fun TabPanel(
    isTabPanelVisible: Boolean,
    selectedTabIndex: Int,
    alTabInfo: List<TabInfo>,
    onTabClick: (index: Int) -> Unit,
    onTabCloseClick: (index: Int) -> Unit,
) {
    if (isTabPanelVisible) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
        ) {
            alTabInfo.forEachIndexed { index, tabInfo ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { onTabClick(index) },
                ) {
                    Text(
                        text = tabInfo.alText.joinToString("\n"),
                    )
                    Text(
                        text = "X",
                        modifier = Modifier.clickable(
                            onClick = {
                                onTabCloseClick(index)
                            }
                        ),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
