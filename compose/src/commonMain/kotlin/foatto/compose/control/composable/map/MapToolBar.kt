package foatto.compose.control.composable.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.compose.colorTextButton
import foatto.compose.control.composable.RefreshSubToolBar
import foatto.compose.control.composable.ToolBarBlock
import foatto.compose.control.composable.ToolBarIconButton
import foatto.compose.control.model.map.MapWorkMode
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.compose.singleButtonShape
import foatto.core.model.response.xy.XyElementConfig

@Composable
internal fun MapToolBar(
    isWideScreen: Boolean,
    isToolBarsVisible: Boolean,
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
    refreshInterval: Int,
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
                    isEnabled = isPanButtonEnabled && refreshInterval == 0,
                    name = "/images/ic_open_with_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    setMode(MapWorkMode.PAN)
                }
                ToolBarIconButton(
                    isEnabled = isZoomButtonEnabled && refreshInterval == 0,
                    name = "/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    setMode(MapWorkMode.ZOOM_BOX)
                }
                if (isWideScreen) {
                    ToolBarIconButton(
                        isEnabled = isDistancerButtonEnabled && refreshInterval == 0,
                        name = "/images/ic_linear_scale_${getStyleToolbarIconNameSuffix()}.png",
                    ) {
                        setMode(MapWorkMode.DISTANCER)
                    }
                    ToolBarIconButton(
                        isEnabled = isSelectButtonEnabled && refreshInterval == 0,
                        name = "/images/ic_touch_app_${getStyleToolbarIconNameSuffix()}.png",
                    ) {
                        setMode(MapWorkMode.SELECT_FOR_ACTION)
                    }
                }
            }
        }
        ToolBarBlock {
            if (isToolBarsVisible) {
                ToolBarIconButton(
                    isEnabled = isZoomInButtonEnabled && refreshInterval == 0,
                    name = "/images/ic_zoom_in_${getStyleToolbarIconNameSuffix()}.png",
                ) {
                    zoomIn()
                }
                ToolBarIconButton(
                    isEnabled = isZoomOutButtonEnabled && refreshInterval == 0,
                    name = "/images/ic_zoom_out_${getStyleToolbarIconNameSuffix()}.png",
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
                        shape = singleButtonShape,
                        colors = colorTextButton ?: ButtonDefaults.buttonColors(),
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
                isVisible = isEditPointButtonVisible && refreshInterval == 0,
                name = "/images/ic_format_shapes_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                startEditPoint()
            }
            ToolBarIconButton(
                isVisible = isMoveElementsButtonVisible && refreshInterval == 0,
                name = "/images/ic_zoom_out_map_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                startMoveElements()
            }
        }
        ToolBarBlock {
            ToolBarIconButton(
                isVisible = isActionOkButtonVisible && refreshInterval == 0,
                name = "/images/ic_save_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                actionOk()
            }
            ToolBarIconButton(
                isVisible = isActionCancelButtonVisible && refreshInterval == 0,
                name = "/images/ic_exit_to_app_${getStyleToolbarIconNameSuffix()}.png",
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