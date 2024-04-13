package foatto.core_compose.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = selectedTabIndex,
        ) {
            alTabInfo.forEachIndexed { index, tabInfo ->
                Tab(
                    modifier = Modifier.wrapContentWidth(),
                    selected = index == selectedTabIndex,
                    onClick = { onTabClick(index) },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Spacer(modifier = Modifier.weight(1.0f))
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically).weight(1.0f).wrapContentWidth(),
                            text = tabInfo.alText.joinToString("\n"),
                        )
                        Spacer(modifier = Modifier.weight(1.0f))
                        IconButton(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            onClick = {
                                onTabCloseClick(index)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
