package foatto.core_compose.controls.composables.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import foatto.core_compose.controls.composables.ToolBarBlock
import foatto.core_compose.controls.composables.ToolBarIconButton
import foatto.core_compose.controls.models.table.AddActionButtonClient
import foatto.core_compose.controls.models.table.ClientActionButtonClient
import foatto.core_compose.controls.models.table.ServerActionButtonClient

@Composable
fun TableToolBar(
    isWideScreen: Boolean,
    styleIconNameSuffix: String,    // getStyleIconNameSuffix()
    selectorCancelUrl: String,
    isFindTextVisible: Boolean,
    findText: String,
    alAddButton: SnapshotStateList<AddActionButtonClient>,
    isFormButtonVisible: Boolean,
    isGotoButtonVisible: Boolean,
    isPopupButtonVisible: Boolean,
    alServerButton: SnapshotStateList<ServerActionButtonClient>,
    alClientButton: SnapshotStateList<ClientActionButtonClient>,
    appParam: String,
    onFindInput: (newText: String) -> Unit,
    doFind: (isClear: Boolean) -> Unit,
    doForm: () -> Unit,
    doGoto: () -> Unit,
    doPopup: () -> Unit,
    clientAction: (action: String, params: List<Pair<String, String>>) -> Unit,
    call: (appParam: String, inNewWindow: Boolean) -> Unit,    // call(appParam, false)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
//                        padding(styleControlPadding)
//                        backgroundColor(getColorTableToolbarBack())
    ) {
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = selectorCancelUrl.isNotEmpty(),
                name = "/web/images/ic_reply_all_$styleIconNameSuffix.png",
                tooltip = "Отменить выбор",
            ) {
                call(selectorCancelUrl, false)
            }
            if (isFindTextVisible) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { newText ->
                        onFindInput(newText)
                    },
                    label = { Text("Поиск...") },
                    singleLine = true,
//                        setBorder(color = colorMainBorder, arrRadius = getStyleTableFindEditorBorderRadius())
//                        fontSize(styleTableFindEditorFontSize)
//                        padding(getStyleTableFindEditorPadding())
//                        setMargins(getStyleTableFindControlMargin())
                )
//                    size(getStyleTableFindEditLength())
//                    onKeyUp { event ->
//                        if (event.key == "Enter") {
//                            doFind(false)
//                        }
//                    }
//                }
            }
            ToolBarIconButton(
                isVisible = selectorCancelUrl.isNotEmpty(),
                name = "/web/images/ic_search_$styleIconNameSuffix.png",
                tooltip = "Искать",
//                                backgroundColor(getColorTableFindButtonBack())
//                                setBorder(color = getColorButtonBorder(), arrRadius = getStyleTableFindButtonBorderRadius())
//                                fontSize(styleCommonButtonFontSize)
//                                padding(styleIconButtonPadding)
//                                setMargins(getStyleTableFindControlMargin())
//                                cursor("pointer")
            ) {
                doFind(false)
            }
            if (findText.isNotEmpty()) {
                ToolBarIconButton(
                    name = "/web/images/ic_youtube_searched_for_$styleIconNameSuffix.png",
                    tooltip = "Отключить поиск",
//                            backgroundColor(getColorTableFindButtonBack())
//                            setBorder(color = getColorButtonBorder(), arrRadius = getStyleTableFindButtonBorderRadius())
//                            fontSize(styleCommonButtonFontSize)
//                            padding(styleIconButtonPadding)
//                            setMargins(getStyleTableFindControlMargin())
//                            cursor("pointer")
                ) {
                    doFind(true)
                }
            }
        }

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
                name = "/web/images/ic_mode_edit_$styleIconNameSuffix.png",
                tooltip = "Открыть форму"
            ) {
                doForm()
            }
            ToolBarIconButton(
                isVisible = (isWideScreen || !isFindTextVisible) && isGotoButtonVisible,
                name = "/web/images/ic_exit_to_app_$styleIconNameSuffix.png",
                tooltip = "Перейти"
            ) {
                doGoto()
            }
            ToolBarIconButton(
                isVisible = (isWideScreen || !isFindTextVisible) && isPopupButtonVisible,
                name = "/web/images/ic_menu_$styleIconNameSuffix.png",
                tooltip = "Показать операции по строке"
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
                    call(serverButton.action, serverButton.inNewWindow)
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
                    name = "/web/images/ic_sync_$styleIconNameSuffix.png",
                    tooltip = "Обновить",
//                            backgroundColor(getColorRefreshButtonBack())
//                            fontSize(styleCommonButtonFontSize)
//                            setBorder(getStyleToolbarButtonBorder())
//                            padding(styleIconButtonPadding)
//                            setMargins(arrStyleCommonMargin)
//                            cursor("pointer")
                ) {
                    call(appParam, false)
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