package foatto.core_compose.controls.composables.scheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.core_compose.controls.ElementMoveData
import foatto.core_compose.controls.composables.RefreshSubToolBar
import foatto.core_compose.controls.composables.ToolBarBlock
import foatto.core_compose.controls.composables.ToolBarIconButton
import foatto.core_compose.controls.models.table.ServerActionButtonClient
import foatto.core_compose.getStyleToolbarIconNameSuffix

@Composable
internal fun SchemeToolBar(
    isWideScreen: Boolean,
    refreshInterval: Int,
    isElementMoveEnabled: Boolean,
    isShowElementList: Boolean,
    alServerButton: SnapshotStateList<ServerActionButtonClient>,
    alMoveableElementData: SnapshotStateList<ElementMoveData>,
    onServerButtonClick: (url: String) -> Unit,
    onMoveButtonClick: () -> Unit,
    doCloseElementList: () -> Unit,
    onElementListClick: (data: ElementMoveData) -> Unit,
    setInterval: (interval: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
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
        ToolBarBlock {
            // Space()?
        }
        ToolBarBlock {
            for (serverButton in alServerButton) {
                if (isWideScreen || !serverButton.isForWideScreenOnly) {
                    ToolBarIconButton(
                        name = serverButton.name,
                        tooltip = serverButton.tooltip,
                    ) {
                        onServerButtonClick(serverButton.action)
                    }
                }
            }
        }
        ToolBarBlock {
            if (isElementMoveEnabled) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0,
                    name = "/web/images/ic_touch_app_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Перемещение датчиков по объекту",
                ) {
                    onMoveButtonClick()
                }
                if (refreshInterval == 0 && isShowElementList) {
//                                        backgroundColor(getColorPopupMenuBack())
//                                        color(colorMenuTextDefault)
//                                        setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                    DropdownMenu(
                        expanded = isShowElementList,
                        onDismissRequest = {
                            doCloseElementList()
                        },
                    ) {
                        for (data in alMoveableElementData) {
                            DropdownMenuItem(
//                                                backgroundColor(getColorButtonBack())
                                text = {
                                    Text(data.descr)
                                },
                                contentPadding = PaddingValues(16.dp),
                                onClick = {
                                    doCloseElementList()
                                    onElementListClick(data)
                                },
                            )
                        }
                    }
                }
            }
        }
        RefreshSubToolBar(
            isWideScreen = isWideScreen,
            refreshInterval = refreshInterval,
        ) { interval ->
            setInterval(interval)
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