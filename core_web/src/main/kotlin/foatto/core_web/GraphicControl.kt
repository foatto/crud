package foatto.core_web

import foatto.core.app.UP_GRAPHIC_SHOW_BACK
import foatto.core.app.UP_GRAPHIC_SHOW_LINE
import foatto.core.app.UP_GRAPHIC_SHOW_POINT
import foatto.core.app.UP_GRAPHIC_SHOW_TEXT
import foatto.core.app.graphic.GraphicAction
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicElement
import foatto.core.app.graphic.GraphicViewCoord
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.GraphicResponse
import foatto.core.link.SaveUserPropertyRequest
import foatto.core.util.getSplittedDouble
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.window
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

private const val MARGIN_LEFT = 100     // на каждую ось Y

//private val MARGIN_RIGHT = 40
private const val MARGIN_TOP = 40
private const val MARGIN_BOTTOM = 60

private const val GRAPHIC_MIN_HEIGHT = 300

private const val MIN_GRID_STEP_X = 40  // минимальный шаг между линиями сетки в пикселях
private const val MIN_GRID_STEP_Y = 40  // минимальный шаг между линиями сетки в пикселях

private const val MIN_SCALE_X = 15 * 60     // минимальный разрешённый масштаб - диапазон не менее 15 мин
private const val MAX_SCALE_X = 32 * 86400  // максимальный разрешённый масштаб - диапазон не более 32 дней (чуть более месяца)

private const val GRAPHIC_TEXT_HEIGHT = 20              // высота текстового блока
private const val GRAPHIC_TEXT_MIN_VISIBLE_WIDTH = 4    // минимальная ширина видимого текстового блока

private val arrGridStepX = arrayOf(
    1, 5, 15,                           // 1 - 5 - 15 сек
    1 * 60, 5 * 60, 15 * 60,            // 1 - 5 - 15 мин
    1 * 3_600, 3 * 3_600, 6 * 3_600,    // 1 - 3 - 6 час
    1 * 86_400, 3 * 86_400, 9 * 86_400, // 1 - 3 - 9 суток
    27 * 86_400, 81 * 86_400
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
    1_000_000.0
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
    0
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
    """ +

        if (!styleIsNarrowScreen) {
            """
                <span v-bind:style="style_toolbar_block">
                    <input v-bind:style="style_htp_checkbox"
                           v-on:click="setShowBack()"
                           v-model="isShowBack"
                           title="Включить/выключить отображение фона графиков"
                           type="checkbox"
                    >
                    </input>
                    <span v-bind:style="style_htp_checkbox_label">
                        Фон
                    </span>
                    <input v-bind:style="style_htp_checkbox"
                           v-on:click="setShowPoint()"
                           v-model="isShowPoint"
                           title="Включить/выключить отображение точек точных значений"
                           type="checkbox"
                    >
                    </input>
                    <span v-bind:style="style_htp_checkbox_label">
                        Точки
                    </span>
                    <input v-bind:style="style_htp_checkbox"
                           v-on:click="setShowLine()"
                           v-model="isShowLine"
                           title="Включить/выключить отображение линий графиков"
                           type="checkbox"
                    >
                    </input>
                    <span v-bind:style="style_htp_checkbox_label">
                        Графики
                    </span>
                    <input v-bind:style="style_htp_checkbox"
                           v-on:click="setShowText()"
                           v-model="isShowText"
                           title="Включить/выключить отображение состояний"
                           type="checkbox"
                    >
                    </input>
                    <span v-bind:style="style_htp_checkbox_label">
                        Состояния
                    </span>
                </span>
            """
        } else {
            """

            """
        } +

        """
                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_sync_black_48dp.png"
                         v-bind:style="style_icon_button"
                         title="Обновить"
                         v-on:click="refreshView( null, null )"
                    >
                </span>
            </div>

            <div style="display: flex;"
                 v-on:mousewheel.stop.prevent="onMouseWheel( ${'$'}event )">

                <svg id="svg_axis_$tabId"
                     v-bind:width="svg_axis_width"
                     v-bind:height="svg_height"
                     v-bind:viewBox="viewBoxAxis"
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
                              v-bind:text-anchor="axisText.hAlign"
                              v-bind:dominant-baseline="axisText.vAlign"
                              v-bind:transform="axisText.transform"
                              v-bind:style="style_graphic_text"
                        >
                            {{ axisText.text }}
                        </text>

                    </template>
                </svg>

                <svg id="svg_body_$tabId" width="100%" v-bind:height="svg_height" v-bind:viewBox="viewBoxBody"
                     v-on:mousedown.stop.prevent="onMousePressed( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
                     v-on:mousemove.stop.prevent="onMouseMove( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
                     v-on:mouseup.stop.prevent="onMouseReleased( false, ${'$'}event.offsetX, ${'$'}event.offsetY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
                     v-on:mousewheel.stop.prevent="onMouseWheel( ${'$'}event )"
                     v-on:touchstart.stop.prevent="onMousePressed( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
                     v-on:touchmove.stop.prevent="onMouseMove( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
                     v-on:touchend.stop.prevent="onMouseReleased( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
                >

                    <template v-for="element in arrGraphicElement">                   
                        <text v-bind:x="element.title.x"
                              v-bind:y="element.title.y"
                              v-bind:fill="element.title.stroke"
                              v-bind:text-anchor="element.title.hAlign"
                              v-bind:dominant-baseline="element.title.vAlign"
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
                              v-bind:x1="axisLine.x1" v-bind:y1="axisLine.y1" v-bind:x2="axisLine.x2" v-bind:y2="axisLine.y2"
                              v-bind:stroke="axisLine.stroke" v-bind:stroke-width="axisLine.width" v-bind:stroke-dasharray="axisLine.dash" />

                        <text v-for="axisText in element.arrAxisXText"
                              v-bind:x="axisText.x"
                              v-bind:y="axisText.y"
                              v-bind:fill="axisText.stroke"
                              v-bind:text-anchor="axisText.hAlign"
                              v-bind:dominant-baseline="axisText.vAlign"
                              v-bind:style="style_graphic_text"
                        >
                            <tspan v-for="textLine in axisText.arrText"
                                   v-bind:x="axisText.x"
                                   v-bind:dy="textLine.dy">
                                {{textLine.text}}</tspan>    <!-- специально, чтобы не было лишних символов в конце строк -->
                        </text>

                        <circle v-for="graphicPoint in element.arrGraphicPoint"
                                v-bind:cx="graphicPoint.cx" v-bind:cy="graphicPoint.cy" v-bind:r="graphicPoint.radius" v-bind:fill="graphicPoint.fill"
                                v-on:mouseenter="onMouseOver( ${'$'}event, graphicPoint )" v-on:mouseleave="onMouseOut()" />

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
                              v-on:mouseenter="onMouseOver( ${'$'}event, graphicText )"
                              v-on:mouseleave="onMouseOut()"
                        />
-->
                        <line v-for="graphicLine in element.arrGraphicLine"
                              v-bind:x1="graphicLine.x1" v-bind:y1="graphicLine.y1" v-bind:x2="graphicLine.x2" v-bind:y2="graphicLine.y2"
                              v-bind:stroke="graphicLine.stroke" v-bind:stroke-width="graphicLine.width" v-bind:stroke-dasharray="graphicLine.dash"
                              v-on:mouseenter="onMouseOver( ${'$'}event, graphicLine )" v-on:mouseleave="onMouseOut()" />

                    </template>

                    <!-- v-show в svg не работает -->

                    <line v-if="timeLine.isVisible"
                          v-bind:x1="timeLine.x1" v-bind:y1="timeLine.y1" v-bind:x2="timeLine.x2" v-bind:y2="timeLine.y2"
                          v-bind:stroke-width="timeLine.width" stroke="$COLOR_GRAPHIC_TIME_LINE" />

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

                </svg>

                <template v-for="element in arrGraphicElement">
                    <div v-for="graphicText in element.arrGraphicText"
                         v-show="graphicText.isVisible"
                         v-bind:style="[graphicText.pos, graphicText.style]"
                         v-on:mouseenter="onMouseOver( ${'$'}event, graphicText )"
                         v-on:mouseleave="onMouseOut()"
                    >
                        {{graphicText.text}}
                    </div>
                </template>
                
                <template v-for="element in arrTimeLabel">
                    <div v-show="element.isVisible"
                         v-bind:style="[element.pos, element.style]"
                         v-html="element.text"
                    >
                    </div>
                </template>

                <div v-show="tooltipVisible"
                     v-bind:style="[style_tooltip_text, style_tooltip_pos]"
                     v-html="tooltipText"
                >
                </div>
            </div>
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
        //--- метод может вызываться из лямбд, поэтому возможен проброс ему "истинного" this
        "refreshView" to { aThat: dynamic, aView: GraphicViewCoord? ->
            val that = aThat ?: that()

            val newView =
                if (aView != null) {
                    //--- обновляем, только если изменилось (оптимизируем цепочку реактивных изменений)
                    that.viewCoord = aView
                    aView
                } else {
                    that.viewCoord.unsafeCast<GraphicViewCoord>()
                }

            val timeOffset = that.`$root`.timeOffset.unsafeCast<Int>()
            val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

            val svgCoords = defineGraphicSvgCoords(tabId)

            that.`$root`.setWait(true)
            invokeGraphic(
                GraphicActionRequest(
                    documentTypeName = graphicResponse.documentTypeName,
                    action = GraphicAction.GET_ELEMENTS,
                    startParamId = graphicResponse.startParamId,
                    graphicCoords = Pair(newView.t1, newView.t2),
                    //--- передавая полную ширину компоненты (без учёта margin по бокам/сверху/снизу для отрисовки шкалы/полей/заголовков),
                    //--- мы делаем сглаживание/масштабирование чуть точнее, чем надо, но на момент запроса величины margin неизвестны :/,
                    //--- а от "лишней" точности хуже не будет
                    viewSize = Pair((svgCoords.bodyWidth / scaleKoef).roundToInt(), (svgCoords.bodyHeight / scaleKoef).roundToInt())
                ),

                { graphicActionResponse: GraphicActionResponse ->

                    val alElement = graphicActionResponse.alElement!!
                    //--- пары element-descr -> element-key, отсортированные по element-descr для определения ключа,
                    //--- по которому будет управляться видимость графиков
                    val alVisibleElement = graphicActionResponse.alVisibleElement!!

                    val hmIndexColor = mutableMapOf<String, MutableMap<String, String>>()

                    var maxMarginLeft = (MARGIN_LEFT * scaleKoef).roundToInt()
                    //--- определить hard/soft-высоты графиков (для распределения области окна между графиками)
                    var sumHard = 0        // сумма жестко заданных высот
                    var sumSoft = 0        // сумма мягко/относительно заданных высот
                    alElement.forEach { pair ->
                        val key = pair.first
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
                                    GraphicDataContainer.ElementType.POINT.toString() -> {
                                        gdc.alGPD.forEach { gpd ->
                                            gpd.y = -gpd.y
                                        }
                                    }
                                }
                            }
                        }

                        hmIndexColor[key] = cge.alIndexColor.associate { e ->
                            e.first.toString() to getColorFromInt(e.second)
                        }.toMutableMap()

                        //--- переинициализировать значение левого поля
                        maxMarginLeft = max(maxMarginLeft, (cge.alAxisYData.size * MARGIN_LEFT * scaleKoef).roundToInt())

                        val grHeight = cge.graphicHeight.toInt()
                        if (grHeight > 0) {
                            //--- "положительная" высота - жестко заданная
                            sumHard += (grHeight * scaleKoef).roundToInt()
                        } else {
                            //--- "отрицательная" высота - относительная ( в долях от экрана )
                            sumSoft += -grHeight
                        }
                    }
                    //--- реальная высота одной единицы относительной высоты
                    val oneSoftHeight = if (sumSoft == 0) {
                        0
                    } else {
                        (svgCoords.bodyHeight - sumHard) / sumSoft
                    }

                    var pixStartY = 0

                    //!!! установить ширину svg_axis в зависимости от maxMarginLeft

                    val alGraphicElement = mutableListOf<GraphicElementData>()
                    val alYData = mutableListOf<YData>()
                    for (e in alElement) {
                        val element = e.second

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
                            svgAxisWidth = svgCoords.axisWidth,
                            svgBodyWidth = svgCoords.bodyWidth,
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

                    //--- сбрасываем горизонтальный скроллинг/смещение
                    val arrViewBoxBody = getGraphicViewBoxBody(that)
                    setGraphicViewBoxBody(that, intArrayOf(0, arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))

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
                            arrGraphicText = it.alGraphicText.toTypedArray()
                        )
                    }
                        .toTypedArray()

                    setGraphicTextOffset(that, svgCoords.bodyLeft, svgCoords.bodyTop)

                    that.arrYData = alYData.toTypedArray()

                    that.`$root`.setWait(false)
                }
            )
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

                that().tooltipVisible = true
                that().tooltipText = tooltipValue
                that().style_tooltip_pos = json("left" to "${tooltipX}px", "top" to "${tooltipY}px")
                that().tooltipOffTime = Date().getTime() + 3000
            } else if (graphicElement.tooltip.isNotEmpty()) {
                val tooltipX = mouseEvent.clientX + (8 * scaleKoef).roundToInt()
                val tooltipY = mouseEvent.clientY + (0 * scaleKoef).roundToInt()

                that().tooltipVisible = true
                that().tooltipText = graphicElement.tooltip.replace("\n", "<br>")
                that().style_tooltip_pos = json("left" to "${tooltipX}px", "top" to "${tooltipY}px")
                that().tooltipOffTime = Date().getTime() + 3000
            } else {
                that().tooltipVisible = false
            }
        },
        "onMouseOut" to {
            //--- через 3 сек выключить тултип, если не было других активаций тултипов
            //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
            //--- причём после ухода с графика других mouseleave не вызывается.
            val that = that()
            window.setTimeout({
                val tooltipOffTime = that.tooltipOffTime.unsafeCast<Double>()
                if (Date().getTime() > tooltipOffTime) {
                    that.tooltipVisible = false
                }
            }, 3000)
        },
        "onMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            val timeOffset = that().`$root`.timeOffset.unsafeCast<Int>()
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().curMode.unsafeCast<GraphicWorkMode>()

            val svgCoords = defineGraphicSvgCoords(tabId)

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

            val timeOffset = that().`$root`.timeOffset.unsafeCast<Int>()
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().curMode.unsafeCast<GraphicWorkMode>()
            val pixStartY = that().pixStartY.unsafeCast<Int>()

            val isMouseDown = that().isMouseDown.unsafeCast<Boolean>()
            val panPointOldX = that().panPointOldX.unsafeCast<Int>()
            val panPointOldY = that().panPointOldY.unsafeCast<Int>()
            val panDX = that().panDX.unsafeCast<Int>()

            val svgCoords = defineGraphicSvgCoords(tabId)

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
                        if (abs(dx) >= abs(dy)) dy = 0 else dx = 0

                        val arrViewBoxAxis = getGraphicViewBoxAxis(that())
                        val arrViewBoxBody = getGraphicViewBoxBody(that())

                        arrViewBoxBody[0] -= dx

                        if (dy > 0) {
                            arrViewBoxAxis[1] -= dy
                            if (arrViewBoxAxis[1] < 0) arrViewBoxAxis[1] = 0

                            arrViewBoxBody[1] -= dy
                            if (arrViewBoxBody[1] < 0) arrViewBoxBody[1] = 0
                        } else if (dy < 0 && pixStartY - arrViewBoxAxis[1] > svgCoords.bodyHeight) {
                            arrViewBoxAxis[1] -= dy

                            arrViewBoxBody[1] -= dy
                        }

                        that().panPointOldX = mouseX
                        that().panPointOldY = mouseY
                        that().panDX = panDX + dx

                        setGraphicViewBoxAxis(that(), intArrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                        setGraphicViewBoxBody(that(), intArrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))

                        setGraphicTextOffset(that(), svgCoords.bodyLeft, svgCoords.bodyTop)
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
                            val timeLine = that().timeLine.unsafeCast<LineData>()
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
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().curMode.unsafeCast<GraphicWorkMode>()
            val panDX = that().panDX.unsafeCast<Int>()

            val svgCoords = defineGraphicSvgCoords(tabId)

            when (curMode) {
                GraphicWorkMode.PAN -> {
                    //--- перезагружаем график, только если был горизонтальный сдвиг
                    if (abs(panDX) >= 1) {
                        //--- именно в этом порядке операндов, чтобы:
                        //--- не было всегда 0 из-за целочисленного деления panDX / svgBodyWidth
                        //--- и не было возможного переполнения из-за умножения viewCoord.width * panDX
                        val deltaT = getTimeFromX(-panDX, svgCoords.bodyWidth, 0, viewCoord.width)
                        viewCoord.moveRel(deltaT)
                        that().refreshView(null, viewCoord)
                    }
                    that().panPointOldX = 0
                    that().panPointOldY = 0
                    that().panDX = 0
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
                                if (curMode == GraphicWorkMode.ZOOM_BOX) that().refreshView(null, GraphicViewCoord(newT1, newT2))
                                else {
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
        "onMouseWheel" to { event: Event ->
            val wheelEvent = event as WheelEvent
            val isCtrl = wheelEvent.ctrlKey
            val mouseX = wheelEvent.offsetX.toInt()
//            val mouseY = wheelEvent.offsetY.toInt()
            val deltaY = wheelEvent.deltaY.toInt()

//            val timeOffset = that().timeOffset.unsafeCast<Int>()
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()
            val curMode = that().curMode.unsafeCast<GraphicWorkMode>()
            val pixStartY = that().pixStartY.unsafeCast<Int>()

            val isMouseDown = that().isMouseDown.unsafeCast<Boolean>()

            val svgCoords = defineGraphicSvgCoords(tabId)

            if (curMode == GraphicWorkMode.PAN && !isMouseDown || curMode == GraphicWorkMode.ZOOM_BOX && !isMouseDown) {
                //|| grControl.curMode == GraphicModel.WorkMode.SELECT_FOR_PRINT && grControl.selectorX1 < 0  ) {
                //--- масштабирование
                if (isCtrl) {
                    val t1 = viewCoord.t1
                    val t2 = viewCoord.t2
                    //--- вычисляем текущую координату курсора в реальных координатах
                    val curT = getTimeFromX(mouseX, svgCoords.bodyWidth, t1, viewCoord.width)

                    val newT1 = if (deltaY < 0) curT - (curT - t1) / 2 else curT - (curT - t1) * 2
                    val newT2 = if (deltaY < 0) curT + (t2 - curT) / 2 else curT + (t2 - curT) * 2

                    if (newT2 - newT1 in MIN_SCALE_X..MAX_SCALE_X)
                        that().refreshView(null, GraphicViewCoord(newT1, newT2))
                }
                //--- вертикальная прокрутка
                else {
                    val arrViewBoxAxis = getGraphicViewBoxAxis(that())
                    val arrViewBoxBody = getGraphicViewBoxBody(that())

                    val dy = (deltaY * scaleKoef).roundToInt()

                    if (dy < 0) {
                        arrViewBoxAxis[1] += dy
                        if (arrViewBoxAxis[1] < 0) arrViewBoxAxis[1] = 0

                        arrViewBoxBody[1] += dy
                        if (arrViewBoxBody[1] < 0) arrViewBoxBody[1] = 0
                    } else if (dy > 0 && pixStartY - arrViewBoxAxis[1] > svgCoords.bodyHeight) {
                        arrViewBoxAxis[1] += dy

                        arrViewBoxBody[1] += dy
                    }

                    setGraphicViewBoxAxis(that(), intArrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                    setGraphicViewBoxBody(that(), intArrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))

                    setGraphicTextOffset(that(), svgCoords.bodyLeft, svgCoords.bodyTop)
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

            that().curMode = GraphicWorkMode.PAN
        },
        "setModeZoomBox" to {
            that().isPanButtonDisabled = false
            that().isZoomButtonDisabled = true

            that().curMode = GraphicWorkMode.ZOOM_BOX
        },
        "zoomIn" to {
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()

            val t1 = viewCoord.t1
            val t2 = viewCoord.t2
            val grWidth = viewCoord.width

            val newT1 = t1 + grWidth / 4
            val newT2 = t2 - grWidth / 4

            if (newT2 - newT1 >= MIN_SCALE_X) {
                that().refreshView(null, GraphicViewCoord(newT1, newT2))
            }

        },
        "zoomOut" to {
            val viewCoord = that().viewCoord.unsafeCast<GraphicViewCoord>()

            val t1 = viewCoord.t1
            val t2 = viewCoord.t2
            val grWidth = viewCoord.width

            val newT1 = t1 - grWidth / 2
            val newT2 = t2 + grWidth / 2
            if (newT2 - newT1 <= MAX_SCALE_X) {
                that().refreshView(null, GraphicViewCoord(newT1, newT2))
            }
        },
        "setShowBack" to {
            val that = that()
            val isShowBack = that().isShowBack.unsafeCast<Boolean>()
            invokeSaveUserProperty(
                SaveUserPropertyRequest(UP_GRAPHIC_SHOW_BACK, (!isShowBack).toString()),
                {
                    that.refreshView(that, null)
                }
            )
        },
        "setShowPoint" to {
            val that = that()
            val isShowPoint = that().isShowPoint.unsafeCast<Boolean>()
            invokeSaveUserProperty(
                SaveUserPropertyRequest(UP_GRAPHIC_SHOW_POINT, (!isShowPoint).toString()),
                {
                    that.refreshView(that, null)
                }
            )
        },
        "setShowLine" to {
            val that = that()
            val isShowLine = that().isShowLine.unsafeCast<Boolean>()
            invokeSaveUserProperty(
                SaveUserPropertyRequest(UP_GRAPHIC_SHOW_LINE, (!isShowLine).toString()),
                {
                    that.refreshView(that, null)
                }
            )
        },
        "setShowText" to {
            val that = that()
            val isShowText = that().isShowText.unsafeCast<Boolean>()
            invokeSaveUserProperty(
                SaveUserPropertyRequest(UP_GRAPHIC_SHOW_TEXT, (!isShowText).toString()),
                {
                    that.refreshView(that, null)
                }
            )
        }
    )
/*
                else if( comp == butElementVisible ) {
                    if( grModel.alVisibleElement.isEmpty() ) showInformation( "Выбор графиков", "Нет графиков для показа" )
                    else {
                        val dialog = Dialog<Any>()
                        dialog.title = "Выбор графиков для показа"
                        dialog.headerText = null
                        //dialog.setContentText(  null  );
                        dialog.initStyle( StageStyle.UTILITY )
                        dialog.initModality( Modality.APPLICATION_MODAL )
                        //!!! ни один из вариантов пока не работает
                        //            dialog.initOwner(  appStage  );
                        //            URL iconURL = getClass().getResource(  ICON_URL  );
                        //            if(  iconURL != null  ) (  ( Stage ) dialog.getDialogPane().getScene().getWindow()  ).getIcons().add(  new Image(  iconURL.toString()  )  );
                        dialog.graphic = ImageView( javaClass.getResource( "/images/ic_trending_up_black_18dp.png" ).toString() )

                        val okButtonType = ButtonType( "OK", ButtonBar.ButtonData.OK_DONE )
                        dialog.dialogPane.buttonTypes.addAll( okButtonType, ButtonType.CANCEL )

                        val grid = GridPane()
                        grid.hgap = 16.0
                        grid.vgap = 16.0

                        val arrKey = arrayOfNulls<String>( grModel.alVisibleElement.size )
                        val arrCheckBox = arrayOfNulls<CheckBox>( grModel.alVisibleElement.size )
                        var i = 0
                        for( ( key, value ) in grModel.alVisibleElement ) {
                            val strGraphicVisible = appContainer.getUserProperty( value )
                            val isGraphicVisible = strGraphicVisible == null || java.lang.Boolean.parseBoolean( strGraphicVisible )
                            arrKey[ i ] = value
                            arrCheckBox[ i ] = CheckBox( key )
                            arrCheckBox[ i ]!!.setSelected( isGraphicVisible )
                            grid.add( arrCheckBox[ i ], 0, i )
                            i++
                        }
                        dialog.dialogPane.content = grid
                        dialog.setResultConverter { dialogButton -> if( dialogButton == okButtonType ) Any() else null }
                        val result = dialog.showAndWait()
                        if( result.isPresent ) {
                            result.get()   //--- возвращаемый объект не нужен, у нас и так есть доступ к полям
                            i = 0
                            while( i < arrCheckBox.size ) {
                                appContainer.saveUserProperty( arrKey[ i ]!!, java.lang.Boolean.toString( arrCheckBox[ i ]!!.isSelected ) )
                                i++
                            }
                            refreshView( 0 )
                        }
                    }
                }
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
                        else refreshView( GraphicViewCoord( begTime, endTime ), 0 )
                    }
                    catch( nfe: NumberFormatException ) {
                        showError( "Ошибка задания периода", "Неправильно заданы дата/время" )
                    }

                }

 */
    this.mounted = {
        val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

        that().fullTitle = graphicResponse.fullTitle
        that().`$root`.setTabInfo(tabId, graphicResponse.shortTitle, graphicResponse.fullTitle)

//        //--- показ точек по умолчанию выключен, если не указано явно иное
//        val pointShowMode = appContainer.getUserProperty( iCoreAppContainer.UP_GRAPHIC_SHOW_POINT )
//        chShowPoint.isSelected = pointShowMode != null && java.lang.Boolean.parseBoolean( pointShowMode )
//
//        val lineShowMode = appContainer.getUserProperty( iCoreAppContainer.UP_GRAPHIC_SHOW_LINE )
//        chShowLine.isSelected = lineShowMode == null || java.lang.Boolean.parseBoolean( lineShowMode )
//
//        val textShowMode = appContainer.getUserProperty( iCoreAppContainer.UP_GRAPHIC_SHOW_TEXT )
//        chShowText.isSelected = textShowMode == null || java.lang.Boolean.parseBoolean( textShowMode )

        that().setModePan()

        //--- установка динамической (зависящей от scaleKoef) ширины области с вертикальными осями
        that().svg_axis_width = (MARGIN_LEFT * 2 * scaleKoef).roundToInt()

        //--- принудительная установка полной высоты svg-элементов
        //--- (BUG: иначе высота либо равна 150px - если не указывать высоту,
        //--- либо равно width, если указать height="100%")
        val svgCoords = defineGraphicSvgCoords(tabId)
        that().svg_height = window.innerHeight - svgCoords.bodyTop

        that().style_graphic_text = json(
            "font-size" to "${1.0 * scaleKoef}rem"
        )

        //--- для проброса this внутрь лямбд
        val that = that()
        that.`$root`.setWait(true)
        invokeGraphic(
            GraphicActionRequest(
                documentTypeName = graphicResponse.documentTypeName,
                action = GraphicAction.GET_COORDS,
                startParamId = graphicResponse.startParamId
            ),
            { graphicActionResponse: GraphicActionResponse ->

                val svgAxisElement = document.getElementById("svg_axis_$tabId")!!
                val svgAxisWidth = svgAxisElement.clientWidth
                val svgAxisHeight = svgAxisElement.clientHeight

                setGraphicViewBoxAxis(that, intArrayOf(0, 0, svgAxisWidth, svgAxisHeight))

                val svgBodyElement = document.getElementById("svg_body_$tabId")!!
                val svgBodyWidth = svgBodyElement.clientWidth
                val svgBodyHeight = svgBodyElement.clientHeight

                setGraphicViewBoxBody(that, intArrayOf(0, 0, svgBodyWidth, svgBodyHeight))

                val newViewCoord = GraphicViewCoord(graphicActionResponse.begTime!!, graphicActionResponse.endTime!!)
                //--- именно до refreshView, чтобы не сбросить сразу после включения
                that.`$root`.setWait(false)
                that.refreshView(that, newViewCoord)
            }
        )
    }

    this.data = {
        json(
            "fullTitle" to "",

            "svg_axis_width" to 0,
            "svg_height" to "100%",

            "viewCoord" to GraphicViewCoord(0, 0),
            "arrGraphicElement" to arrayOf<GraphicElement>(),
            "arrYData" to arrayOf<YData>(),
            "curMode" to GraphicWorkMode.PAN,
            "isPanButtonDisabled" to true,
            "isZoomButtonDisabled" to false,

            "isShowBack" to true,
            "isShowPoint" to false,
            "isShowLine" to true,
            "isShowText" to true,

            "pixStartY" to 0,
            "viewBoxAxis" to "0 0 1 1",
            "viewBoxBody" to "0 0 1 1",
            "isMouseDown" to false,
            "panPointOldX" to 0,
            "panPointOldY" to 0,
            "panDX" to 0,
            "tooltipOffTime" to 0.0,

            "mouseRect" to MouseRectData(false, 0, 0, 0, 0, 1),
            "timeLine" to LineData(false, 0, 0, 0, 0, 1),

            "arrTimeLabel" to arrayOf(TimeLabelData(), TimeLabelData(), TimeLabelData()),

            "tooltipVisible" to false,
            "tooltipText" to "",

            "style_header" to json(
                "border-top" to if (!styleIsNarrowScreen) "none" else "1px solid $COLOR_BUTTON_BORDER"
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
            "style_htp_checkbox" to json(
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS_SMALL,
                "transform" to styleControlCheckBoxTransform(),
                "margin" to styleGraphicCheckBoxMargin()
            ),
            "style_htp_checkbox_label" to json(
                "font-size" to styleControlTextFontSize(),
                "padding" to styleGraphicCheckBoxLabelPadding()
            ),
            "style_graphic_text" to json(
            ),
            "style_tooltip_text" to json(
                "position" to "absolute",
                "color" to COLOR_GRAPHIC_LABEL_TEXT,
                "background" to COLOR_GRAPHIC_LABEL_BACK,
                "border" to "1px solid $COLOR_GRAPHIC_LABEL_BORDER",
                "border-radius" to BORDER_RADIUS,
                "padding" to styleControlTooltipPadding(),
                "user-select" to if (styleIsNarrowScreen) "none" else "auto"
            ),
            "style_tooltip_pos" to json(
                "left" to "",
                "top" to ""
            )
        )
    }
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class GraphicSvgCoords(
    val axisWidth: Int,
    val bodyLeft: Int,
    val bodyTop: Int,
    val bodyWidth: Int,
    val bodyHeight: Int,
)

private fun defineGraphicSvgCoords(tabId: Int): GraphicSvgCoords {
    val menuBarElement = document.getElementById(MENU_BAR_ID)
    val svgTabPanel = document.getElementById("tab_panel")!!
    val svgGraphicTitle = document.getElementById("graphic_title_$tabId")!!
    val svgGraphicToolbar = document.getElementById("graphic_toolbar_$tabId")!!
    val svgAxisElement = document.getElementById("svg_axis_$tabId")!!
    val svgBodyElement = document.getElementById("svg_body_$tabId")!!

    val menuBarWidth = menuBarElement?.clientWidth ?: 0
    val svgAxisWidth = svgAxisElement.clientWidth

    return GraphicSvgCoords(
        axisWidth = svgAxisWidth,
        bodyLeft = menuBarWidth + svgAxisWidth,  //svgBodyElement.clientLeft - BUG: всегда даёт 0
        bodyTop = svgTabPanel.clientHeight + svgGraphicTitle.clientHeight + svgGraphicToolbar.clientHeight,  //svgBodyElement.clientTop - BUG: всегда даёт 0
        bodyWidth = svgBodyElement.clientWidth,
        bodyHeight = svgBodyElement.clientHeight,
    )
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class GraphicElementData(
    val title: SvgText,
    val alAxisYLine: MutableList<SvgLine>,
    val alAxisYText: MutableList<SvgText>,
    val alAxisXLine: MutableList<SvgLine>,
    val alAxisXText: MutableList<SvgMultiLineText>,
    val alGraphicBack: MutableList<SvgRect>,
    val alGraphicLine: MutableList<SvgLine>,
    val alGraphicPoint: MutableList<SvgCircle>,
    val alGraphicText: MutableList<GraphicTextData>
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
    val arrGraphicText: Array<GraphicTextData>
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
    val sViewBox = that.viewBoxAxis.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

private fun setGraphicViewBoxAxis(that: dynamic, arrViewBox: IntArray) {
    that.viewBoxAxis = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
}

private fun getGraphicViewBoxBody(that: dynamic): IntArray {
    val sViewBox = that.viewBoxBody.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

private fun setGraphicViewBoxBody(that: dynamic, arrViewBox: IntArray) {
    that.viewBoxBody = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
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
    val timeLine = that.timeLine.unsafeCast<LineData>()
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
    hmIndexColor: Map<String, Map<String, String>>,
    svgAxisWidth: Int,
    svgBodyWidth: Int,
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

    //--- заголовок

    val titleData = SvgText(
        x = (MIN_GRID_STEP_X * scaleKoef).roundToInt(),
        y = (pixDrawTopY - 4 * scaleKoef).roundToInt(),
        text = element.graphicTitle,
        stroke = COLOR_GRAPHIC_TITLE,
        hAlign = "start",
        vAlign = "text-bottom"
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

    //--- графики ---

    val alPrevTextBounds = mutableListOf<XyRect>()

    //--- для преодоления целочисленного переполнения
    val svgBodyWidthDouble = svgBodyWidth.toDouble()
    val hmCurIndexColor = hmIndexColor[element.graphicTitle]!!

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
                                stroke = hmCurIndexColor[gld.colorIndex.toString()]!!,
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

            GraphicDataContainer.ElementType.POINT.toString() -> {
                val ayd = element.alAxisYData[cagdc.axisYIndex]
                val graphicHeight = ayd.max - ayd.min

                for (gpd in cagdc.alGPD) {
                    val drawX = svgBodyWidth * (gpd.x - t1) / (t2 - t1)
                    val drawY = pixDrawY0 - pixDrawHeight * (gpd.y - ayd.min) / graphicHeight

                    val value = if (cagdc.itReversedY) {
                        -gpd.y
                    } else {
                        gpd.y
                    }

                    alGraphicPoint.add(
                        SvgCircle(
                            cx = drawX,
                            cy = drawY.toInt(),
                            radius = max(1, scaleKoef.roundToInt()),
                            fill = hmCurIndexColor[gpd.colorIndex.toString()]!!,
                            tooltip = getSplittedDouble(value, alYData[alAxisYDataIndex[axisYIndex]].prec, true, '.')
                        )
                    )
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
                                "border" to "${1 * scaleKoef}px solid ${hmCurIndexColor[gtd.borderColorIndex.toString()]!!}",
                                "color" to hmCurIndexColor[gtd.textColorIndex.toString()]!!,
                                "background" to hmCurIndexColor[gtd.fillColorIndex.toString()]!!,
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
            dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) "${scaleKoef * 2},${scaleKoef * 2}" else ""
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
                hAlign = "middle",
                vAlign = "hanging"
            )

            alAxisText.add(axisText)
        }
        notchX += notchStepX
    }
    //--- первую метку перевыровнять к началу, последнюю - к концу
    alAxisText.first().hAlign = "start"
    alAxisText.last().hAlign = "end"

    //--- ось X
    val line = SvgLine(
        x1 = 0,
        y1 = pixDrawY0,
        x2 = pixWidth,
        y2 = pixDrawY0,
        stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
        width = max(1, scaleKoef.roundToInt()),
        //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
        dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) "${scaleKoef * 2},${scaleKoef * 2}" else ""
    )
    alAxisLine.add(line)
}

private fun drawAxisY(
    scaleKoef: Double,

    hmIndexColor: Map<String, Map<String, String>>,

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

        if (round(notchY * mult).toInt() % round(labelGraphicStepY * mult).toInt() == 0) {
            val value = if (ayd.itReversedY) {
                -notchY
            } else {
                notchY
            }
            val axisText = SvgText(
                x = axisX - (2 * scaleKoef).roundToInt(),
                y = drawY.toInt() - (2 * scaleKoef).roundToInt(),
                text = getSplittedDouble(value, precY, true, '.'),
                stroke = hmIndexColor[element.graphicTitle]!![ayd.colorIndex.toString()]!!,
                hAlign = "end",
                vAlign = "text-bottom"
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
        stroke = hmIndexColor[element.graphicTitle]!![ayd.colorIndex.toString()]!!,
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
        stroke = hmIndexColor[element.graphicTitle]!![ayd.colorIndex.toString()]!!,
        hAlign = "middle",
        vAlign = "hanging",
        transform = "rotate(-90 $axisTextX $axisTextY)"
    )

    alAxisText.add(axisText)  //!!! не видно?

    return precY
}
