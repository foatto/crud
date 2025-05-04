package foatto.compose.control.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import foatto.compose.colorControlBack
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorToolBar
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
    selectorCancelAction: (() -> Unit)?,
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
    ) {
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = selectorCancelAction != null,
                name = "/images/ic_reply_all_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                selectorCancelAction?.invoke()
            }
            if (isFindTextVisible) {
                OutlinedTextField(
                    modifier = Modifier.background(colorControlBack).onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter) {
                            doFind(false)
                            true
                        } else {
                            false
                        }
                    },
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = findText,
                    onValueChange = { newText ->
                        onFindInput(newText)
                    },
                    //label = { Text("Поиск...") }, - появляется паразитный белый фон вокруг рамки
                    placeholder = { Text("Поиск...") },
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
                name = "/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                doFind(false)
            }
            ToolBarIconButton(
                isVisible = findText.isNotBlank(),
                name = "/images/ic_youtube_searched_for_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                doFind(true)
            }
        }

        if (!isSelectorMode) {
            ToolBarBlock {
                for (addButton in alAddButton) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || !isFindTextVisible,
                        name = addButton.name,
                    ) {
                        call(addButton.action, false)
                    }
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isFormButtonVisible,
                    name = "/images/ic_mode_edit_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    doForm()
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isGotoButtonVisible,
                    name = "/images/ic_exit_to_app_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    doGoto()
                }
                ToolBarIconButton(
                    isVisible = (isWideScreen || !isFindTextVisible) && isPopupButtonVisible,
                    name = "/images/ic_menu_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    doPopup()
                }
            }

            ToolBarBlock {
                for (serverButton in alServerButton) {
                    ToolBarIconButton(
                        isVisible = isWideScreen || (!isFindTextVisible && !serverButton.isForWideScreenOnly),
                        name = serverButton.name,
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
                    ) {
                        clientAction(clientButton.action, clientButton.params)
                    }
                }
            }
        }

        ToolBarBlock {
            if (isWideScreen || !isFindTextVisible) {
                ToolBarIconButton(
                    name = "/images/ic_sync_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    call(tableAction, false)
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