package foatto.compose.control.composable.scheme

import androidx.compose.foundation.background
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
import foatto.compose.colorToolBar
import foatto.compose.control.composable.RefreshSubToolBar
import foatto.compose.control.composable.ToolBarBlock
import foatto.compose.control.composable.ToolBarIconButton
import foatto.compose.control.model.scheme.ElementMoveData
import foatto.compose.control.model.table.ServerActionButtonClient
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.core.model.AppAction

@Composable
internal fun SchemeToolBar(
    isWideScreen: Boolean,
    isElementMoveEnabled: Boolean,
    isShowElementList: Boolean,
    alServerButton: SnapshotStateList<ServerActionButtonClient>,
    alMoveableElementData: SnapshotStateList<ElementMoveData>,
    refreshInterval: Int,
    onServerButtonClick: (action: AppAction) -> Unit,
    onMoveButtonClick: () -> Unit,
    doCloseElementList: () -> Unit,
    onElementListClick: (data: ElementMoveData) -> Unit,
    setInterval: (interval: Int) -> Unit,
) {
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
        ToolBarBlock {
            // Space()?
        }
        ToolBarBlock {
            for (serverButton in alServerButton) {
                ToolBarIconButton(
                    isVisible = isWideScreen || !serverButton.isForWideScreenOnly,
                    name = serverButton.name,
                ) {
                    onServerButtonClick(serverButton.action)
                }
            }
        }
        ToolBarBlock {
            if (isElementMoveEnabled) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0,
                    name = "/images/ic_touch_app_${getStyleToolbarIconNameSuffix()}.png",
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