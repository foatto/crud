package foatto.core_web

import foatto.core.app.graphic.GraphicAction
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicElement
import foatto.core.app.graphic.GraphicViewCoord
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.GraphicResponse
import foatto.core.link.SaveUserPropertyRequest
import foatto.core.util.getSplittedDouble
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

//--- (считаем что больше трёх графиков не будет)
private const val MAX_AXIS_COUNT = 3

private const val MARGIN_LEFT = 100     // на каждую ось Y

//private const val MARGIN_RIGHT = 40 - вычисляется динамически по размеру шрифта
private const val MARGIN_TOP = 40
private const val MARGIN_BOTTOM = 60

const val GRAPHIC_MIN_HEIGHT = 300

private const val MIN_GRID_STEP_X = 40  // минимальный шаг между линиями сетки в пикселях
private const val MIN_GRID_STEP_Y = 40  // минимальный шаг между линиями сетки в пикселях

private const val MIN_SCALE_X = 15 * 60     // минимальный разрешённый масштаб - диапазон не менее 15 мин
private const val MAX_SCALE_X = 32 * 86400  // максимальный разрешённый масштаб - диапазон не более 32 дней (чуть более месяца)

private const val GRAPHIC_TEXT_HEIGHT = 20              // высота текстового блока
private const val GRAPHIC_TEXT_MIN_VISIBLE_WIDTH = 4    // минимальная ширина видимого текстового блока

private const val LEGEND_TEXT_MARGIN = 4        // поля вокруг текста легенды справа графика

private val arrGridStepX = arrayOf(
    1, 5, 15,                           // 1 - 5 - 15 seconds
    1 * 60, 5 * 60, 15 * 60,            // 1 - 5 - 15 minutes
    1 * 3_600, 3 * 3_600, 6 * 3_600,    // 1 - 3 - 6 hours
    1 * 86_400, 3 * 86_400, 9 * 86_400, // 1 - 3 - 9 days
    27 * 86_400, 81 * 86_400            // 27 - 81 days
)

private val arrGridStepY = arrayOf(
    0.001, 0.002, 0.0025, 0.005,
    0.01, 0.02, 0.025, 0.05,
    0.1, 0.2, 0.25, 0.5,
    1.0, 2.0, 2.5, 5.0,
    10.0, 20.0, 25.0, 50.0,
    100.0, 200.0, 250.0, 500.0,
    1_000.0, 2_000.0, 2_500.0, 5_000.0,
    10_000.0, 20_000.0, 25_000.0, 50_000.0,
    100_000.0, 200_000.0, 250_000.0, 500_000.0,
    1_000_000.0, 2_000_000.0, 2_500_000.0, 5_000_000.0,
    10_000_000.0, 20_000_000.0, 25_000_000.0, 50_000_000.0,
)

private val arrPrecY = arrayOf(
    3, 3, 4, 3,
    2, 2, 3, 2,
    1, 1, 2, 1,
    0, 0, 1, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
)

private enum class GraphicWorkMode {
    PAN, ZOOM_BOX/*, SELECT_FOR_PRINT*/
}
/*
    private lateinit var butElementVisible: Button

        butElementVisible = Button( null, ImageView( Image( c.getResourceAsStream( "/images/ic_visibility_black_18dp.png" ) ) ) )
        butElementVisible.tooltip = Tooltip( "Показать/скрыть график" )
        butElementVisible.font = curControlFont
        butElementVisible.setOnAction( this )
*/

@Suppress("UnsafeCastFromDynamic")
fun graphicControl(graphicResponse: GraphicResponse, tabId: Int) = vueComponentOptions().apply {

    this.template = """
        <div>
            <div id="graphic_title_$tabId" v-bind:style="[ style_toolbar, style_header ]">
                <span v-bind:style="style_toolbar_block">
                </span>
                <span v-bind:style="[style_toolbar_block, style_title]">
                    {{fullTitle}}
                </span>
                <span v-bind:style="style_toolbar_block">
                </span>
            </div>
            <div id="graphic_toolbar_$tabId" v-bind:style="style_toolbar">
                
                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_open_with_black_48dp.png" 
                         v-on:click="setModePan()"
                         v-bind:style="style_icon_button"
                         v-bind:disabled="isPanButtonDisabled"
                         title="Перемещение по графику"
                    >
                    <img src="/web/images/ic_search_black_48dp.png" 
                         v-on:click="setModeZoomBox()"
                         v-bind:style="style_icon_button"
                         v-bind:disabled="isZoomButtonDisabled"
                         title="Выбор области для показа"
                    >
                </span>                   

                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_zoom_in_black_48dp.png"
                         v-bind:style="style_icon_button"
                         title="Ближе"
                         v-on:click="zoomIn()"
                    >
                    <img src="/web/images/ic_zoom_out_black_48dp.png"
                         v-bind:style="style_icon_button"
                         title="Дальше"
                         v-on:click="zoomOut()"
                    >
                </span>

                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_timeline_black_48dp.png"
                         v-bind:style="style_icon_button"
                         v-on:click="isShowGraphicVisibility=!isShowGraphicVisibility"
                         title="Включить/выключить отдельные графики"
                    >
        
                    <div v-show="isShowGraphicVisibility" 
                         v-bind:style="style_visibility_list"
                    >
                        <template v-for="data in arrGraphicVisibleData">
                            <input type="checkbox"
                                   v-model="data.check"
                                   v-bind:style="style_graphic_visibility_checkbox"
                            >
                            {{ data.descr }}
                            <br>
                        </template>
                        
                        <br>
                        &nbsp;&nbsp;&nbsp;&nbsp;
                                                
                        <button v-on:click="doChangeGraphicVisibility()"
                                v-bind:style="style_graphic_visibility_button"
                                title="Применить изменения"                                                                        
                        >
                            OK
                        </button>
                    </div>
                </span>

                <span v-bind:style="style_toolbar_block">
                    <template v-for="legend in arrGrLegend">
                        <button v-bind:style="legend.style"
                                v-bind:title="legend.text"
                        >
                            {{ legend.text }}
                        </button>
                    </template>
                </span>

                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_sync_black_48dp.png"
                         v-bind:style="style_icon_button"
                         title="Обновить"
                         v-on:click="grRefreshView( null, null )"
                    >
                </span>
            </div>
        """ +

        getGraphicElementTemplate(tabId) +

        """
        </div>
    """
/*
    private val panTimeBar = HBox( iAppContainer.DEFAULT_SPACING )
    private val arrTxtDateTime = arrayOfNulls<TextField>( 10 )
    private lateinit var butShowForTime: Button

        //--- нижняя панель временнОго масштабирования
        for( i in arrTxtDateTime.indices ) {
            arrTxtDateTime[ i ] = TextField()
            arrTxtDateTime[ i ]!!.setPrefColumnCount( if( i == 2 || i == 7 ) 4 else 2 )
            arrTxtDateTime[ i ]!!.setAlignment( Pos.CENTER )
            arrTxtDateTime[ i ]!!.setFont( curControlFont )
            arrTxtDateTime[ i ]!!.setOnKeyPressed( this )
        }

        butShowForTime = Button( "Показать" )
        butShowForTime.tooltip = Tooltip( "Показать график на заданный период" )
        butShowForTime.setOnAction( this )

        panTimeBar.children.addAll( Label( "Начало:" ), arrTxtDateTime[ 0 ], Label( "." ), arrTxtDateTime[ 1 ], Label( "." ), arrTxtDateTime[ 2 ], Label( " " ), arrTxtDateTime[ 3 ], Label( ":" ), arrTxtDateTime[ 4 ], Label( "Окончание:" ), arrTxtDateTime[ 5 ], Label( "." ), arrTxtDateTime[ 6 ], Label( "." ), arrTxtDateTime[ 7 ], Label( " " ), arrTxtDateTime[ 8 ], Label( ":" ), arrTxtDateTime[ 9 ], butShowForTime )

 */

    this.methods = json(
        "grRefreshView" to { aThat: dynamic, aView: GraphicViewCoord? ->
            val that = aThat ?: that()

            doGraphicRefresh(
                that = that,
                graphicResponse = graphicResponse,
                tabId = tabId,
                elementPrefix = "graphic",
                arrAddElements = emptyArray(),
                aView = aView,
            )
        },
        "doChangeGraphicVisibility" to {
            that().isShowGraphicVisibility = false
            val arrGraphicVisibleData = that().arrGraphicVisibleData.unsafeCast<Array<GraphicVisibleData>>()
            arrGraphicVisibleData.forEach { graphicVisibleData ->
                invokeSaveUserProperty(
                    SaveUserPropertyRequest(
                        name = graphicVisibleData.name,
                        value = graphicVisibleData.check.toString(),
                    )
                )
            }
            that().grRefreshView(null, null)
        },
        "onMouseOver" to { event: Event, graphicElement: SvgElement ->
            val mouseEvent = event as MouseEvent
            //val mouseX = mouseEvent.offsetX.toInt()
            val mouseY = mouseEvent.offsetY.toInt()

            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val arrViewBoxBody = getGraphicViewBoxBody(that())

            if (graphicElement is SvgLine) {
                val arrYData = that().arrYData.unsafeCast<Array<YData>>()
                val yData = arrYData[graphicElement.tooltip.toInt()]
                //--- именно в таком порядке, чтобы не нарваться на 0 при целочисленном делении
                //--- (yData.y1 - нижняя/большая координата, yData.y2 - верхняя/меньшая координата)
                var value = (yData.value2 - yData.value1) * (yData.y1 - (mouseY + arrViewBoxBody[1])) / (yData.y1 - yData.y2) + yData.value1
                if (yData.itReversedY) {
                    value = -value
                }
                val tooltipValue = getSplittedDouble(value, yData.prec, true, '.')

                val tooltipX = mouseEvent.clientX + (8 * scaleKoef).roundToInt()
                val tooltipY = mouseEvent.clientY + (0 * scaleKoef).roundToInt()

                that().grTooltipVisible = true
                that().grTooltipText = tooltipValue
                that().style_gr_tooltip_pos = json("left" to "${tooltipX}px", "top" to "${tooltipY}px")
                that().grTooltipOffTime = Date().getTime() + 3000
            } else if (graphicElement.tooltip.isNotEmpty()) {
                val tooltipX = mouseEvent.clientX + (8 * scaleKoef).roundToInt()
                val tooltipY = mouseEvent.clientY + (0 * scaleKoef).roundToInt()

                that().grTooltipVisible = true
                that().grTooltipText = graphicElement.tooltip.replace("\n", "<br>")
                that().style_gr_tooltip_pos = json("left" to "${tooltipX}px", "top" to "${tooltipY}px")
                that().grTooltipOffTime = Date().getTime() + 3000
            } else {
                that().grTooltipVisible = false
            }
        },
        "onMouseOut" to {
            //--- через 3 сек выключить тултип, если не было других активаций тултипов
            //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
            //--- причём после ухода с графика других mouseleave не вызывается.
            val that = that()
            window.setTimeout({
                val tooltipOffTime = that.grTooltipOffTime.unsafeCast<Double>()
                if (Date().getTime() > tooltipOffTime) {
                    that.grTooltipVisible = false
                }
            }, 3000)
        },
        "onMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            val timeOffset = that().`$root`.timeOffset.unsafeCast<Int>()
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().grViewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().grCurMode.unsafeCast<GraphicWorkMode>()

            val svgCoords = defineGraphicSvgCoords(tabId, "graphic", emptyArray()) //!!! в случае работы в сложной схеме могут поехать y-координаты

            if (isNeedOffsetCompensation) {
                mouseX -= svgCoords.bodyLeft
                mouseY -= svgCoords.bodyTop
            }

            //--- при нажатой кнопке мыши положение курсора не отслеживается
            disableCursorLinesAndLabels(that())

            when (curMode) {
                GraphicWorkMode.PAN -> {
                    that().panPointOldX = mouseX
                    that().panPointOldY = mouseY
                    that().panDX = 0
                }
                GraphicWorkMode.ZOOM_BOX -> {
                    //            case SELECT_FOR_PRINT:
                    val arrViewBoxBody = getGraphicViewBoxBody(that())
                    val arrTimeLabel = that().arrTimeLabel.unsafeCast<Array<TimeLabelData>>()

                    that().mouseRect = MouseRectData(
                        true, mouseX, arrViewBoxBody[1], mouseX,
                        arrViewBoxBody[1] + arrViewBoxBody[3] - scaleKoef.roundToInt(), max(1, scaleKoef.roundToInt())
                    )

                    setTimeLabel(timeOffset, viewCoord, svgCoords.bodyLeft, svgCoords.bodyWidth, mouseX, arrTimeLabel[1])
                    setTimeLabel(timeOffset, viewCoord, svgCoords.bodyLeft, svgCoords.bodyWidth, mouseX, arrTimeLabel[2])
                }
//                else  -> super.handle( event )
            }
            that().isMouseDown = true
        },
        "onMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            val that = that()

            val timeOffset = that.`$root`.timeOffset.unsafeCast<Int>()
            val viewCoord = that.grViewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that.grCurMode.unsafeCast<GraphicWorkMode>()
            val pixStartY = that.pixStartY.unsafeCast<Int>()

            val isMouseDown = that.isMouseDown.unsafeCast<Boolean>()
            val panPointOldX = that.panPointOldX.unsafeCast<Int>()
            val panPointOldY = that.panPointOldY.unsafeCast<Int>()
            val panDX = that.panDX.unsafeCast<Int>()

            val svgCoords = defineGraphicSvgCoords(tabId, "graphic", emptyArray()) //!!! в случае работы в сложной схеме могут поехать y-координаты

            if (isNeedOffsetCompensation) {
                mouseX -= svgCoords.bodyLeft
                mouseY -= svgCoords.bodyTop
            }

            //--- mouse dragged
            if (isMouseDown) {
                when (curMode) {
                    GraphicWorkMode.PAN -> {
                        var dx = mouseX - panPointOldX
                        var dy = mouseY - panPointOldY

                        //--- чтобы убрать раздражающую диагональную прокрутку, нормализуем dx и dy - выбираем только один из них
                        if (abs(dx) >= abs(dy)) {
                            dy = 0
                        } else {
                            dx = 0
                        }

                        val arrViewBoxAxis = getGraphicViewBoxAxis(that)
                        val arrViewBoxBody = getGraphicViewBoxBody(that)
                        val arrViewBoxLegend = getGraphicViewBoxLegend(that)

                        arrViewBoxBody[0] -= dx

                        if (dy > 0) {
                            listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                                arr[1] -= dy
                                if (arr[1] < 0) {
                                    arr[1] = 0
                                }
                            }
                        } else if (dy < 0 && pixStartY - arrViewBoxAxis[1] > svgCoords.bodyHeight) {
                            listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                                arr[1] -= dy
                            }
                        }

                        that.panPointOldX = mouseX
                        that.panPointOldY = mouseY
                        that.panDX = panDX + dx

                        setGraphicViewBoxAxis(that, intArrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                        setGraphicViewBoxBody(that, intArrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))
                        setGraphicViewBoxLegend(that, intArrayOf(arrViewBoxLegend[0], arrViewBoxLegend[1], arrViewBoxLegend[2], arrViewBoxLegend[3]))

                        setGraphicTextOffset(that, svgCoords.bodyLeft, svgCoords.bodyTop)
                    }
                    GraphicWorkMode.ZOOM_BOX -> {
                        //            case SELECT_FOR_PRINT:
                        val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()
                        val arrTimeLabel = that().arrTimeLabel.unsafeCast<Array<TimeLabelData>>()

                        if (mouseRect.isVisible && mouseX >= 0 && mouseX <= svgCoords.bodyWidth) {
                            mouseRect.x2 = mouseX
                            setTimeLabel(timeOffset, viewCoord, svgCoords.bodyLeft, svgCoords.bodyWidth, mouseX, arrTimeLabel[2])
                        }
                    }
                    //                else -> super.handle( event )
                }
            }
            //--- mouse moved
            else {
                when (curMode) {
                    GraphicWorkMode.PAN, GraphicWorkMode.ZOOM_BOX -> {
                        //                    case SELECT_FOR_PRINT:
                        if (mouseX in 0..svgCoords.bodyWidth) {
                            val arrViewBoxBody = getGraphicViewBoxBody(that())
                            val timeLine = that().grTimeLine.unsafeCast<LineData>()
                            val arrTimeLabel = that().arrTimeLabel.unsafeCast<Array<TimeLabelData>>()

                            timeLine.isVisible = true
                            timeLine.x1 = mouseX
                            timeLine.y1 = arrViewBoxBody[1]
                            timeLine.x2 = mouseX
                            timeLine.y2 = arrViewBoxBody[1] + arrViewBoxBody[3]

                            setTimeLabel(timeOffset, viewCoord, svgCoords.bodyLeft, svgCoords.bodyWidth, mouseX, arrTimeLabel[0])
                        } else disableCursorLinesAndLabels(that())
                    }
//                    else -> super.handle( event )
                }

            }
        },
        "onMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().grViewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().grCurMode.unsafeCast<GraphicWorkMode>()
            val panDX = that().panDX.unsafeCast<Int>()

            val that = that()

            val svgCoords = defineGraphicSvgCoords(tabId, "graphic", emptyArray()) //!!! в случае работы в сложной схеме могут поехать y-координаты

            when (curMode) {
                GraphicWorkMode.PAN -> {
                    //--- перезагружаем график, только если был горизонтальный сдвиг
                    if (abs(panDX) >= 1) {
                        //--- именно в этом порядке операндов, чтобы:
                        //--- не было всегда 0 из-за целочисленного деления panDX / svgBodyWidth
                        //--- и не было возможного переполнения из-за умножения viewCoord.width * panDX
                        val deltaT = getTimeFromX(-panDX, svgCoords.bodyWidth, 0, viewCoord.width)
                        viewCoord.moveRel(deltaT)
                        that().grRefreshView(null, viewCoord)
                    }
                    that.panPointOldX = 0
                    that.panPointOldY = 0
                    that.panDX = 0
                }

                GraphicWorkMode.ZOOM_BOX -> {
                    val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()
                    val arrTimeLabel = that().arrTimeLabel.unsafeCast<Array<TimeLabelData>>()

                    //            case SELECT_FOR_PRINT:
                    if (mouseRect.isVisible) {
                        mouseRect.isVisible = false
                        arrTimeLabel[1].isVisible = false
                        arrTimeLabel[2].isVisible = false

                        //--- если размер прямоугольника меньше 8 pix, то это видимо ошибка - игнорируем
                        if (abs(mouseRect.x2 - mouseRect.x1) >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt() &&
                            abs(mouseRect.y2 - mouseRect.y1) >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt()
                        ) {

                            //--- именно в этом порядке операндов, чтобы:
                            //--- не было всегда 0 из-за целочисленного деления min( mouseRect.x1, mouseRect.x2 ) / svgBodyWidth
                            //--- и не было возможного переполнения из-за умножения viewCoord.width * min( mouseRect.x1, mouseRect.x2 )
                            val newT1 = getTimeFromX(min(mouseRect.x1, mouseRect.x2), svgCoords.bodyWidth, viewCoord.t1, viewCoord.width)
                            val newT2 = getTimeFromX(max(mouseRect.x1, mouseRect.x2), svgCoords.bodyWidth, viewCoord.t1, viewCoord.width)
                            if (newT2 - newT1 >= MIN_SCALE_X) {
                                if (curMode == GraphicWorkMode.ZOOM_BOX) {
                                    that().grRefreshView(null, GraphicViewCoord(newT1, newT2))
                                } else {
                                    //!!! пока пусть будет сразу печать с текущими границами, без возможности их отдельного определения перед печатью ( а оно надо ли ? )
                                    //outRect = mouseRectangle.getBoundsReal(  null  );
                                    //outViewStage1();
                                }
                            }
                        }
                    }
                }
//                else -> super.handle( event )
            }

            that().isMouseDown = false
        },
        "onGrMouseWheel" to { event: Event ->
            val wheelEvent = event as WheelEvent
            val isCtrl = wheelEvent.ctrlKey
            val mouseX = wheelEvent.offsetX.toInt()
            val deltaY = wheelEvent.deltaY.toInt()

            val that = that()

            val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that.grViewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that.grCurMode.unsafeCast<GraphicWorkMode>()
            val pixStartY = that.pixStartY.unsafeCast<Int>()

            val isMouseDown = that.isMouseDown.unsafeCast<Boolean>()

            val svgCoords = defineGraphicSvgCoords(tabId, "graphic", emptyArray()) //!!! в случае работы в сложной схеме могут поехать y-координаты

            if (curMode == GraphicWorkMode.PAN && !isMouseDown || curMode == GraphicWorkMode.ZOOM_BOX && !isMouseDown) {
                //|| grControl.curMode == GraphicModel.WorkMode.SELECT_FOR_PRINT && grControl.selectorX1 < 0  ) {
                //--- масштабирование
                if (isCtrl) {
                    val t1 = viewCoord.t1
                    val t2 = viewCoord.t2
                    //--- вычисляем текущую координату курсора в реальных координатах
                    val curT = getTimeFromX(mouseX, svgCoords.bodyWidth, t1, viewCoord.width)

                    val newT1 = if (deltaY < 0) {
                        curT - (curT - t1) / 2
                    } else {
                        curT - (curT - t1) * 2
                    }
                    val newT2 = if (deltaY < 0) {
                        curT + (t2 - curT) / 2
                    } else {
                        curT + (t2 - curT) * 2
                    }

                    if (newT2 - newT1 in MIN_SCALE_X..MAX_SCALE_X) {
                        that().grRefreshView(null, GraphicViewCoord(newT1, newT2))
                    }
                }
                //--- вертикальная прокрутка
                else {
                    val arrViewBoxAxis = getGraphicViewBoxAxis(that())
                    val arrViewBoxBody = getGraphicViewBoxBody(that())
                    val arrViewBoxLegend = getGraphicViewBoxLegend(that)

                    val dy = (deltaY * scaleKoef).roundToInt()

                    if (dy < 0) {
                        listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                            arr[1] += dy
                            if (arr[1] < 0) {
                                arr[1] = 0
                            }
                        }
                    } else if (dy > 0 && pixStartY - arrViewBoxAxis[1] > svgCoords.bodyHeight) {
                        listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                            arr[1] += dy
                        }
                    }

                    setGraphicViewBoxAxis(that, intArrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                    setGraphicViewBoxBody(that, intArrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))
                    setGraphicViewBoxLegend(that, intArrayOf(arrViewBoxLegend[0], arrViewBoxLegend[1], arrViewBoxLegend[2], arrViewBoxLegend[3]))

                    setGraphicTextOffset(that, svgCoords.bodyLeft, svgCoords.bodyTop)
                }
            }
        },
        "setModePan" to {
//            when( newMode ) {
//                WorkMode.PAN      -> {
////                    stackPane.cursor = Cursor.MOVE
//                }
//                WorkMode.ZOOM_BOX -> {
////                    stackPane.cursor = Cursor.CROSSHAIR
//                }
//            }
            that().isPanButtonDisabled = true
            that().isZoomButtonDisabled = false

            that().grCurMode = GraphicWorkMode.PAN
        },
        "setModeZoomBox" to {
            that().isPanButtonDisabled = false
            that().isZoomButtonDisabled = true

            that().grCurMode = GraphicWorkMode.ZOOM_BOX
        },
        "zoomIn" to {
            val viewCoord = that().grViewCoord.unsafeCast<GraphicViewCoord>()

            val t1 = viewCoord.t1
            val t2 = viewCoord.t2
            val grWidth = viewCoord.width

            val newT1 = t1 + grWidth / 4
            val newT2 = t2 - grWidth / 4

            if (newT2 - newT1 >= MIN_SCALE_X) {
                that().grRefreshView(null, GraphicViewCoord(newT1, newT2))
            }

        },
        "zoomOut" to {
            val viewCoord = that().grViewCoord.unsafeCast<GraphicViewCoord>()

            val t1 = viewCoord.t1
            val t2 = viewCoord.t2
            val grWidth = viewCoord.width

            val newT1 = t1 - grWidth / 2
            val newT2 = t2 + grWidth / 2
            if (newT2 - newT1 <= MAX_SCALE_X) {
                that().grRefreshView(null, GraphicViewCoord(newT1, newT2))
            }
        },
    )
/*
                else if( comp == butShowForTime ) {
                    try {
                        val begTime = Arr_DateTime( appContainer.timeZone, intArrayOf(
                            Integer.parseInt( arrTxtDateTime[ 2 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 1 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 0 ]!!.text ),
                            Integer.parseInt( arrTxtDateTime[ 3 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 4 ]!!.text ), 0 ) )
                        val endTime = Arr_DateTime( appContainer.timeZone, intArrayOf(
                            Integer.parseInt( arrTxtDateTime[ 7 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 6 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 5 ]!!.text ),
                            Integer.parseInt( arrTxtDateTime[ 8 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 9 ]!!.text ), 0 ) )
                        val graphicWidth = endTime - begTime

                        if( graphicWidth < GraphicModel.MIN_SCALE_X ) showError( "Ошибка задания периода", "Слишком короткий период для показа графика" )
                        else if( graphicWidth > GraphicModel.MAX_SCALE_X ) showError( "Ошибка задания периода", "Слишком большой период для показа графика" )
                        else grRefreshView( GraphicViewCoord( begTime, endTime ), 0 )
                    }
                    catch( nfe: NumberFormatException ) {
                        showError( "Ошибка задания периода", "Неправильно заданы дата/время" )
                    }

                }

 */
    this.mounted = {
        that().`$root`.setTabInfo(tabId, graphicResponse.shortTitle, graphicResponse.fullTitle)
        that().fullTitle = graphicResponse.fullTitle

        doGraphicSpecificComponentMounted(
            that = that(),
            graphicResponse = graphicResponse,
            tabId = tabId,
            elementPrefix = "graphic",
            svgHeight = null,
            arrAddElements = emptyArray(),
        )
    }

    this.data = {
        json(
            "fullTitle" to "",

            "isPanButtonDisabled" to true,
            "isZoomButtonDisabled" to false,

            "isShowGraphicVisibility" to false,
            "arrGraphicVisibleData" to arrayOf<GraphicVisibleData>(),

            "isMouseDown" to false,

            "panPointOldX" to 0,
            "panPointOldY" to 0,
            "panDX" to 0,

            "mouseRect" to MouseRectData(false, 0, 0, 0, 0, 1),

            "arrTimeLabel" to arrayOf(TimeLabelData(), TimeLabelData(), TimeLabelData()),

            "style_header" to json(
                "border-top" to if (!styleIsNarrowScreen) {
                    "none"
                } else {
                    "1px solid $COLOR_BUTTON_BORDER"
                }
            ),
            "style_toolbar" to json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "space-between",
                "align-items" to "center",        // "baseline" ?
                "padding" to styleControlPadding(),
                "background" to COLOR_PANEL_BACK
            ),
            "style_toolbar_block" to json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "nowrap",
                "justify-content" to "center",
                "align-items" to "center"        // "baseline" ?
            ),
            "style_title" to json(
                "font-size" to styleControlTitleTextFontSize(),
                "padding" to styleControlTitlePadding()
            ),
            "style_icon_button" to json(
                "background" to COLOR_BUTTON_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleIconButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer"
            ),
            "style_visibility_list" to json(
                "z-index" to "2",   // popup menu must be above than table headers
                "position" to "absolute",
                "top" to styleGraphicVisibilityTop(),
                "width" to "auto",
                "max-width" to styleGraphicVisibilityMaxWidth(),
                "background" to COLOR_MENU_GROUP_BACK,
                "border" to "1px solid $COLOR_MENU_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleMenuFontSize(),
                "padding" to styleMenuStartPadding(),
                "overflow" to "auto",
                "cursor" to "pointer",
            ),
            "style_graphic_visibility_checkbox" to json(
                //                "padding" to styleMenuItemPadding_0(),
                //{ 'background-color' : ( $menuDataName.itHover? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_ITEM_BACK' ) },
                //{ 'text-decoration' : ( $menuDataName.url || $menuDataName.text ? '' : 'line-through' ) },
                //{ 'color' : ( $menuDataName.url || $menuDataName.text ? '$COLOR_TEXT' : '$COLOR_MENU_DELIMITER' ) }
            ),
            "style_graphic_visibility_button" to json(
                "background" to COLOR_BUTTON_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleFileNameButtonPadding(),
                "margin" to styleFileNameButtonMargin(),
                "cursor" to "pointer"
            ),
        ).add(
            getGraphicSpecificComponentData()
        )
    }
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun getGraphicElementTemplate(
    tabId: Int,
    withInteractive: Boolean = true,
) =
    """
        <div style="display: flex;"
        """ +

        if (withInteractive) {
            """
                v-on:mousewheel.stop.prevent="onGrMouseWheel( ${'$'}event )"
            """
        } else {
            ""
        } +

        """
        >

            <svg id="gr_svg_axis_$tabId"
                 v-bind:width="gr_svg_axis_width"
                 v-bind:height="gr_svg_height"
                 v-bind:viewBox="grViewBoxAxis"
                 style="flex-shrink: 0;"
            >
                <template v-for="element in arrGraphicElement">
                    <line v-for="axisLine in element.arrAxisYLine"
                          v-bind:x1="axisLine.x1"
                          v-bind:y1="axisLine.y1"
                          v-bind:x2="axisLine.x2"
                          v-bind:y2="axisLine.y2"
                          v-bind:stroke="axisLine.stroke"
                          v-bind:stroke-width="axisLine.width"
                          v-bind:stroke-dasharray="axisLine.dash"
                    />

                    <text v-for="axisText in element.arrAxisYText"
                          v-bind:x="axisText.x"
                          v-bind:y="axisText.y"
                          v-bind:fill="axisText.stroke"
                          v-bind:text-anchor="axisText.hAnchor"
                          v-bind:dominant-baseline="axisText.vAnchor"
                          v-bind:transform="axisText.transform"
                          v-bind:style="style_graphic_text"
                    >
                        {{ axisText.text }}
                    </text>
                </template>
            </svg>

            <svg id="gr_svg_body_$tabId" 
                 width="100%" 
                 v-bind:height="gr_svg_height" 
                 v-bind:viewBox="grViewBoxBody"
            """ +
        if (withInteractive) {
            """
                         v-on:mousedown.stop.prevent="onMousePressed( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
                         v-on:mousemove.stop.prevent="onMouseMove( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
                         v-on:mouseup.stop.prevent="onMouseReleased( false, ${'$'}event.offsetX, ${'$'}event.offsetY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
                         v-on:mousewheel.stop.prevent="onGrMouseWheel( ${'$'}event )"
                         v-on:touchstart.stop.prevent="onMousePressed( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
                         v-on:touchmove.stop.prevent="onMouseMove( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
                         v-on:touchend.stop.prevent="onMouseReleased( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
                    """
        } else {
            ""
        } +
        """
            >

                <template v-for="element in arrGraphicElement">                   
                    <text v-bind:x="element.title.x"
                          v-bind:y="element.title.y"
                          v-bind:fill="element.title.stroke"
                          v-bind:text-anchor="element.title.hAnchor"
                          v-bind:dominant-baseline="element.title.vAnchor"
                          v-bind:style="style_graphic_text"
                    >
                        {{ element.title.text }}
                    </text>

                    <rect v-for="graphicBack in element.arrGraphicBack"
                          v-bind:x="graphicBack.x"
                          v-bind:y="graphicBack.y"
                          v-bind:width="graphicBack.width"
                          v-bind:height="graphicBack.height"
                          v-bind:fill="graphicBack.fill"
                    />
                    
                    <line v-for="axisLine in element.arrAxisXLine"
                          v-bind:x1="axisLine.x1" 
                          v-bind:y1="axisLine.y1" 
                          v-bind:x2="axisLine.x2" 
                          v-bind:y2="axisLine.y2"
                          v-bind:stroke="axisLine.stroke" 
                          v-bind:stroke-width="axisLine.width" 
                          v-bind:stroke-dasharray="axisLine.dash" 
                    />

                    <text v-for="axisText in element.arrAxisXText"
                          v-bind:x="axisText.x"
                          v-bind:y="axisText.y"
                          v-bind:fill="axisText.stroke"
                          v-bind:text-anchor="axisText.hAnchor"
                          v-bind:dominant-baseline="axisText.vAnchor"
                          v-bind:style="style_graphic_text"
                    >
                        <tspan v-for="textLine in axisText.arrText"
                               v-bind:x="axisText.x"
                               v-bind:dy="textLine.dy"
                        >
                            {{textLine.text}}</tspan>    <!-- специально, чтобы не было лишних символов в конце строк -->
                    </text>

                    <circle v-for="graphicPoint in element.arrGraphicPoint"
                            v-bind:cx="graphicPoint.cx" 
                            v-bind:cy="graphicPoint.cy" 
                            v-bind:r="graphicPoint.radius" 
                            v-bind:fill="graphicPoint.fill"
            """ +
        if (withInteractive) {
            """
                            v-on:mouseenter="onMouseOver( ${'$'}event, graphicPoint )" 
                            v-on:mouseleave="onMouseOut()" 
                    """
        } else {
            ""
        } +
        """
                    />

                    <!-- текст теперь выводится по-другому, но может ещё пригодиться для других прямоугольников
                        <rect v-for="graphicText in element.arrGraphicText"
                              v-bind:x="graphicText.x"
                              v-bind:y="graphicText.y"
                              v-bind:width="graphicText.width"
                              v-bind:height="graphicText.height"
                              v-bind:stroke="graphicText.stroke"
                              v-bind:fill="graphicText.fill"
                              v-bind:strokeWidth="graphicText.strokeWidth"
                              v-bind:rx="graphicText.rx"
                              v-bind:ry="graphicText.ry"
            """ +
        if (withInteractive) {
            """
                              v-on:mouseenter="onMouseOver( ${'$'}event, graphicText )"
                              v-on:mouseleave="onMouseOut()"
                    """
        } else {
            ""
        } +
        """
                        />
                    -->
                    <line v-for="graphicLine in element.arrGraphicLine"
                          v-bind:x1="graphicLine.x1" v-bind:y1="graphicLine.y1" v-bind:x2="graphicLine.x2" v-bind:y2="graphicLine.y2"
                          v-bind:stroke="graphicLine.stroke" v-bind:stroke-width="graphicLine.width" v-bind:stroke-dasharray="graphicLine.dash"
            """ +
        if (withInteractive) {
            """
                          v-on:mouseenter="onMouseOver( ${'$'}event, graphicLine )" 
                          v-on:mouseleave="onMouseOut()" 
                    """
        } else {
            ""
        } +
        """
                    />

                </template>

                <!-- v-show в svg не работает -->

                <line v-if="grTimeLine.isVisible"
                      v-bind:x1="grTimeLine.x1" v-bind:y1="grTimeLine.y1" v-bind:x2="grTimeLine.x2" v-bind:y2="grTimeLine.y2"
                      v-bind:stroke-width="grTimeLine.width" stroke="$COLOR_GRAPHIC_TIME_LINE" />
                """ +

        if (withInteractive) {
            """
                        <template v-if="mouseRect.isVisible">
                            <rect v-bind:x="Math.min(mouseRect.x1, mouseRect.x2)" v-bind:y="mouseRect.y1"
                                  v-bind:width="Math.abs(mouseRect.x2 - mouseRect.x1)" v-bind:height="mouseRect.y2 - mouseRect.y1"
                                  fill="$COLOR_GRAPHIC_TIME_LINE" opacity="0.25"/>
                            <line v-bind:x1="mouseRect.x1" v-bind:y1="mouseRect.y1" v-bind:x2="mouseRect.x2" v-bind:y2="mouseRect.y1"
                                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_GRAPHIC_TIME_LINE" />
                            <line v-bind:x1="mouseRect.x2" v-bind:y1="mouseRect.y1" v-bind:x2="mouseRect.x2" v-bind:y2="mouseRect.y2"
                                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_GRAPHIC_TIME_LINE" />
                            <line v-bind:x1="mouseRect.x2" v-bind:y1="mouseRect.y2" v-bind:x2="mouseRect.x1" v-bind:y2="mouseRect.y2"
                                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_GRAPHIC_TIME_LINE" />
                            <line v-bind:x1="mouseRect.x1" v-bind:y1="mouseRect.y2" v-bind:x2="mouseRect.x1" v-bind:y2="mouseRect.y1"
                                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_GRAPHIC_TIME_LINE" />
                        </template>
                    """
        } else {
            ""
        } +

        """
            </svg>

            <svg id="gr_svg_legend_$tabId"
                 v-bind:width="gr_svg_legend_width"
                 v-bind:height="gr_svg_height"
                 v-bind:viewBox="grViewBoxLegend" 
                 style="flex-shrink: 0;"
            >
                <template v-for="element in arrGraphicElement">
                    <rect v-for="legendBack in element.arrLegendBack"
                          v-bind:x="legendBack.x"
                          v-bind:y="legendBack.y"
                          v-bind:width="legendBack.width"
                          v-bind:height="legendBack.height"
                          v-bind:stroke="legendBack.stroke"
                          v-bind:fill="legendBack.fill"
                          v-bind:rx="legendBack.rx"
                          v-bind:ry="legendBack.ry"
                    />

                    <text v-for="legendText in element.arrLegendText"
                          v-bind:x="legendText.x"
                          v-bind:y="legendText.y"
                          v-bind:fill="legendText.stroke"
                          v-bind:text-anchor="legendText.hAnchor"
                          v-bind:dominant-baseline="legendText.vAnchor"
                          v-bind:transform="legendText.transform"
                          v-bind:style="style_graphic_text"
                    >
                        {{ legendText.text }}
                    </text>

                </template>
            </svg>

            <template v-for="element in arrGraphicElement">
                <div v-for="graphicText in element.arrGraphicText"
                     v-show="graphicText.isVisible"
                     v-bind:style="[graphicText.pos, graphicText.style]"
            """ +
        if (withInteractive) {
            """
                     v-on:mouseenter="onMouseOver( ${'$'}event, graphicText )"
                     v-on:mouseleave="onMouseOut()"
                    """
        } else {
            ""
        } +
        """
                >
                    {{graphicText.text}}
                </div>
            </template>
            """ +

        if (withInteractive) {
            """
                <template v-for="element in arrTimeLabel">
                    <div v-show="element.isVisible"
                         v-bind:style="[element.pos, element.style]"
                         v-html="element.text"
                    >
                    </div>
                </template>
            """
        } else {
            ""
        } +
        """
            <div v-show="grTooltipVisible"
                 v-bind:style="[style_gr_tooltip_text, style_gr_tooltip_pos]"
                 v-html="grTooltipText"
            >
            </div>
        </div>
    """

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun doGraphicSpecificComponentMounted(
    that: dynamic,
    graphicResponse: GraphicResponse,
    tabId: Int,
    elementPrefix: String,
    svgHeight: Int?,
    arrAddElements: Array<Element>,
) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

    //--- принудительная установка полной высоты svg-элементов
    //--- (BUG: иначе высота либо равна 150px - если не указывать высоту,
    //--- либо равно width, если указать height="100%")
    val svgCoords = defineGraphicSvgCoords(tabId, elementPrefix, arrAddElements)
    //--- из всего svgCoords используется только svgCoords.bodyTop
    that.gr_svg_height = svgHeight ?: (window.innerHeight - svgCoords.bodyTop)

    that.style_graphic_text = json(
        "font-size" to "${1.0 * scaleKoef}rem"
    )

    that.`$root`.setWait(true)
    invokeGraphic(
        GraphicActionRequest(
            documentTypeName = graphicResponse.documentTypeName,
            action = GraphicAction.GET_COORDS,
            startParamId = graphicResponse.startParamId
        ),
        { graphicActionResponse: GraphicActionResponse ->

            val newViewCoord = GraphicViewCoord(graphicActionResponse.begTime!!, graphicActionResponse.endTime!!)
            that.grRefreshView(that, newViewCoord)
            that.`$root`.setWait(false) as Unit
        }
    )
}

fun doGraphicRefresh(
    that: dynamic,
    graphicResponse: GraphicResponse,
    tabId: Int,
    elementPrefix: String,
    arrAddElements: Array<Element>,
    aView: GraphicViewCoord?,
) {
    val newView =
        if (aView != null) {
            //--- обновляем, только если изменилось (оптимизируем цепочку реактивных изменений)
            that.grViewCoord = aView
            aView
        } else {
            that.grViewCoord.unsafeCast<GraphicViewCoord>()
        }

    val timeOffset = that.`$root`.timeOffset.unsafeCast<Int>()
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

    that.`$root`.setWait(true)
    invokeGraphic(
        GraphicActionRequest(
            documentTypeName = graphicResponse.documentTypeName,
            action = GraphicAction.GET_ELEMENTS,
            startParamId = graphicResponse.startParamId,
            graphicCoords = Pair(newView.t1, newView.t2),
            //--- передавая полную ширину окна (без учёта margin по бокам/сверху/снизу для отрисовки шкалы/полей/заголовков),
            //--- мы делаем сглаживание/масштабирование чуть точнее, чем надо, но на момент запроса величины margin неизвестны :/,
            //--- а от "лишней" точности хуже не будет
            viewSize = Pair((window.innerWidth / scaleKoef).roundToInt(), (window.innerHeight / scaleKoef).roundToInt())
        ),

        { graphicActionResponse: GraphicActionResponse ->

            val arrElement = graphicActionResponse.arrElement

            //--- пары element-descr -> element-key, отсортированные по element-descr для определения ключа,
            //--- по которому будет управляться видимость графиков
            that.arrGraphicVisibleData = graphicActionResponse.arrVisibleElement.map { triple ->
                GraphicVisibleData(
                    descr = triple.first,
                    name = triple.second,
                    check = triple.third,
                )
            }.toTypedArray()

            val hmIndexColor = graphicActionResponse.arrIndexColor.associate { e ->
                e.first.toString() to getColorFromInt(e.second)
            }
            that.arrGrLegend = graphicActionResponse.arrLegend.map { triple ->
                val color = triple.first
                val isBack = triple.second
                val text = triple.third

                LegendData(
                    text = text,
                    style = json(
                        "background" to if (isBack) {
                            getColorFromInt(color)
                        } else {
                            COLOR_BUTTON_BACK
                        },
                        "color" to if (isBack) {
                            COLOR_TEXT
                        } else {
                            getColorFromInt(color)
                        },
                        "font-size" to styleCommonButtonFontSize(),
                        "padding" to styleTextButtonPadding(),
                        "margin" to styleCommonMargin(),
                        //"cursor" to "none",
                        "border" to "1px solid ${getColorFromInt(color)}",
                        "border-radius" to "0.2rem",
                    )
                )
            }.toTypedArray()

            var maxMarginLeft = 0
            var maxMarginRight = 0

            //--- определить hard/soft-высоты графиков (для распределения области окна между графиками)
            var sumHard = 0        // сумма жестко заданных высот
            var sumSoft = 0        // сумма мягко/относительно заданных высот
            arrElement.forEach { pair ->
                val cge = pair.second
                //--- prerare data for Y-reversed charts
                cge.alAxisYData.forEach { axisYData ->
                    if (axisYData.itReversedY) {
                        //--- во избежание перекрёстных изменений
                        val minY = axisYData.min
                        val maxY = axisYData.max
                        axisYData.min = -maxY
                        axisYData.max = -minY
                    }
                }
                cge.alGDC.forEach { gdc ->
                    if (gdc.itReversedY) {
                        when (gdc.type.toString()) {
                            GraphicDataContainer.ElementType.LINE.toString() -> {
                                gdc.alGLD.forEach { gld ->
                                    gld.y = -gld.y
                                }
                            }
                        }
                    }
                }

                //--- переинициализировать значение левого поля
                maxMarginLeft = max(maxMarginLeft, (cge.alAxisYData.size * MARGIN_LEFT * scaleKoef).roundToInt())
                maxMarginRight = max(maxMarginRight, cge.alLegend.size * getLegendWidth(scaleKoef))

                val grHeight = cge.graphicHeight.toInt()
                if (grHeight > 0) {
                    //--- "положительная" высота - жестко заданная
                    sumHard += (grHeight * scaleKoef).roundToInt()
                } else {
                    //--- "отрицательная" высота - относительная (в долях от экрана)
                    sumSoft += -grHeight
                }
            }

            //--- установка динамической (зависящей от scaleKoef) ширины области с вертикальными осями
            that.gr_svg_axis_width = maxMarginLeft
            that.gr_svg_legend_width = maxMarginRight

            //--- сбрасываем горизонтальный скроллинг/смещение и устанавливаем размеры SVG-компонент
            val svgAxisElement = document.getElementById("gr_svg_axis_$tabId")!!
            val svgAxisWidth = maxMarginLeft
            val svgAxisHeight = svgAxisElement.clientHeight

            val arrViewBoxAxis = getGraphicViewBoxAxis(that)
            setGraphicViewBoxAxis(that, intArrayOf(0, arrViewBoxAxis[1], svgAxisWidth, svgAxisHeight))

            val svgBodyElement = document.getElementById("gr_svg_body_$tabId")!!
            val svgBodyWidth = window.innerWidth - (maxMarginLeft + maxMarginRight)
            val svgBodyHeight = svgBodyElement.clientHeight

            val arrViewBoxBody = getGraphicViewBoxBody(that)
            setGraphicViewBoxBody(that, intArrayOf(0, arrViewBoxBody[1], svgBodyWidth, svgBodyHeight))

            val svgLegendElement = document.getElementById("gr_svg_legend_$tabId")!!
            val svgLegendWidth = maxMarginRight
            val svgLegendHeight = svgLegendElement.clientHeight

            val arrViewBoxLegend = getGraphicViewBoxBody(that)
            setGraphicViewBoxLegend(that, intArrayOf(0, arrViewBoxLegend[1], svgLegendWidth, svgLegendHeight))

            //--- реальная высота одной единицы относительной высоты
            val oneSoftHeight = if (sumSoft == 0) {
                0
            } else {
                //--- только для svgCoords.bodyHeight, которое уже установлено
                val svgCoords = defineGraphicSvgCoords(tabId, elementPrefix, arrAddElements)
                (svgCoords.bodyHeight - sumHard) / sumSoft
            }

            var pixStartY = 0

            val alGraphicElement = mutableListOf<GraphicElementData>()
            val alYData = mutableListOf<YData>()
            arrElement.forEach { pair ->
                val element = pair.second

                val grHeight = element.graphicHeight.toInt()
                val pixRealHeight = if (grHeight > 0) {
                    (grHeight * scaleKoef).roundToInt()
                } else {
                    max((GRAPHIC_MIN_HEIGHT * scaleKoef).roundToInt(), -grHeight * oneSoftHeight)
                }
                outElement(
                    timeOffset = timeOffset,
                    scaleKoef = scaleKoef,
                    hmIndexColor = hmIndexColor,
                    svgAxisWidth = svgAxisWidth,
                    svgBodyWidth = svgBodyWidth,
                    svgLegendWidth = svgLegendWidth,
                    t1 = newView.t1,
                    t2 = newView.t2,
                    element = element,
                    pixRealHeight = pixRealHeight,
                    pixStartY = pixStartY,
                    alGraphicElement = alGraphicElement,
                    alYData = alYData
                )
                pixStartY += pixRealHeight
            }

//        //--- перезагрузка данных может быть связана с изменением показываемого временнОго диапазона,
//        //--- поэтому переотобразим его
//        val arrBegDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t1 )
//        arrTxtDateTime[ 2 ]!!.text = arrBegDT[ 0 ].toString()
//        arrTxtDateTime[ 1 ]!!.text = arrBegDT[ 1 ].toString()
//        arrTxtDateTime[ 0 ]!!.text = arrBegDT[ 2 ].toString()
//        arrTxtDateTime[ 3 ]!!.text = arrBegDT[ 3 ].toString()
//        arrTxtDateTime[ 4 ]!!.text = arrBegDT[ 4 ].toString()
//        val arrEndDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t2 )
//        arrTxtDateTime[ 7 ]!!.text = arrEndDT[ 0 ].toString()
//        arrTxtDateTime[ 6 ]!!.text = arrEndDT[ 1 ].toString()
//        arrTxtDateTime[ 5 ]!!.text = arrEndDT[ 2 ].toString()
//        arrTxtDateTime[ 8 ]!!.text = arrEndDT[ 3 ].toString()
//        arrTxtDateTime[ 9 ]!!.text = arrEndDT[ 4 ].toString()
//
//        onRequestFocus()

            that.pixStartY = pixStartY

            that.arrGraphicElement = alGraphicElement.map {
                GraphicElementData_(
                    title = it.title,
                    arrAxisYLine = it.alAxisYLine.toTypedArray(),
                    arrAxisYText = it.alAxisYText.toTypedArray(),
                    arrAxisXLine = it.alAxisXLine.toTypedArray(),
                    arrAxisXText = it.alAxisXText.toTypedArray(),
                    arrGraphicBack = it.alGraphicBack.toTypedArray(),
                    arrGraphicLine = it.alGraphicLine.toTypedArray(),
                    arrGraphicPoint = it.alGraphicPoint.toTypedArray(),
                    arrGraphicText = it.alGraphicText.toTypedArray(),
                    arrLegendBack = it.alLegendBack.toTypedArray(),
                    arrLegendText = it.alLegendText.toTypedArray(),
                )
            }.toTypedArray()

            val menuBarElement = document.getElementById(MENU_BAR_ID)
            val menuBarWidth = menuBarElement?.clientWidth ?: 0
            val bodyLeft = menuBarWidth + svgAxisWidth  //svgBodyElement.clientLeft - BUG: всегда даёт 0
            //--- только для svgCoords.bodyTop, которое уже установлено
            val svgCoords = defineGraphicSvgCoords(tabId, elementPrefix, arrAddElements)
            setGraphicTextOffset(that, bodyLeft, svgCoords.bodyTop)

            that.arrYData = alYData.toTypedArray()

            that.`$root`.setWait(false) as Unit
        }
    )
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun getGraphicSpecificComponentData() = json(
    "arrGrLegend" to emptyArray<LegendData>(),

    "gr_svg_axis_width" to 0,
    "gr_svg_legend_width" to 0,
    "gr_svg_height" to "100%",

    "grViewBoxAxis" to "0 0 1 1",
    "grViewBoxBody" to "0 0 1 1",
    "grViewBoxLegend" to "0 0 1 1",

    "arrGraphicElement" to arrayOf<GraphicElement>(),

    "grTooltipVisible" to false,
    "grTooltipText" to "",
    "grTooltipOffTime" to 0.0,

    "grTimeLine" to LineData(false, 0, 0, 0, 0, 1),

    "grViewCoord" to GraphicViewCoord(0, 0),
    "arrYData" to arrayOf<YData>(),
    "grCurMode" to GraphicWorkMode.PAN,
    "pixStartY" to 0,

    "style_graphic_text" to json(
    ),
    "style_gr_tooltip_text" to json(
        "position" to "absolute",
        "color" to COLOR_GRAPHIC_LABEL_TEXT,
        "background" to COLOR_GRAPHIC_LABEL_BACK,
        "border" to "1px solid $COLOR_GRAPHIC_LABEL_BORDER",
        "border-radius" to BORDER_RADIUS,
        "padding" to styleControlTooltipPadding(),
        "user-select" to if (styleIsNarrowScreen) {
            "none"
        } else {
            "auto"
        }
    ),
    "style_gr_tooltip_pos" to json(
        "left" to "",
        "top" to ""
    ),
)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class GraphicSvgCoords(
    val axisWidth: Int,
    val bodyLeft: Int,
    val bodyTop: Int,
    val bodyWidth: Int,
    val bodyHeight: Int,
    val legendWidth: Int,
)

private fun defineGraphicSvgCoords(
    tabId: Int,
    elementPrefix: String,
    arrAddElements: Array<Element>,
): GraphicSvgCoords {
    val menuBarElement = document.getElementById(MENU_BAR_ID)

    val svgTabPanel = document.getElementById("tab_panel")!!
    val svgGraphicTitle = document.getElementById("${elementPrefix}_title_$tabId")!!
    val svgGraphicToolbar = document.getElementById("${elementPrefix}_toolbar_$tabId")!!

    val svgAxisElement = document.getElementById("gr_svg_axis_$tabId")!!
    val svgBodyElement = document.getElementById("gr_svg_body_$tabId")!!
    val svgLegendElement = document.getElementById("gr_svg_legend_$tabId")!!

    val menuBarWidth = menuBarElement?.clientWidth ?: 0
    val svgAxisWidth = svgAxisElement.clientWidth

    return GraphicSvgCoords(
        axisWidth = svgAxisWidth,
        bodyLeft = menuBarWidth + svgAxisWidth,  //svgBodyElement.clientLeft - BUG: всегда даёт 0
        //--- svgBodyElement.clientTop - BUG: всегда даёт 0
        bodyTop = svgTabPanel.clientHeight + svgGraphicTitle.clientHeight + svgGraphicToolbar.clientHeight + arrAddElements.sumOf { it.clientHeight },
        bodyWidth = svgBodyElement.clientWidth,
        bodyHeight = svgBodyElement.clientHeight,
        legendWidth = svgLegendElement.clientWidth,
    )
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class GraphicVisibleData(
    val descr: String,
    val name: String,
    val check: Boolean,
)

private class LegendData(
    val text: String,
    val style: Json,
)

private class GraphicElementData(
    val title: SvgText,
    val alAxisYLine: MutableList<SvgLine>,
    val alAxisYText: MutableList<SvgText>,
    val alAxisXLine: MutableList<SvgLine>,
    val alAxisXText: MutableList<SvgMultiLineText>,
    val alGraphicBack: MutableList<SvgRect>,
    val alGraphicLine: MutableList<SvgLine>,
    val alGraphicPoint: MutableList<SvgCircle>,
    val alGraphicText: MutableList<GraphicTextData>,
    val alLegendBack: MutableList<SvgRect>,
    val alLegendText: MutableList<SvgText>,
)

private class GraphicElementData_(
    val title: SvgText,
    val arrAxisYLine: Array<SvgLine>,
    val arrAxisYText: Array<SvgText>,
    val arrAxisXLine: Array<SvgLine>,
    val arrAxisXText: Array<SvgMultiLineText>,
    val arrGraphicBack: Array<SvgRect>,
    val arrGraphicLine: Array<SvgLine>,
    val arrGraphicPoint: Array<SvgCircle>,
    val arrGraphicText: Array<GraphicTextData>,
    val arrLegendBack: Array<SvgRect>,
    val arrLegendText: Array<SvgText>,
)

private class LineData(
    var isVisible: Boolean,
    var x1: Int,
    var y1: Int,
    var x2: Int,
    var y2: Int,
    var width: Int
)

private class YData(
    val y1: Int,
    val y2: Int,
    val value1: Double,
    val value2: Double,
    val prec: Int,
    val itReversedY: Boolean
)

private class GraphicTextData(
    var isVisible: Boolean,
    val x: Int,
    val y: Int,
    var pos: Json,
    val style: Json,
    val text: String,
    tooltip: String
) : SvgElement(tooltip)

private class TimeLabelData(
    var isVisible: Boolean = false,
    var pos: Json = json("" to ""),
    val style: Json = json(
        "position" to "absolute",
        "text-align" to "center",
        "color" to COLOR_GRAPHIC_LABEL_TEXT,
        "background" to COLOR_GRAPHIC_LABEL_BACK,
        "border" to "1px solid $COLOR_GRAPHIC_LABEL_BORDER",
        "border-radius" to BORDER_RADIUS,
        "padding" to styleGraphicTimeLabelPadding(),
        "user-select" to if (styleIsNarrowScreen) "none" else "auto"
    ),
    var text: String = ""
)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private fun getGraphicViewBoxAxis(that: dynamic): IntArray {
    val sViewBox = that.grViewBoxAxis.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

private fun setGraphicViewBoxAxis(that: dynamic, arrViewBox: IntArray) {
    that.grViewBoxAxis = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
}

private fun getGraphicViewBoxBody(that: dynamic): IntArray {
    val sViewBox = that.grViewBoxBody.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

private fun setGraphicViewBoxBody(that: dynamic, arrViewBox: IntArray) {
    that.grViewBoxBody = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
}

private fun getGraphicViewBoxLegend(that: dynamic): IntArray {
    val sViewBox = that.grViewBoxLegend.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

private fun setGraphicViewBoxLegend(that: dynamic, arrViewBox: IntArray) {
    that.grViewBoxLegend = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
}

private fun setGraphicTextOffset(that: dynamic, svgBodyLeft: Int, svgBodyTop: Int) {
    val arrGraphicElement = that.arrGraphicElement.unsafeCast<Array<GraphicElementData_>>()
    val arrViewBoxBody = getGraphicViewBoxBody(that)

    for (grElement in arrGraphicElement) {
        for (grTextData in grElement.arrGraphicText) {
            val newX = grTextData.x - arrViewBoxBody[0]
            val newY = grTextData.y - arrViewBoxBody[1]

            grTextData.isVisible = newX >= 0 && newY >= 0 && newX < arrViewBoxBody[2] && newY < arrViewBoxBody[3]

            grTextData.pos = json(
                "left" to "${svgBodyLeft + newX}px",
                "top" to "${svgBodyTop + newY}px"
            )
        }
    }
}

private fun setTimeLabel(timeOffset: Int, viewCoord: GraphicViewCoord, svgBodyLeft: Int, svgBodyWidth: Int, x: Int, timeLabelData: TimeLabelData) {
    val cursorTime = getTimeFromX(x, svgBodyWidth, viewCoord.t1, viewCoord.width)

    timeLabelData.isVisible = true
    timeLabelData.text = DateTime_DMYHMS(timeOffset, cursorTime).replace(" ", "<br>")
    timeLabelData.pos = json(
        "bottom" to "0",
        (if (x > svgBodyWidth * 7 / 8) {
            "right"
        } else {
            "left"
        }) to (if (x > svgBodyWidth * 7 / 8) {
            "${svgBodyWidth - x}px"
        } else {
            "${svgBodyLeft + x}px"
        })
    )
}

//--- в double и обратно из-за ошибок округления
private fun getTimeFromX(pixX: Int, pixWidth: Int, timeStart: Int, timeWidth: Int): Int = (1.0 * timeWidth * pixX / pixWidth + timeStart).roundToInt()

private fun disableCursorLinesAndLabels(that: dynamic) {
    val timeLine = that.grTimeLine.unsafeCast<LineData>()
    val arrTimeLabel = that.arrTimeLabel.unsafeCast<Array<TimeLabelData>>()

    timeLine.isVisible = false
    arrTimeLabel.forEach {
        it.isVisible = false
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private fun outElement(
    timeOffset: Int,
    scaleKoef: Double,
    hmIndexColor: Map<String, String>,
    svgAxisWidth: Int,
    svgBodyWidth: Int,
    svgLegendWidth: Int,
    t1: Int,
    t2: Int,
    element: GraphicElement,
    pixRealHeight: Int,
    pixStartY: Int,
    alGraphicElement: MutableList<GraphicElementData>,
    alYData: MutableList<YData>
) {
    //--- maxMarginLeft уходит на левую панель, к оси Y
    val pixDrawHeight = pixRealHeight - ((MARGIN_TOP + MARGIN_BOTTOM) * scaleKoef).roundToInt()
    val pixDrawY0 = pixStartY + pixRealHeight - (MARGIN_BOTTOM * scaleKoef).roundToInt()   // "нулевая" ось Y
    val pixDrawTopY = pixStartY + (MARGIN_TOP * scaleKoef).roundToInt()  // верхний край графика

    val alAxisYLine = mutableListOf<SvgLine>()
    val alAxisYText = mutableListOf<SvgText>()
    val alAxisXLine = mutableListOf<SvgLine>()
    val alAxisXText = mutableListOf<SvgMultiLineText>()
    val alGraphicBack = mutableListOf<SvgRect>()
    val alGraphicLine = mutableListOf<SvgLine>()
    val alGraphicPoint = mutableListOf<SvgCircle>()
    val alGraphicText = mutableListOf<GraphicTextData>()
    val alLegendBack = mutableListOf<SvgRect>()
    val alLegendText = mutableListOf<SvgText>()

    //--- заголовок

    val titleData = SvgText(
        x = (MIN_GRID_STEP_X * scaleKoef).roundToInt(),
        y = (pixDrawTopY - 4 * scaleKoef).roundToInt(),
        text = element.graphicTitle,
        stroke = COLOR_GRAPHIC_TITLE,
        hAnchor = "start",
        vAnchor = "text-bottom"
    )

    //--- ось X ---

    drawTimePane(
        timeOffset = timeOffset,
        scaleKoef = scaleKoef,
        t1 = t1,
        t2 = t2,
        pixWidth = svgBodyWidth,
        pixDrawY0 = pixDrawY0,
        pixDrawTopY = pixDrawTopY,
        alAxisLine = alAxisXLine,
        alAxisText = alAxisXText
    )

    //--- оси Y

    val alAxisYDataIndex = mutableListOf<Int>()
    for (i in element.alAxisYData.indices) {
        val ayd = element.alAxisYData[i]

        val precY = drawAxisY(
            scaleKoef = scaleKoef,
            hmIndexColor = hmIndexColor,
            element = element,
            axisIndex = i,
            pixDrawWidth = svgAxisWidth,
            pixDrawHeight = pixDrawHeight,
            pixDrawY0 = pixDrawY0,
            pixDrawTopY = pixDrawTopY,
            pixBodyWidth = svgBodyWidth,
            alAxisLine = alAxisYLine,
            alAxisText = alAxisYText,
            alBodyLine = alAxisXLine    // для горизонтальной линии на самом графике
        )

        val axisYDataIndex = alYData.size
        alAxisYDataIndex.add(axisYDataIndex)

        alYData.add(
            YData(
                y1 = pixDrawY0,
                y2 = pixDrawTopY,
                value1 = ayd.min,
                value2 = ayd.max,
                prec = precY,
                itReversedY = ayd.itReversedY,
            )
        )
    }

    //--- легенда ---

    for (i in element.alLegend.indices) {
        drawLegend(
            scaleKoef = scaleKoef,
            hmIndexColor = hmIndexColor,
            element = element,
            legendIndex = i,
            pixDrawHeight = pixDrawHeight,
            pixDrawY0 = pixDrawY0,
            pixDrawTopY = pixDrawTopY,
            alLegendBack = alLegendBack,
            alLegendText = alLegendText,
        )
    }

    //--- графики ---

    val alPrevTextBounds = mutableListOf<XyRect>()

    //--- для преодоления целочисленного переполнения
    val svgBodyWidthDouble = svgBodyWidth.toDouble()

    for (cagdc in element.alGDC) {
        val axisYIndex = cagdc.axisYIndex

        when (cagdc.type.toString()) {
            GraphicDataContainer.ElementType.BACK.toString() -> {
                for (grd in cagdc.alGBD) {
                    val drawX1 = (svgBodyWidthDouble * (grd.x1 - t1) / (t2 - t1)).toInt()
                    val drawX2 = (svgBodyWidthDouble * (grd.x2 - t1) / (t2 - t1)).toInt()

                    alGraphicBack.add(
                        SvgRect(
                            x = drawX1,
                            y = pixDrawTopY,
                            width = drawX2 - drawX1,
                            height = pixDrawY0 - pixDrawTopY,
                            fill = getColorFromInt(grd.color),
                        )
                    )
                }
            }

            GraphicDataContainer.ElementType.LINE.toString() -> {
                var prevDrawX = -1
                var prevDrawY = -1.0
                var prevDrawColorIndex: GraphicColorIndex? = null
                val ayd = element.alAxisYData[axisYIndex]
                val graphicHeight = ayd.max - ayd.min

                for (gld in cagdc.alGLD) {
                    val drawX = (svgBodyWidthDouble * (gld.x - t1) / (t2 - t1)).toInt()
                    val drawY = pixDrawY0 - pixDrawHeight * (gld.y - ayd.min) / graphicHeight
                    prevDrawColorIndex?.let {
                        alGraphicLine.add(
                            SvgLine(
                                x1 = prevDrawX,
                                y1 = prevDrawY.toInt(),
                                x2 = drawX,
                                y2 = drawY.toInt(),
                                stroke = hmIndexColor[gld.colorIndex.toString()]!!,
                                width = (cagdc.lineWidth * scaleKoef).roundToInt(),
                                tooltip = alAxisYDataIndex[axisYIndex].toString()
                            )
                        )
                    }
                    prevDrawX = drawX
                    prevDrawY = drawY
                    prevDrawColorIndex = gld.colorIndex
                }
            }

            GraphicDataContainer.ElementType.TEXT.toString() -> {
                for (gtd in cagdc.alGTD) {
                    val drawX1 = svgBodyWidth * (gtd.textX1 - t1) / (t2 - t1)
                    val drawX2 = svgBodyWidth * (gtd.textX2 - t1) / (t2 - t1)
                    val drawWidth = drawX2 - drawX1
                    val drawHeight = (GRAPHIC_TEXT_HEIGHT * scaleKoef).roundToInt()

                    //--- смысла нет показывать коротенькие блоки
                    if (drawWidth <= (GRAPHIC_TEXT_MIN_VISIBLE_WIDTH * scaleKoef).roundToInt()) {
                        continue
                    }

                    val rect = XyRect(drawX1, pixDrawTopY, drawWidth, drawHeight)

                    //--- обеспечим отсутствие накладок текстов/прямоугольников
                    //--- ( многопроходной алгоритм, учитывающий "смену обстановки" после очередного сдвига )
                    while (true) {
                        var crossNotFound = true
                        for (otherRect in alPrevTextBounds) {
                            //System.out.println(  "Bounds = " + b  );
                            //--- если блок текста пересекается с кем-то предыдущим, опустимся ниже его
                            if (rect.isIntersects(otherRect)) {
                                rect.y += rect.height
                                crossNotFound = false
                                break
                            }
                        }
                        if (crossNotFound) break
                    }
                    //--- для следующих текстов
                    alPrevTextBounds.add(rect)
                    alGraphicText.add(
                        GraphicTextData(
                            isVisible = false,
                            x = rect.x,
                            y = rect.y,
                            pos = json(
                                "left" to "0px",
                                "top" to "0px"
                            ),
                            style = json(
                                "position" to "absolute",
                                "padding-top" to "0.0rem",      // иначе прямоугольники текста налезают друг на друга по вертикали
                                "padding-bottom" to "0.0rem",
                                "padding-left" to "0.0rem",     // иначе прямоугольники текста налезают друг на друга по горизонтали
                                "padding-right" to "0.0rem",
                                "overflow" to "hidden",
                                "width" to "${rect.width - 2 * scaleKoef}px",   // чтобы избежать некрасивого перекрытия прямоугольников
                                "height" to "${rect.height - 2 * scaleKoef}px",
                                "border-radius" to "${2 * scaleKoef}px",
                                "border" to "${1 * scaleKoef}px solid ${hmIndexColor[gtd.borderColorIndex.toString()]}",
                                "color" to hmIndexColor[gtd.textColorIndex.toString()],
                                "background" to hmIndexColor[gtd.fillColorIndex.toString()],
                                "font-size" to "${1.0 * scaleKoef}rem",
                                "user-select" to if (styleIsNarrowScreen) "none" else "auto"
                            ),
                            text = gtd.text,
                            tooltip = gtd.text
                        )
                    )
                }
            }
        }
    }

    alGraphicElement.add(
        GraphicElementData(
            title = titleData,
            alAxisYLine = alAxisYLine,
            alAxisYText = alAxisYText,
            alAxisXLine = alAxisXLine,
            alAxisXText = alAxisXText,
            alGraphicBack = alGraphicBack,
            alGraphicLine = alGraphicLine,
            alGraphicPoint = alGraphicPoint,
            alGraphicText = alGraphicText,
            alLegendBack = alLegendBack,
            alLegendText = alLegendText,
        )
    )
}

private fun drawTimePane(
    timeOffset: Int,
    scaleKoef: Double,

    t1: Int,
    t2: Int,

    pixWidth: Int,

    pixDrawY0: Int,
    pixDrawTopY: Int,

    alAxisLine: MutableList<SvgLine>,
    alAxisText: MutableList<SvgMultiLineText>
) {
    val timeWidth = t2 - t1

    //--- сетка, насечки, надписи по оси X
    val minStepX: Int = (timeWidth * MIN_GRID_STEP_X * scaleKoef / pixWidth).roundToInt()
    var notchStepX = 0   // шаг насечек
    var labelStepX = 0   // шаг подписей под насечками
    for (i in arrGridStepX.indices) {
        if (arrGridStepX[i] >= minStepX) {
            notchStepX = arrGridStepX[i]
            //--- подписи по шкале X делаются реже, чем насечки
            labelStepX = arrGridStepX[if (i == arrGridStepX.size - 1) i else i + 1]
            break
        }
    }
    //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
    if (notchStepX == 0) {
        notchStepX = arrGridStepX[arrGridStepX.size - 1]
        labelStepX = arrGridStepX[arrGridStepX.size - 1]
    }

    var notchX = (t1 + timeOffset) / notchStepX * notchStepX - timeOffset
    while (notchX <= t2) {

        if (notchX < t1) {
            notchX += notchStepX
            continue
        }
        //--- в double и обратно из-за ошибок округления
        val pixDrawX = (1.0 * pixWidth * (notchX - t1) / timeWidth).roundToInt()
        //--- вертикальная линия сетки, переходящая в насечку
        val line = SvgLine(
            x1 = pixDrawX,
            y1 = pixDrawTopY,
            x2 = pixDrawX,
            y2 = pixDrawY0 + (2 * scaleKoef).roundToInt(),
            stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
            width = max(1, scaleKoef.roundToInt()),
            //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
            dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
                "${scaleKoef * 2},${scaleKoef * 2}"
            } else {
                ""
            }
        )
        alAxisLine.add(line)

        //--- текст метки по оси X
        if ((notchX + timeOffset) % labelStepX == 0) {
            val alTextLine = DateTime_DMYHMS(timeOffset, notchX).split(' ')
            val alTextSpan = mutableListOf<SVGTextSpan>()
            for (i in alTextLine.indices) {
                alTextSpan.add(
                    SVGTextSpan(
                        dy = "${(if (i == 0) 0.2 else 1.0) * scaleKoef}rem",
                        text = alTextLine[i].trim()
                    )
                )
            }
            val axisText = SvgMultiLineText(
                x = pixDrawX,
                y = pixDrawY0 + (scaleKoef * 2).roundToInt(),
                arrText = alTextSpan.toTypedArray(),
                stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                hAnchor = "middle",
                vAnchor = "hanging"
            )

            alAxisText.add(axisText)
        }
        notchX += notchStepX
    }
    //--- первую метку перевыровнять к началу, последнюю - к концу
    alAxisText.first().hAnchor = "start"
    alAxisText.last().hAnchor = "end"

    //--- ось X
    val line = SvgLine(
        x1 = 0,
        y1 = pixDrawY0,
        x2 = pixWidth,
        y2 = pixDrawY0,
        stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
        width = max(1, scaleKoef.roundToInt()),
        //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
        dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
            "${scaleKoef * 2},${scaleKoef * 2}"
        } else {
            ""
        }
    )
    alAxisLine.add(line)
}

private fun drawAxisY(
    scaleKoef: Double,
    hmIndexColor: Map<String, String>,
    element: GraphicElement,
    axisIndex: Int,
    pixDrawWidth: Int,
    pixDrawHeight: Int,
    pixDrawY0: Int,
    pixDrawTopY: Int,
    pixBodyWidth: Int,      // ширина основного графика
    alAxisLine: MutableList<SvgLine>,
    alAxisText: MutableList<SvgText>,
    alBodyLine: MutableList<SvgLine>
): Int {

    val ayd = element.alAxisYData[axisIndex]
    val grHeight = ayd.max - ayd.min
    val axisX = (pixDrawWidth - axisIndex * MARGIN_LEFT * scaleKoef).roundToInt()

    //--- сетка, насечки, надписи по оси Y
    val minGraphicStepY = grHeight * MIN_GRID_STEP_Y * scaleKoef / pixDrawHeight
    var notchGraphicStepY = 0.0   // шаг насечек
    var labelGraphicStepY = 0.0   // шаг подписей под насечками
    var precY = 0
    for (i in arrGridStepY.indices) {
        val gridStepY = arrGridStepY[i]
        if (gridStepY >= minGraphicStepY) {
            notchGraphicStepY = gridStepY
            //--- подписи по шкале Y делаются на каждой насечке
            labelGraphicStepY = gridStepY  //element.getGridStepX(  i == element.getGridStepXCount() - 1 ? i : i + 1  );
            //--- сразу же определим precY
            precY = arrPrecY[i]
            break
        }
    }
    //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
    if (notchGraphicStepY <= 0.0) {
        notchGraphicStepY = arrGridStepY.last()
        labelGraphicStepY = arrGridStepY.last()
        //--- сразу же определим precY
        precY = arrPrecY.last()
    }
    //--- для последующего корректного вычисления остатка от деления дробных чисел будем приводить их к целым числам путём умножения на квадрат минимального шага
    val mult = round(1.0 / arrGridStepY[0] / arrGridStepY[0])

    var notchY = floor(ayd.min / notchGraphicStepY) * notchGraphicStepY
    while (notchY <= ayd.max) {

        if (notchY < ayd.min) {
            notchY += notchGraphicStepY
            continue
        }
        val drawY = pixDrawY0 - pixDrawHeight * (notchY - ayd.min) / grHeight

        //--- горизонтальная линия сетки, переходящая в насечку
        val line = SvgLine(
            x1 = axisX - (MARGIN_LEFT * scaleKoef / 2).roundToInt(),
            y1 = drawY.toInt(),
            x2 = /*if( axisIndex == 0 ) pixDrawWidth else*/ axisX,
            y2 = drawY.toInt(),
            stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
            width = max(1, scaleKoef.roundToInt()),
            //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
            dash = "${scaleKoef * 2},${scaleKoef * 2}"
        )
        alAxisLine.add(line)

        //--- горизонтальная линия сетки на основном графике
        if (axisIndex == 0) {
            alBodyLine.add(
                SvgLine(
                    x1 = 0,
                    y1 = drawY.toInt(),
                    x2 = pixBodyWidth,
                    y2 = drawY.toInt(),
                    stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                    width = max(1, scaleKoef.roundToInt()),
                    //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                    dash = "${scaleKoef * 2},${scaleKoef * 2}"
                )
            )
        }

        //--- текст метки по оси Y
        if (round(notchY * mult).toLong() % round(labelGraphicStepY * mult).toLong() == 0L) {
            val value = if (ayd.itReversedY) {
                -notchY
            } else {
                notchY
            }
            val axisText = SvgText(
                x = axisX - (2 * scaleKoef).roundToInt(),
                y = drawY.toInt() - (2 * scaleKoef).roundToInt(),
                text = getSplittedDouble(value, precY, true, '.'),
                stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
                hAnchor = "end",
                vAnchor = "text-bottom"
            )
            alAxisText.add(axisText)
        }
        notchY += notchGraphicStepY
    }
    //--- ось Y
    val line = SvgLine(
        x1 = axisX,
        y1 = pixDrawY0,
        x2 = axisX,
        y2 = pixDrawTopY,
        stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
        width = max(1, scaleKoef.roundToInt())
    )
    alAxisLine.add(line)

    //--- подпись оси Y - подпись отодвинем подальше от цифр, чтобы не перекрывались
    val axisTextX = axisX - (MARGIN_LEFT * scaleKoef * 5 / 6).roundToInt()
    val axisTextY = pixDrawY0 - pixDrawHeight / 2
    val axisText = SvgText(
        x = axisTextX,
        y = axisTextY,
        text = ayd.title,
        stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
        hAnchor = "middle",
        vAnchor = "hanging",
        transform = "rotate(-90 $axisTextX $axisTextY)"
    )

    alAxisText.add(axisText)

    return precY
}

private fun drawLegend(
    scaleKoef: Double,
    hmIndexColor: Map<String, String>,
    element: GraphicElement,
    legendIndex: Int,
    pixDrawHeight: Int,
    pixDrawY0: Int,
    pixDrawTopY: Int,
    alLegendBack: MutableList<SvgRect>,
    alLegendText: MutableList<SvgText>,
) {
    val triple = element.alLegend[legendIndex]
    val color = triple.first
    val isBack = triple.second
    val text = triple.third

    val width = getLegendWidth(scaleKoef)

    val x1 = width * legendIndex
    val y1 = pixDrawTopY
    val x2 = x1 + width
    val y2 = pixDrawY0

    val legendBack = SvgRect(
        x = x1,
        y = y1,
        width = width,
        height = pixDrawHeight,
        stroke = getColorFromInt(color),
        fill = if (isBack) {
            getColorFromInt(color)
        } else {
            "none"
        },
        rx = "2",
        ry = "2",
        //tooltip: String = "" - на случай, если будем выводить только прямоугольники без текста (для мобильной версии, например)
    )
    alLegendBack += legendBack

    val textX = x1 + (LEGEND_TEXT_MARGIN * scaleKoef).toInt()
    val textY = y2 - (LEGEND_TEXT_MARGIN * scaleKoef).toInt()
    val legendText = SvgText(
        x = textX,
        y = textY,
        text = text,
        stroke = if (isBack) {
            COLOR_TEXT
        } else {
            getColorFromInt(color)
        },
        hAnchor = "start",
        vAnchor = "hanging",
        transform = "rotate(-90 $textX $textY)"
    )
    alLegendText += legendText
}

private fun getLegendWidth(scaleKoef: Double) = (iCoreAppContainer.BASE_FONT_SIZE * scaleKoef + 2 * LEGEND_TEXT_MARGIN * scaleKoef).toInt()