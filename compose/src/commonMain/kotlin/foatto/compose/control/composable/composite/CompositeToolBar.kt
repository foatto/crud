package foatto.compose.control.composable.composite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.compose.colorToolBar
import foatto.compose.control.composable.RefreshSubToolBar
import foatto.compose.control.composable.ToolBarBlock
import foatto.compose.control.composable.ToolBarIconButton
import foatto.compose.control.model.composite.CompositeBlockControl
import foatto.compose.control.model.composite.CompositeWorkMode
import foatto.compose.getStyleToolbarIconNameSuffix

@Composable
internal fun CompositeToolBar(
    isWideScreen: Boolean,
    isToolBarsVisible: Boolean,
    isPanButtonEnabled: Boolean,
    isLayoutButtonEnabled: Boolean,
    isBlocksVisibilityButtonVisible: Boolean,
    isShowBlocksList: Boolean,
    blocks: SnapshotStateList<CompositeBlockControl>,
    isLayoutSaveButtonVisible: Boolean,
    isRefreshButtonsVisible: Boolean,
    refreshInterval: Int,
    setMode: (compositeWorkMode: CompositeWorkMode) -> Unit,
    onShowBlocksList: () -> Unit,
    doCloseBlocksList: () -> Unit,
    onBlocksListClick: (block: CompositeBlockControl) -> Unit,
    saveLayout: () -> Unit,
    setInterval: (interval: Int) -> Unit,
) {
    /*
        onShowChartList: () -> Unit,
        doCloseChartList: () -> Unit,
        onChartListClick: (data: ChartVisibleData) -> Unit,
        setInterval: (interval: Int) -> Unit,
     */
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp)
            .background(colorToolBar),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
//                        borderTop(
//                            width = getStyleTableCaptionBorderTop().width,
//                            lineStyle = getStyleTableCaptionBorderTop().style,
//                            color = getStyleTableCaptionBorderTop().color,
//                        )
//                        borderBottom(
//                            width = getStyleTableCaptionBorderTop().width,
//                            lineStyle = getStyleTableCaptionBorderTop().style,
//                            color = getStyleTableCaptionBorderTop().color,
//                        )
//                        backgroundColor(getColorTableToolbarBack())
    ) {
        if (isToolBarsVisible) {
            ToolBarBlock {
                ToolBarIconButton(
                    isEnabled = isPanButtonEnabled && refreshInterval == 0,
                    name = "/images/ic_open_with_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    setMode(CompositeWorkMode.PAN)
                }
                if (isWideScreen) {
                    ToolBarIconButton(
                        isEnabled = isLayoutButtonEnabled && refreshInterval == 0,
                        name = "/images/ic_touch_app_${getStyleToolbarIconNameSuffix()}.png",
                    ) {
                        setMode(CompositeWorkMode.LAYOUT)
                    }
                }
            }
        }
        ToolBarBlock {
        }
        ToolBarBlock {
            if (isBlocksVisibilityButtonVisible) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0,
                    name = "/images/ic_visibility_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    onShowBlocksList()
                }
            }
            if (isShowBlocksList && refreshInterval == 0) {
//                                        backgroundColor(getColorPopupMenuBack())
//                                        color(colorMenuTextDefault)
//                                        setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                DropdownMenu(
                    expanded = isShowBlocksList,
                    onDismissRequest = {
                        doCloseBlocksList()
                    },
                ) {
                    for (block in blocks) {
                        val headerData = block.chartBlock?.headerData
                            ?: block.mapBlock?.headerData
                            ?: block.schemeBlock?.headerData
                            ?: block.tableBlock?.headerData
                        val text = headerData?.rows?.firstOrNull()?.second ?: "Не задан заголовок блока!"

                        DropdownMenuItem(
//                                                backgroundColor(getColorButtonBack())
                            text = {
                                Text(text)
                            },
                            trailingIcon = if (!block.isHidden) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                            contentPadding = PaddingValues(16.dp),
                            onClick = {
                                doCloseBlocksList()
                                onBlocksListClick(block)
                            },
                        )
                    }
                }
            }
            if (isLayoutSaveButtonVisible) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0,
                    name = "/images/ic_save_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    saveLayout()
                }
            }
        }
        ToolBarBlock {
        }
        if (isToolBarsVisible && isRefreshButtonsVisible) {
            RefreshSubToolBar(
                isWideScreen = isWideScreen,
                refreshInterval = refreshInterval,
            ) { interval ->
                setInterval(interval)
            }
        }
    }
}

/*
                getToolBarSpan {
                    Input(InputType.Text) {
                        style {
                            border(width = 0.px)
                            outline("none")
                            backgroundColor(hsla(0, 0, 0, 0))
                            color(hsla(0, 0, 0, 0))
                        }
                        id("table_cursor_$tabId")
                        readOnly()
                        size(1)
                        onInput { syntheticInputEvent ->
                            findText.value = syntheticInputEvent.value
                        }
                        onKeyUp { syntheticKeyboardEvent ->
                            when (syntheticKeyboardEvent.key) {
                                "Enter" -> doKeyEnter()
                                "Escape" -> doKeyEsc()
                                "ArrowUp" -> doKeyUp()
                                "ArrowDown" -> doKeyDown()
                                "Home" -> doKeyHome()
                                "End" -> doKeyEnd()
                                "PageUp" -> doKeyPageUp()
                                "PageDown" -> doKeyPageDown()
                                "F4" -> closeTabById()
                            }
                        }
                    }
 */