package foatto.compose.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import foatto.compose.colorTabActive
import foatto.compose.colorTabInactive
import foatto.compose.colorTabSelected
import foatto.compose.colorTabUnselected
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
            indicator =
            @Composable { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = colorTabSelected ?: TabRowDefaults.primaryContentColor,
                    )
                }
            },
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
                    selectedContentColor = colorTabSelected ?: LocalContentColor.current,
                    unselectedContentColor = colorTabUnselected ?: LocalContentColor.current,
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
                        FilledIconButton(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(0.dp),               // чтобы не срезало углы иконок
                            colors = IconButtonDefaults.iconButtonColors(), // иконки должны быть на прозрачном фоне
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
