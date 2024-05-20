package foatto.core_compose.controls.composables.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.core.link.XyElementConfig
import foatto.core_compose.controls.composables.RefreshSubToolBar
import foatto.core_compose.controls.composables.ToolBarBlock
import foatto.core_compose.controls.composables.ToolBarIconButton
import foatto.core_compose.controls.models.map.MapWorkMode
import foatto.core_compose.getStyleToolbarIconNameSuffix

@Composable
internal fun MapToolBar(
    isWideScreen: Boolean,
    isToolBarsVisible: Boolean,
    refreshInterval: Int,
    isPanButtonEnabled: Boolean,
    isZoomButtonEnabled: Boolean,
    isDistancerButtonEnabled: Boolean,
    isSelectButtonEnabled: Boolean,
    isZoomInButtonEnabled: Boolean,
    isZoomOutButtonEnabled: Boolean,
    isAddElementButtonVisible: Boolean,
    isEditPointButtonVisible: Boolean,
    isMoveElementsButtonVisible: Boolean,
    isActionOkButtonVisible: Boolean,
    isActionCancelButtonVisible: Boolean,
    alAddEC: List<XyElementConfig>,
    setMode: (mapWorkMode: MapWorkMode) -> Unit,
    zoomIn: () -> Unit,
    zoomOut: () -> Unit,
    startAdd: (elementConfig: XyElementConfig) -> Unit,
    startEditPoint: () -> Unit,
    startMoveElements: () -> Unit,
    actionOk: () -> Unit,
    actionCancel: () -> Unit,
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
            if (isToolBarsVisible) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0 && isPanButtonEnabled,
                    name = "/web/images/ic_open_with_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Перемещение по карте",
                ) {
                    setMode(MapWorkMode.PAN)
                }
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0 && isZoomButtonEnabled,
                    name = "/web/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Выбор области для показа",
                ) {
                    setMode(MapWorkMode.ZOOM_BOX)
                }
                if (isWideScreen) {
                    ToolBarIconButton(
                        isEnabled = refreshInterval == 0 && isDistancerButtonEnabled,
                        name = "/web/images/ic_linear_scale_${getStyleToolbarIconNameSuffix()}.png",
                        tooltip = "Измерение расстояний",
                    ) {
                        setMode(MapWorkMode.DISTANCER)
                    }
                    ToolBarIconButton(
                        isEnabled = refreshInterval == 0 && isSelectButtonEnabled,
                        name = "/web/images/ic_touch_app_${getStyleToolbarIconNameSuffix()}.png",
                        tooltip = "Работа с объектами",
                    ) {
                        setMode(MapWorkMode.SELECT_FOR_ACTION)
                    }
                }
            }
        }
        ToolBarBlock {
            if (isToolBarsVisible) {
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0 && isZoomInButtonEnabled,
                    name = "/web/images/ic_zoom_in_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Ближе",
                ) {
                    zoomIn()
                }
                ToolBarIconButton(
                    isEnabled = refreshInterval == 0 && isZoomOutButtonEnabled,
                    name = "/web/images/ic_zoom_out_${getStyleToolbarIconNameSuffix()}.png",
                    tooltip = "Дальше",
                ) {
                    zoomOut()
                }
            }
        }
        ToolBarBlock {
            if (isAddElementButtonVisible) {
                if (alAddEC.isNotEmpty()) {
                    Text(text = "Добавить:")
                }
                for (ec in alAddEC) {
                    Button(
                        shape = RoundedCornerShape(0.dp),
                        onClick = {
                            startAdd(ec)
                        }
//                        title("Добавить ${ec.descrForAction}")
//                        backgroundColor(getColorButtonBack())
//                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                    ) {
                        Text(text = ec.descrForAction)
                    }
                }
            }
        }
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = refreshInterval == 0 && isEditPointButtonVisible,
                name = "/web/images/ic_format_shapes_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Редактирование точек",
            ) {
                startEditPoint()
            }
            ToolBarIconButton(
                isVisible = refreshInterval == 0 && isMoveElementsButtonVisible,
                name = "/web/images/ic_zoom_out_map_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Перемещение объектов",
            ) {
                startMoveElements()
            }
        }
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = refreshInterval == 0 && isActionOkButtonVisible,
                name = "/web/images/ic_save_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Сохранить",
            ) {
                actionOk()
            }
            ToolBarIconButton(
                isVisible = refreshInterval == 0 && isActionCancelButtonVisible,
                name = "/web/images/ic_exit_to_app_${getStyleToolbarIconNameSuffix()}.png",
                tooltip = "Отменить",
            ) {
                actionCancel()
            }
        }
        if (isToolBarsVisible) {
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