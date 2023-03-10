package foatto.ts_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.CompositeResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.control.BaseCompositeControl
import foatto.core_compose_web.control.GRAPHIC_MIN_HEIGHT
import foatto.core_compose_web.control.GraphicControl
import foatto.core_compose_web.control.StateControl
import foatto.core_compose_web.control.composable.getRefreshSubToolbar
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose_web.control.composable.getXyServerActionSubToolbar
import foatto.core_compose_web.control.model.TitleData
import foatto.core_compose_web.control.model.XyServerActionButtonData
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val TS_XY_SVG_HEIGHT = 400

private const val TS_START_EXPAND_KOEF = 0.0

private const val TS_COMPOSITE_PREFIX = "ts_composite"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class TSCompositeControl(
    root: Root,
    appControl: AppControl,
    compositeResponse: CompositeResponse,
    tabId: Int,
) : BaseCompositeControl(root, appControl, compositeResponse, tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alXyServerButton = mutableStateListOf<XyServerActionButtonData>()

    private val showStateAlert = mutableStateOf(false)
    private val stateAlertMessage = mutableStateOf("")

    private val refreshInterval: MutableState<Int> = mutableStateOf(0)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var refreshHandlerId = 0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val stateControl = StateControl(root, appControl, compositeResponse.xyResponse, tabId).apply {
        containerPrefix = TS_COMPOSITE_PREFIX
    }
    private val graphicControl = GraphicControl(root, appControl, compositeResponse.graphicResponse, tabId).apply {
        containerPrefix = TS_COMPOSITE_PREFIX
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- State Header
            getGraphicAndXyHeader(TS_COMPOSITE_PREFIX)

            //--- Composite Toolbar
            getGraphicAndXyToolbar(TS_COMPOSITE_PREFIX) {
                getToolBarSpan {
                    // empty, for aligning refresh buttons to right side
                }
                getXyServerActionSubToolbar(alXyServerButton) { url ->
                    invokeServerButton(url)
                }
                getToolBarSpan {
                    // empty, for aligning refresh buttons to right side
                }
                getRefreshSubToolbar(refreshInterval) { interval ->
                    setInterval(interval)
                }
            }

            Div(
                attrs = {
                    style {
                        height(100.percent)
                        overflow("auto")
                        display(DisplayStyle.Grid)
                        gridTemplateRows("${TS_XY_SVG_HEIGHT}px ${GRAPHIC_MIN_HEIGHT * 2}px")
                        gridTemplateColumns("repeat(1,auto)")
                    }
                }
            ) {
                Div(
                    attrs = {
                        style {
                            justifySelf("stretch")  // horizontal align
                            alignSelf(AlignSelf.Stretch)
                            gridArea("1", "1", "2", "2")
                        }
                    }
                ) {
                    stateControl.getXyElementTemplate(false)
                }
                Div(
                    attrs = {
                        style {
                            justifySelf("stretch")  // horizontal align
                            alignSelf(AlignSelf.Stretch)
                            gridArea("2", "1", "3", "2")
                        }
                    }
                ) {
                    graphicControl.getGraphicElementTemplate(false)
                }
            }

            stateControl.getStateAlertBody()
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        val xyResponse = compositeResponse.xyResponse

        root.setTabInfo(tabId, xyResponse.shortTitle, xyResponse.fullTitle)
        alTitleData += xyResponse.fullTitle.split('\n').filter { it.isNotBlank() }.map { TitleData("", it) }

        XyServerActionButtonData.readXyServerActionButton(xyResponse.arrServerActionButton, alXyServerButton)

        stateControl.doXySpecificComponentMounted(
            startExpandKoef = TS_START_EXPAND_KOEF,
            isCentered = true,
            curScale = 1,
            svgHeight = TS_XY_SVG_HEIGHT,
            arrAddHeights = emptyArray(),  // верхний элемент, нет элементов выше его
        )

        graphicControl.doGraphicSpecificComponentMounted(
            svgHeight = GRAPHIC_MIN_HEIGHT * 2,
            arrAddHeights = arrayOf(TS_XY_SVG_HEIGHT),
        )

        setInterval(10)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun invokeServerButton(url: String) {
        root.openTab(url)
    }

    private fun setInterval(sec: Int) {
        if (refreshHandlerId != 0) {
            window.clearInterval(refreshHandlerId)
        }

        if (sec == 0) {
            refreshView(true)
        } else {
            refreshHandlerId = window.setInterval({
                refreshView(false)
            }, sec * 1000)
        }

        refreshInterval.value = sec
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun refreshView(withWait: Boolean) {
        stateControl.doStateRefreshView(
            aView = null,
            withWait = withWait,
            doAdditionalWork = { xyActionResponse: XyActionResponse ->
                xyActionResponse.arrParams?.firstOrNull { pair ->
                    pair.first == STATE_ALERT_MESSAGE
                }?.let { pair ->
                    stateAlertMessage.value = pair.second
                    showStateAlert.value = true
                }
            },
        )
        graphicControl.doGraphicRefresh(
            aView = null,
            withWait = withWait,
            arrAddHeights = arrayOf(TS_XY_SVG_HEIGHT),
        )
    }

}
