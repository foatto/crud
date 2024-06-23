package foatto.compose.controls.composables.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.compose.control.composable.ToolBarBlock
import foatto.compose.control.composable.ToolBarIconButton
import foatto.compose.control.model.table.AddActionButtonClient
import foatto.compose.control.model.table.ClientActionButtonClient
import foatto.compose.control.model.table.ServerActionButtonClient
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.core.model.AppAction

@Composable
fun TableToolBar(
    isSelectorMode: Boolean,
    isWideScreen: Boolean,
    selectorCancelAction: AppAction?,
    isFindTextVisible: Boolean,
    findText: String,
    alAddButton: SnapshotStateList<AddActionButtonClient>,
    isFormButtonVisible: Boolean,
    isGotoButtonVisible: Boolean,
    isPopupButtonVisible: Boolean,
    alServerButton: SnapshotStateList<ServerActionButtonClient>,
    alClientButton: SnapshotStateList<ClientActionButtonClient>,
    tableAction: AppAction,
    onFindInput: (newText: String) -> Unit,
    doFind: (isClear: Boolean) -> Unit,
    doForm: () -> Unit,
    doGoto: () -> Unit,
    doPopup: () -> Unit,
    clientAction: (action: AppAction, params: List<Pair<String, String>>) -> Unit,
    call: (action: AppAction, inNewTab: Boolean) -> Unit,    // call(appParam, false)
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
            ToolBarIconButton(
                isVisible = selectorCancelAction != null,
                name = "/web/images/ic_reply_all_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Отменить выбор",
            ) {
                selectorCancelAction?.let {
                    call(selectorCancelAction, false)
                }
            }
            if (isFindTextVisible) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { newText ->
                        onFindInput(newText)
                    },
                    label = { Text("Поиск...") },
                    singleLine = true,
                )
//                    onKeyUp { event ->
//                        if (event.key == "Enter") {
//                            doFind(false)
//                        }
//                    }
//                }
            }
            ToolBarIconButton(
                isVisible = selectorCancelAction != null,
                name = "/web/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Искать",
            ) {
                doFind(false)
            }
            if (findText.isNotEmpty()) {
                ToolBarIconButton(
                    name = "/web/images/ic_youtube_searched_for_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Отключить поиск",
                ) {
                    doFind(true)
                }
            }
        }

        if (!isSelectorMode) {
            ToolBarBlock {
                for (addButton in alAddButton) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !isFindTextVisible,
                        name = addButton.name,
                        tooltip = addButton.tooltip,
                    ) {
                        call(addButton.action, false)
                    }
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isFormButtonVisible,
                    name = "/web/images/ic_mode_edit_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Открыть форму",
                ) {
                    doForm()
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isGotoButtonVisible,
                    name = "/web/images/ic_exit_to_app_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Перейти",
                ) {
                    doGoto()
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isPopupButtonVisible,
                    name = "/web/images/ic_menu_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Показать действия по строке",
                ) {
                    doPopup()
                }
            }

            ToolBarBlock {
                for (serverButton in alServerButton) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || (!isFindTextVisible && !serverButton.isForWideScreenOnly),
                        name = serverButton.name,
                        tooltip = serverButton.tooltip,
                    ) {
                        call(serverButton.action, serverButton.inNewTab)
                    }
                }
            }

            ToolBarBlock {
                for (clientButton in alClientButton) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || (!isFindTextVisible && !clientButton.isForWideScreenOnly),
                        name = clientButton.name,
                        tooltip = clientButton.tooltip
                    ) {
                        clientAction(clientButton.action, clientButton.params)
                    }
                }
            }

            ToolBarBlock {
                if (isWideScreen || !isFindTextVisible) {
                    ToolBarIconButton(
                        name = "/web/images/ic_sync_${getStyleToolbarIconNameSuffix()}.png",
                        tooltip = "Обновить",
                    ) {
                        call(tableAction, false)
                    }
                }
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