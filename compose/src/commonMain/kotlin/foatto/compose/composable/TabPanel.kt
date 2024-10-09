package foatto.compose.composable

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import foatto.compose.colorTabActive
import foatto.compose.colorTabInactive
import foatto.compose.model.TabInfo

@Composable
internal fun TabPanel(
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
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(
                            if (index == selectedTabIndex) {
                                colorTabActive
                            } else {
                                colorTabInactive
                            }
                        ),
                    selected = index == selectedTabIndex,
                    onClick = { onTabClick(index) },
                ) {
                    Row(
                        //horizontalArrangement = Arrangement.SpaceBetween, - don't work properly, use Spacer
                    ) {
                        Spacer(modifier = Modifier.weight(1.0f))
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1.0f)
                                .wrapContentWidth(),
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            text = tabInfo.alText.joinToString("\n"),
                        )
                        Spacer(modifier = Modifier.weight(1.0f))
                        IconButton(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            enabled = alTabInfo.size > 1,
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
