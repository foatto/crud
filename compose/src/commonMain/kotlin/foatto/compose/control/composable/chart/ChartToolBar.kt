package foatto.compose.control.composable.chart

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
import foatto.compose.control.model.chart.ChartVisibleData
import foatto.compose.control.model.chart.ChartWorkMode
import foatto.compose.getStyleToolbarIconNameSuffix

@Composable
internal fun ChartToolBar(
    isWideScreen: Boolean,
    isPanButtonEnabled: Boolean,
    isZoomButtonEnabled: Boolean,
    isShowChartList: Boolean,
    alChartVisibleData: SnapshotStateList<ChartVisibleData>,
    refreshInterval: Int,
    setMode: (chartWorkMode: ChartWorkMode) -> Unit,
    zoomIn: () -> Unit,
    zoomOut: () -> Unit,
    onShowChartList: () -> Unit,
    doCloseChartList: () -> Unit,
    onChartListClick: (data: ChartVisibleData) -> Unit,
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
            ToolBarIconButton(
                isEnabled = isPanButtonEnabled && refreshInterval == 0,
                name = "/images/ic_open_with_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                setMode(ChartWorkMode.PAN)
            }
            ToolBarIconButton(
                isEnabled = isZoomButtonEnabled && refreshInterval == 0,
                name = "/images/ic_search_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                setMode(ChartWorkMode.ZOOM_BOX)
            }
        }
        ToolBarBlock {
            ToolBarIconButton(
                isEnabled = refreshInterval == 0,
                name = "/images/ic_zoom_in_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                zoomIn()
            }
            ToolBarIconButton(
                isEnabled = refreshInterval == 0,
                name = "/images/ic_zoom_out_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                zoomOut()
            }
        }
        ToolBarBlock {
            ToolBarIconButton(
                isEnabled = refreshInterval == 0,
                name = "/images/ic_timeline_${getStyleToolbarIconNameSuffix()}.png",
            ) {
                onShowChartList()
            }
            if (refreshInterval == 0 && isShowChartList) {
//                                        backgroundColor(getColorPopupMenuBack())
//                                        color(colorMenuTextDefault)
//                                        setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                DropdownMenu(
                    expanded = isShowChartList,
                    onDismissRequest = {
                        doCloseChartList()
                    },
                ) {
                    for (data in alChartVisibleData) {
                        DropdownMenuItem(
//                                                backgroundColor(getColorButtonBack())
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            },
                            text = {
                                Text(data.descr)
                            },
                            contentPadding = PaddingValues(16.dp),
                            onClick = {
                                doCloseChartList()
                                onChartListClick(data)
                            },
                        )
                    }
                }
            }
        }
        ToolBarBlock {
//                    for (legend in alGrLegend) {
//                        Button(
//                            attrs = {
//                                style {
//                                    legend.style(this)
//                                }
//                                title(legend.text)
//                            }
//                        ) {
//                            Text(legend.text)
//                        }
//                    }
        }
        ToolBarBlock {
//                    if (!styleIsNarrowScreen) {
//                        getToolBarIconButton(
//                            refreshInterval.value == 0,
//                            url = "/web/images/ic_menu_${getStyleIconNameSuffix()}.png",
//                            text = "Включить/выключить отдельный показ данных"
//                        ) {
//                            isShowGraphicDataVisible.value = !isShowGraphicDataVisible.value
//                        }
//                        if (refreshInterval.value == 0 && isShowGraphicDataVisible.value) {
//                            Div(
//                                attrs = {
//                                    style {
//                                        zIndex(Z_INDEX_GRAPHIC_DATA_LIST)   // popup menu must be above than table headers
//                                        position(Position.Absolute)
//                                        top(styleGraphicDataTop)
//                                        right(0.px)
//                                        width(auto)
//                                        setGraphicDataMaxWidth()
//                                        backgroundColor(COLOR_GRAPHIC_DATA_BACK)
//                                        setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
//                                        fontSize(arrStyleMenuFontSize[0])
//                                        setPaddings(arrStyleMenuStartPadding)
//                                        overflow("auto")
//                                        cursor("pointer")
//                                    }
//                                }
//                            ) {
//                                for (data in alGraphicDataData) {
//                                    Text(data)
//                                    Br()
//                                }
//                            }
//                        }
//                    }
        }
        RefreshSubToolBar(
            isWideScreen = isWideScreen,
            refreshInterval = refreshInterval,
        ) { interval ->
            setInterval(interval)
        }
    }
}
