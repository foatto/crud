package foatto.core_web

import foatto.core.app.xy.*
import foatto.core.app.xy.config.XyBitmapType
import foatto.core.app.xy.geom.XyLine
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyElementConfig
import foatto.core.link.XyResponse
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.browser.document
import kotlin.js.json
import kotlin.math.*

private enum class MapWorkMode {
    PAN, ZOOM_BOX, SELECT_FOR_ACTION, DISTANCER, ACTION_ADD, ACTION_EDIT_POINT, ACTION_MOVE
}

//--- опции выбора
private enum class SelectOption { SET, ADD, REVERT, DELETE }

private const val startExpandKoef = 0.05
private val mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта

@Suppress("UnsafeCastFromDynamic")
fun mapControl( xyResponse: XyResponse, tabIndex: Int ) = vueComponentOptions().apply {

    this.template =
"""
    <div>
        <div id="map_title_$tabIndex" v-bind:style="[ style_toolbar, style_header ]">
            <span v-bind:style="style_toolbar_block">
            </span>
            <span v-bind:style="[style_toolbar_block, style_title]">
                {{fullTitle}}
            </span>
            <span v-bind:style="style_toolbar_block">
            </span>
        </div>
        <div id="map_toolbar_$tabIndex" v-bind:style="style_toolbar">
            <span v-bind:style="style_toolbar_block">
                <img src="/web/images/ic_open_with_black_48dp.png" 
                     v-on:click="setMode( '${MapWorkMode.PAN}' )"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isPanButtonDisabled"
                     title="Перемещение по карте"
                >
                <img src="/web/images/ic_search_black_48dp.png" 
                     v-on:click="setMode( '${MapWorkMode.ZOOM_BOX}' )"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isZoomButtonDisabled"
                     title="Выбор области для показа"
                >
                <img src="/web/images/ic_linear_scale_black_48dp.png"
                     v-show="${!styleIsNarrowScreen}"
                     v-on:click="setMode( '${MapWorkMode.DISTANCER}' )"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isDistancerButtonDisabled"
                     title="Измерение расстояний"
                >
                <img src="/web/images/ic_touch_app_black_48dp.png" 
                     v-on:click="setMode( '${MapWorkMode.SELECT_FOR_ACTION}' )"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isSelectButtonDisabled"
                     title="Работа с объектами"
                >
            </span>                             

            <span v-bind:style="style_toolbar_block">
                <img src="/web/images/ic_zoom_in_black_48dp.png"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isZoomInButtonDisabled"
                     title="Ближе"
                     v-on:click="zoomIn()"
                >
                <img src="/web/images/ic_zoom_out_black_48dp.png"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isZoomOutButtonDisabled"
                     title="Дальше"
                     v-on:click="zoomOut()"
                >
            </span>

            <span v-bind:style="style_toolbar_block">
                <template v-if="isAddElementButtonVisible">
                    <span v-show="arrAddEC.length > 0">
                        Добавить:
                    </span>
                    <button v-for="ec in arrAddEC"
                            v-on:click="startAdd( ec )"
                            v-bind:style="style_text_button"
                            v-bind:title="'Добавить `' + ec.descrForAction + '`'"
                    >
                        {{ec.descrForAction}}
                    </button>
                </template>
            </span>

            <span v-bind:style="style_toolbar_block">
                <img src="/web/images/ic_format_shapes_black_48dp.png"
                     v-show="isEditPointButtonVisible"
                     v-bind:style="style_icon_button"
                     title="Редактирование точек"
                     v-on:click="startEditPoint()"
                >
                <img src="/web/images/ic_zoom_out_map_black_48dp.png"
                     v-show="isMoveElementsButtonVisible"
                     v-bind:style="style_icon_button"
                     title="Перемещение объектов"
                     v-on:click="startMoveElements()"
                >
            </span>            

            <span v-bind:style="style_toolbar_block">
                <img src="/web/images/ic_save_black_48dp.png"
                     v-show="isActionOkButtonVisible"
                     v-bind:style="style_icon_button"
                     title="Сохранить"
                     v-on:click="actionOk()"
                >
                <img src="/web/images/ic_exit_to_app_black_48dp.png"
                     v-show="isActionCancelButtonVisible"
                     v-bind:style="style_icon_button"
                     title="Отменить"
                     v-on:click="actionCancel()"
                >
            </span>                     

            <span v-bind:style="style_toolbar_block">
                <img src="/web/images/ic_sync_black_48dp.png"
                     v-bind:style="style_icon_button"
                     v-bind:disabled="isRefreshButtonDisabled"
                     title="Обновить"
                     v-on:click="refreshView( null, null )"
                >
            </span>
        </div>

""" +

getXyElementTemplate( tabIndex,

"""
        <template v-if="mouseRect.isVisible">
            <rect v-bind:x="Math.min(mouseRect.x1, mouseRect.x2)"
                  v-bind:y="Math.min(mouseRect.y1, mouseRect.y2)"
                  v-bind:width="Math.abs(mouseRect.x2 - mouseRect.x1)"
                  v-bind:height="Math.abs(mouseRect.y2 - mouseRect.y1)"
                  fill="$COLOR_XY_LINE"
                  opacity="0.25"
            />
            <line v-bind:x1="mouseRect.x1" v-bind:y1="mouseRect.y1" v-bind:x2="mouseRect.x2" v-bind:y2="mouseRect.y1"
                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_XY_LINE" />
            <line v-bind:x1="mouseRect.x2" v-bind:y1="mouseRect.y1" v-bind:x2="mouseRect.x2" v-bind:y2="mouseRect.y2"
                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_XY_LINE" />
            <line v-bind:x1="mouseRect.x2" v-bind:y1="mouseRect.y2" v-bind:x2="mouseRect.x1" v-bind:y2="mouseRect.y2"
                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_XY_LINE" />
            <line v-bind:x1="mouseRect.x1" v-bind:y1="mouseRect.y2" v-bind:x2="mouseRect.x1" v-bind:y2="mouseRect.y1"
                  v-bind:stroke-width="mouseRect.lineWidth" stroke="$COLOR_XY_LINE" />
        </template>

        <line v-for="distancerLine in arrDistancerLine"
              v-bind:x1="distancerLine.x1"
              v-bind:y1="distancerLine.y1"
              v-bind:x2="distancerLine.x2"
              v-bind:y2="distancerLine.y2"
              v-bind:stroke="distancerLine.stroke"
              v-bind:stroke-width="distancerLine.strokeWidth"
              v-bind:stroke-dasharray="distancerLine.strokeDash"
        />

        <template v-if="addElement">
            <polygon v-if="addElement.type == '${XyElementDataType.POLYGON}'"
                     v-bind:points="addElement.points"
                     v-bind:stroke="addElement.itSelected ? '$COLOR_XY_ZONE_BORDER' : addElement.stroke"
                     v-bind:fill="addElement.fill"
                     v-bind:stroke-width="addElement.strokeWidth"
                     v-bind:stroke-dasharray="addElement.itSelected ? addElement.strokeDash : ''"
                     v-bind:transform="addElement.transform"
            />
        </template>
"""

) +

"""
        <div v-for="distancerText in arrDistancerText"
             v-bind:style="[distancerText.pos, distancerText.style]"
             v-html="distancerText.text"
        >
        </div>
        <!-- именно if, потому что show пытается скрыто нарисовать null-distancerSumText -->
        <div v-if="distancerSumText"
             v-bind:style="[distancerSumText.pos, distancerSumText.style]"
             v-html="distancerSumText.text"
        >
        </div>

    </div>
"""

    this.methods = json(
        //--- метод может вызываться из лямбд, поэтому возможен проброс ему "истинного" this
        "refreshView" to { aThat: dynamic, aView: XyViewCoord? ->
            val that = aThat ?: that()

            val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

            val curViewCoord = that().viewCoord.unsafeCast<XyViewCoord>()

            val svgTabPanel = document.getElementById( "tab_panel" )!!
            val svgMapTitle = document.getElementById( "map_title_$tabIndex" )!!
            val svgMapToolbar = document.getElementById( "map_toolbar_$tabIndex" )!!
            val svgBodyElement = document.getElementById( "svg_body_$tabIndex" )!!

            val svgBodyLeft = 0
            val svgBodyTop = svgTabPanel.clientHeight + svgMapTitle.clientHeight + svgMapToolbar.clientHeight  //svgBodyElement.clientTop - BUG: всегда даёт 0
            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            val newView =
                if( aView != null ) {
                    //--- вычисляем координату середины (безопасным от переполнения способом)
                    //--- и выносим эту точку на середину экрана
                    //--- и сохраненяем новое состояние в view
                    val checkedView = getXyViewCoord(
                        aScale = checkXyScale(
                            minScale = xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin,
                            maxScale = xyResponse.documentConfig.alElementConfig.maxBy { it.second.scaleMax }!!.second.scaleMax,
                            itScaleAlign = xyResponse.documentConfig.itScaleAlign,
                            curScale = curViewCoord.scale,
                            newScale = aView.scale,
                            isAdaptive = false
                        ),
                        aViewWidth = svgBodyWidth,
                        aViewHeight = svgBodyHeight,
                        aCenterX = aView.x1 + ( aView.x2 - aView.x1 ) / 2,
                        aCenterY = aView.y1 + ( aView.y2 - aView.y1 ) / 2,
                        scaleKoef = scaleKoef )

                    //--- обновляем, только если изменилось (оптимизируем цепочку реактивных изменений)
                    that.viewCoord = checkedView
                    checkedView
                }
                else {
                    curViewCoord
                }

            getXyElements( that, xyResponse, scaleKoef, newView, mapBitmapTypeName, svgBodyLeft, svgBodyTop )
            //--- обновление в любом случае сбрасывает выделенность элементов и возможность соответствующих операций
            that.isEditPointButtonVisible = false
            that.isMoveElementsButtonVisible = false
        },
        "onMouseOver" to { event: Event, xyElement: XyElementData ->
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when( curMode.toString() ) {
                MapWorkMode.PAN.toString(),
                MapWorkMode.ZOOM_BOX.toString(),
                MapWorkMode.SELECT_FOR_ACTION.toString() -> onXyMouseOver( that(), event as MouseEvent, xyElement )
            }
        },
        "onMouseOut" to {
            onXyMouseOut( that() )
        },
        "onMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            val svgTabPanel = document.getElementById( "tab_panel" )!!
            val svgMapTitle = document.getElementById( "map_title_$tabIndex" )!!
            val svgMapToolbar = document.getElementById( "map_toolbar_$tabIndex" )!!

            val svgBodyLeft = 0
            val svgBodyTop = svgTabPanel.clientHeight + svgMapTitle.clientHeight + svgMapToolbar.clientHeight  //svgBodyElement.clientTop - BUG: всегда даёт 0

            if( isNeedOffsetCompensation ) {
                mouseX -= svgBodyLeft
                mouseY -= svgBodyTop
            }

            when( curMode ) {
                MapWorkMode.PAN -> {
                    that().panPointOldX = mouseX
                    that().panPointOldY = mouseY
                    that().panDX = 0
                    that().panDY = 0
                }
                MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {
                    that().mouseRect = MouseRectData( true, mouseX, mouseY, mouseX, mouseY, max( 1.0, scaleKoef ).roundToInt() )
                }
                MapWorkMode.ACTION_EDIT_POINT -> {
                    val clickRect = getXyClickRect( mouseX, mouseY )
                    val editElement = that().editElement.unsafeCast<XyElementData>()
                    var editPointIndex = -1
                    //--- попытаемся найти вершину, на которую кликнули
                    for( i in 0..editElement.alPoint!!.lastIndex )
                        if( clickRect.isContains( editElement.alPoint[ i ] ) ) {
                            editPointIndex = i
                            break
                        }
                    //--- если кликнутую вершину не нашли, попытаемся найти отрезок, на который кликнули
                    if( editPointIndex == -1 ) {
                        for( i in 0..editElement.alPoint.lastIndex )
                            if( clickRect.isIntersects(
                                    XyLine( editElement.alPoint[ i ],
                                            editElement.alPoint[ if( i == editElement.alPoint.lastIndex ) 0 else ( i + 1 ) ] ) )) {
                                //--- в месте клика на отрезке добавляем точку, которую будем двигать
                                editPointIndex = i + 1
                                editElement.insertPoint( editPointIndex, mouseX, mouseY )
                                break
                            }
                    }
                    that().editPointIndex = editPointIndex
                }
                MapWorkMode.ACTION_MOVE -> {
                    that().moveStartPoint = XyPoint( mouseX, mouseY )
                    that().moveEndPoint = XyPoint( mouseX, mouseY )
                }
            }
            that().isMouseDown = true
        },
        "onMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

//            val timeOffset = that().`$root`.timeOffset.unsafeCast<Int>()
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            val isMouseDown = that().isMouseDown.unsafeCast<Boolean>()
            val panPointOldX = that().panPointOldX.unsafeCast<Int>()
            val panPointOldY = that().panPointOldY.unsafeCast<Int>()
            val panDX = that().panDX.unsafeCast<Int>()
            val panDY = that().panDY.unsafeCast<Int>()

            val svgTabPanel = document.getElementById( "tab_panel" )!!
            val svgMapTitle = document.getElementById( "map_title_$tabIndex" )!!
            val svgMapToolbar = document.getElementById( "map_toolbar_$tabIndex" )!!
            val svgBodyElement = document.getElementById( "svg_body_$tabIndex" )!!

            val svgBodyLeft = 0
            val svgBodyTop = svgTabPanel.clientHeight + svgMapTitle.clientHeight + svgMapToolbar.clientHeight  //svgBodyElement.clientTop - BUG: всегда даёт 0
            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            if( isNeedOffsetCompensation ) {
                mouseX -= svgBodyLeft
                mouseY -= svgBodyTop
            }

            //--- mouse dragged
            if( isMouseDown ) {
                when( curMode ) {
                    MapWorkMode.PAN -> {
                        var dx = mouseX - panPointOldX
                        var dy = mouseY - panPointOldY

                        val arrViewBoxBody = getXyViewBoxBody( that() )

                        arrViewBoxBody[ 0 ] -= dx
                        arrViewBoxBody[ 1 ] -= dy

                        that().panPointOldX = mouseX
                        that().panPointOldY = mouseY
                        that().panDX = panDX + dx
                        that().panDY = panDY + dy

                        setXyViewBoxBody( that(), intArrayOf( arrViewBoxBody[ 0 ], arrViewBoxBody[ 1 ], arrViewBoxBody[ 2 ], arrViewBoxBody[ 3 ] ) )

                        setXyTextOffset( that(), svgBodyLeft, svgBodyTop )
                    }
                    MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {
                        val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()

                        if( mouseRect.isVisible && mouseX >= 0 && mouseX <= svgBodyWidth && mouseY >= 0 && mouseY <= svgBodyHeight ) {
                            mouseRect.x2 = mouseX
                            mouseRect.y2 = mouseY
                        }
                    }
                    MapWorkMode.ACTION_EDIT_POINT -> {
                        val editElement = that().editElement.unsafeCast<XyElementData>()
                        val editPointIndex = that().editPointIndex.unsafeCast<Int>()
                        if( editPointIndex != -1 )
                            editElement.setPoint( editPointIndex, mouseX, mouseY )
                    }
                    MapWorkMode.ACTION_MOVE -> {
                        val arrMoveElement = that().arrMoveElement.unsafeCast<Array<XyElementData>>()
                        val moveEndPoint = that().moveEndPoint.unsafeCast<XyPoint>()
                        for( element in arrMoveElement )
                            element.moveRel( mouseX - moveEndPoint.x, mouseY - moveEndPoint.y )
                        moveEndPoint.set( mouseX, mouseY )
                        that().moveEndPoint = moveEndPoint
                    }
                }
            }
            //--- mouse moved
            else {
                when( curMode ) {
                    MapWorkMode.DISTANCER -> {
                        val alDistancerLine = that().arrDistancerLine.unsafeCast<Array<XyElementData>>().toMutableList()
                        val alDistancerDist = that().arrDistancerDist.unsafeCast<Array<Double>>().toMutableList()
                        val alDistancerText = that().arrDistancerText.unsafeCast<Array<XyElementData>>().toMutableList()
                        val distancerSumText = that().distancerSumText.unsafeCast<XyElementData>()

                        if( alDistancerLine.isNotEmpty() ) {
                            val line = alDistancerLine.last()
                            line.x2 = mouseX
                            line.y2 = mouseY

                            val dist = XyProjection.distancePrj(
                                XyPoint( viewCoord.x1 + mouseToReal( scaleKoef, viewCoord.scale, line.x1!! ),
                                         viewCoord.y1 + mouseToReal( scaleKoef, viewCoord.scale, line.y1!! ) ),
                                XyPoint( viewCoord.x1 + mouseToReal( scaleKoef, viewCoord.scale, line.x2!! ),
                                         viewCoord.y1 + mouseToReal( scaleKoef, viewCoord.scale, line.y2!! ) ),
                                viewCoord.scale
                            ) / 1000.0

                            alDistancerDist[ alDistancerDist.lastIndex ] = dist

                            val distancerSumDist = alDistancerDist.sum()

                            val text = alDistancerText.last()
                            text.x = ( line.x1 + line.x2!! ) / 2
                            text.y = ( line.y1 + line.y2!! ) / 2
                            text.text = getSplittedDouble( dist, 1 )
                            text.pos = json(
                                "left" to "${svgBodyLeft + text.x!!}px",
                                "top" to "${svgBodyTop + text.y!!}px"
                            )

                            distancerSumText.x = mouseX
                            distancerSumText.y = mouseY
                            //--- иногда вышибает округлятор в getSplittedDouble
                            distancerSumText.text =
                                try {
                                    getSplittedDouble( distancerSumDist, 1 )
                                }
                                catch( t: Throwable ) {
                                    distancerSumText.text
                                }
                            distancerSumText.pos = json(
                                "left" to "${svgBodyLeft + mouseX + 16}px",
                                "top" to "${svgBodyTop + mouseY + 16}px"
                            )

                            that().arrDistancerLine = alDistancerLine.toTypedArray()
                            that().arrDistancerDist = alDistancerDist.toTypedArray()
                            that().arrDistancerText = alDistancerText.toTypedArray()
                            //that().distancerSumText = distancerSumText - излишне
                        }
                    }

                    MapWorkMode.ACTION_ADD -> {
                        val addElement = that().addElement.unsafeCast<XyElementData>()
                        addElement.setLastPoint( mouseX, mouseY )
                    }
                }
            }
        },
        "onMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            val panDX = that().panDX.unsafeCast<Int>()
            val panDY = that().panDY.unsafeCast<Int>()

            val svgTabPanel = document.getElementById( "tab_panel" )!!
            val svgMapTitle = document.getElementById( "map_title_$tabIndex" )!!
            val svgMapToolbar = document.getElementById( "map_toolbar_$tabIndex" )!!
            val svgBodyElement = document.getElementById( "svg_body_$tabIndex" )!!

            val svgBodyLeft = 0
            val svgBodyTop = svgTabPanel.clientHeight + svgMapTitle.clientHeight + svgMapToolbar.clientHeight  //svgBodyElement.clientTop - BUG: всегда даёт 0
            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            if( isNeedOffsetCompensation ) {
                mouseX -= svgBodyLeft
                mouseY -= svgBodyTop
            }

            when( curMode ) {
                MapWorkMode.PAN -> {
                    //--- перезагружаем карту, только если был горизонтальный сдвиг
                    if( abs( panDX ) >= 1 || abs( panDY ) >= 1 ) {
                        viewCoord.moveRel( ( - panDX * viewCoord.scale / scaleKoef ).roundToInt(), ( - panDY * viewCoord.scale / scaleKoef ).roundToInt() )
                        that().refreshView( null, viewCoord )
                    }
                    that().panPointOldX = 0
                    that().panPointOldY = 0
                    that().panDX = 0
                }
                MapWorkMode.ZOOM_BOX -> {
                    val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()

                    if( mouseRect.isVisible ) {
                        mouseRect.isVisible = false

                        val mouseWidth = abs( mouseRect.x2 - mouseRect.x1 )
                        val mouseHeight = abs( mouseRect.y2 - mouseRect.y1 )

                        //--- если размер прямоугольника меньше 8 pix, то это видимо ошибка - игнорируем
                        if( mouseWidth >= ( MIN_USER_RECT_SIZE * scaleKoef ).roundToInt() && mouseHeight >= ( MIN_USER_RECT_SIZE * scaleKoef ).roundToInt() ) {

                            //--- установить показ этой области ( с наиболее близким и разрешенным масштабом )
                            //--- специально установлена работа с double-числами и округление в большую сторону
                            //--- из-за ошибок округления при масштабах, близких к 1
                            //--- (и scaleKoef здесь не нужен!!!)
                            val newScale = ceil( viewCoord.scale * max( 1.0 * mouseWidth / svgBodyWidth, 1.0 * mouseHeight / svgBodyHeight ) ).toInt()
                            //--- переводим в мировые координаты
                            that().refreshView( null, XyViewCoord(
                                newScale,
                                viewCoord.x1 + mouseToReal( scaleKoef, viewCoord.scale, min( mouseRect.x1, mouseRect.x2 ) ),
                                viewCoord.y1 + mouseToReal( scaleKoef, viewCoord.scale, min( mouseRect.y1, mouseRect.y2 ) ),
                                viewCoord.x1 + mouseToReal( scaleKoef, viewCoord.scale, max( mouseRect.x1, mouseRect.x2 ) ),
                                viewCoord.y1 + mouseToReal( scaleKoef, viewCoord.scale, max( mouseRect.y1, mouseRect.y2 ) )
                            ) )
                        }
                    }
                }

                MapWorkMode.DISTANCER -> {
                    val alDistancerLine = that().arrDistancerLine.unsafeCast<Array<XyElementData>>().toMutableList()
                    val alDistancerDist = that().arrDistancerDist.unsafeCast<Array<Double>>().toMutableList()
                    val alDistancerText = that().arrDistancerText.unsafeCast<Array<XyElementData>>().toMutableList()

                    //--- при первом клике заводим сумму, отключаем тулбар и включаем кнопку отмены линейки
                    if( alDistancerLine.isEmpty() ) {
                        that().distancerSumText = XyElementData(
                            type = XyElementDataType.TEXT,
                            x = mouseX,
                            y = mouseY,
                            text = "0.0",
                            pos = json(
                                "left" to "${svgBodyLeft + mouseX + 16}px",
                                "top" to "${svgBodyTop + mouseY + 16}px"
                            ),
                            style = json(
                                "position" to "absolute",
                                "color" to COLOR_XY_LABEL_TEXT,
                                "text-align" to "center",
                                "vertical-align" to "baseline",
                                "border-radius" to "${2 * scaleKoef}px",
                                "border" to "${1 * scaleKoef}px solid $COLOR_XY_LABEL_BORDER",
                                "background" to COLOR_XY_LABEL_BACK,
                                "padding" to styleXyDistancerPadding(),
                                "user-select" to if( styleIsNarrowScreen ) "none" else "auto"
                            )
                        )
                        disableToolbar( that() )
                        that().isActionCancelButtonVisible = true
                    }
                    alDistancerLine.add( XyElementData(
                        type = XyElementDataType.LINE,
                        x1 = mouseX,
                        y1 = mouseY,
                        x2 = mouseX,
                        y2 = mouseY,
                        stroke = COLOR_XY_DISTANCER,
                        strokeWidth = ( 4 * scaleKoef ).roundToInt(),
                        strokeDash = "${scaleKoef * 4},${scaleKoef * 4}"
                    ) )

                    alDistancerDist.add( 0.0 )

                    alDistancerText.add( XyElementData(
                        type = XyElementDataType.TEXT,
                        x = mouseX,
                        y = mouseY,
                        text = "0.0",
                        pos = json(
                            "left" to "${svgBodyLeft + mouseX}px",
                            "top" to "${svgBodyTop + mouseY}px"
                        ),
                        style = json(
                            "position" to "absolute",
                            "color" to COLOR_XY_LABEL_TEXT,
                            "text-align" to "center",
                            "vertical-align" to "baseline",
                            "border-radius" to "${2 * scaleKoef}px",
                            "border" to "${1 * scaleKoef}px solid $COLOR_XY_LABEL_BORDER",
                            "background" to COLOR_XY_LABEL_BACK,
                            "padding" to styleXyDistancerPadding(),
                            "user-select" to if( styleIsNarrowScreen ) "none" else "auto"
                        )
                    ) )

                    that().arrDistancerLine = alDistancerLine.toTypedArray()
                    that().arrDistancerDist = alDistancerDist.toTypedArray()
                    that().arrDistancerText = alDistancerText.toTypedArray()
                }

                MapWorkMode.SELECT_FOR_ACTION -> {
                    val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()

                    if( mouseRect.isVisible ) {
                        mouseRect.isVisible = false

                        //--- установим опцию выбора
                        val selectOption =
                            if( shiftKey ) SelectOption.ADD
                            else if( ctrlKey )  SelectOption.REVERT
                            else if( altKey ) SelectOption.DELETE
                            else SelectOption.SET

                        //--- в обычном режиме ( т.е. без доп.клавиш ) предварительно развыберем остальные элементы
                        if( selectOption == SelectOption.SET ) xyDeselectAll( that() )

                        val mouseXyRect = XyRect( min( mouseRect.x1, mouseRect.x2 ), min( mouseRect.y1, mouseRect.y2 ),
                                                  abs( mouseRect.x1 - mouseRect.x2 ), abs( mouseRect.y1 - mouseRect.y2 ) )
                        var editableElementCount = 0
                        var editElement: XyElementData? = null
                        var itMoveable = false
                        val alMoveElement = mutableListOf<XyElementData>()
                        for( element in getXyElementList( that(), mouseXyRect ) ) {
                            //if( !element.element.itReadOnly ) { - уже проверяется в getXyElementList
                            element.itSelected = when( selectOption ) {
                                SelectOption.SET,
                                SelectOption.ADD -> true
                                SelectOption.REVERT -> !element.itSelected
                                SelectOption.DELETE -> false
                            }
                            if( element.itSelected ) {
                                if( element.itEditablePoint ) {
                                    editableElementCount++
                                    editElement = element
                                }
                                if( element.itMoveable ) {
                                    itMoveable = true
                                    alMoveElement.add( element )
                                }
                            }
                        }
                        //--- проверка на возможность создания элементов при данном масштабе - пока не проверяем, т.к. геозоны можно создавать при любом масштабе
                        //for( mi in hmAddMenuEC.keys ) {
                        //    val tmpActionAddEC = hmAddMenuEC[ mi ]!!
                        //    mi.isDisable = xyModel.viewCoord.scale < tmpActionAddEC.scaleMin || xyModel.viewCoord.scale > tmpActionAddEC.scaleMax
                        //}
                        that().isEditPointButtonVisible = editableElementCount == 1
                        that().editElement = editElement
                        //--- предварительная краткая проверка на наличие выбранных передвигабельных объектов
                        that().isMoveElementsButtonVisible = itMoveable
                        that().arrMoveElement = alMoveElement.toTypedArray()
                    }
                }
                MapWorkMode.ACTION_ADD -> {
                    val addElement = that().addElement.unsafeCast<XyElementData>()

                    val actionAddPointStatus = addElement.addPoint( mouseX, mouseY )

                    if( actionAddPointStatus == AddPointStatus.COMPLETED ) that().doAddElement()
                    else that().isActionOkButtonVisible = actionAddPointStatus == AddPointStatus.COMPLETEABLE
                }
                MapWorkMode.ACTION_EDIT_POINT -> {
                    val editPointIndex = that().editPointIndex.unsafeCast<Int>()
                    if( editPointIndex != -1 ) {
                        editOnePoint( that() )
                        that().editPointIndex = -1
                    }
                }
                MapWorkMode.ACTION_MOVE -> {
                    doMoveElements( that(), xyResponse.documentConfig.name, xyResponse.startParamID, scaleKoef, viewCoord )
                    that().setMode( MapWorkMode.SELECT_FOR_ACTION )
                }
            }
            that().isMouseDown = false
        },
        "onMouseWheel" to { event: Event ->
            val wheelEvent = event as WheelEvent
            val isCtrl = wheelEvent.ctrlKey
            val mouseX = wheelEvent.offsetX.toInt()
            val mouseY = wheelEvent.offsetY.toInt()
            val deltaY = wheelEvent.deltaY.toInt()

            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            val svgBodyElement = document.getElementById( "svg_body_$tabIndex" )!!

            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            if( ( curMode == MapWorkMode.PAN && curMode == MapWorkMode.ZOOM_BOX ) && isCtrl ) {
                //--- вычисляем текущую координату середины
                //--- ( безопасным от переполнения способом )
                val curCenterX = viewCoord.x1 + ( viewCoord.x2 - viewCoord.x1 ) / 2
                val curCenterY = viewCoord.y1 + ( viewCoord.y2 - viewCoord.y1 ) / 2

                //--- сдвиг курсора мыши относительно середины в экранных координатах
                //--- ( не трогать здесь scaleKoef! )
                val sx = ( 1.0 * ( mouseX - svgBodyWidth / 2 ) / scaleKoef ).roundToInt()
                val sy = ( 1.0 * ( mouseY - svgBodyHeight / 2 ) / scaleKoef ).roundToInt()

                //--- то же самое в реальных координатах
                val curDX = sx * viewCoord.scale
                val curDY = sy * viewCoord.scale

                //--- новый сдвиг относительно центра для нового масштаба
                val newScale = checkXyScale(
                    minScale = xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin,
                    maxScale = xyResponse.documentConfig.alElementConfig.maxBy { it.second.scaleMax }!!.second.scaleMax,
                    itScaleAlign = xyResponse.documentConfig.itScaleAlign,
                    curScale = viewCoord.scale,
                    newScale = if( deltaY < 0 ) viewCoord.scale / 2 else viewCoord.scale * 2,
                    isAdaptive = false
                )

                val newDX = sx * newScale
                val newDY = sy * newScale

                //--- новые координаты середины для нового масштаба
                val newCenterX = curCenterX + curDX - newDX
                val newCenterY = curCenterY + curDY - newDY

                val newView = getXyViewCoord( newScale, svgBodyWidth, svgBodyHeight, newCenterX, newCenterY, scaleKoef )
                that().refreshView( null, newView )
            }
        },
        "setMode" to { newMode: MapWorkMode ->
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when( curMode.toString() ) {
                MapWorkMode.PAN.toString() -> that().isPanButtonDisabled = false
                MapWorkMode.ZOOM_BOX.toString() -> that().isZoomButtonDisabled = false
                MapWorkMode.DISTANCER.toString() -> {
                    that().isDistancerButtonDisabled = false
                    that().isActionCancelButtonVisible = false
                    that().arrDistancerLine = arrayOf<XyElementData>()
                    that().arrDistancerDist = arrayOf<Double>()
                    that().arrDistancerText = arrayOf<XyElementData>()
                    that().distancerSumText = null
                }
                MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                    that().isSelectButtonDisabled = false

                    that().isAddElementButtonVisible = false
                    that().isEditPointButtonVisible = false
                    that().isMoveElementsButtonVisible = false
                }
                MapWorkMode.ACTION_ADD.toString(),
                MapWorkMode.ACTION_EDIT_POINT.toString(),
                MapWorkMode.ACTION_MOVE.toString() -> {
                    enableToolbar( that() )
                }
            }

            when( newMode.toString() ) {
                MapWorkMode.PAN.toString() -> {
                    that().isPanButtonDisabled = true
//                    stackPane.cursor = Cursor.MOVE
                    xyDeselectAll( that() )
                }
                MapWorkMode.ZOOM_BOX.toString() -> {
                    that().isZoomButtonDisabled = true
//                    stackPane.cursor = Cursor.CROSSHAIR
                    xyDeselectAll( that() )
                }
                MapWorkMode.DISTANCER.toString() -> {
                    that().isDistancerButtonDisabled = true
//                    stackPane.cursor = Cursor.CROSSHAIR
                    xyDeselectAll( that() )
                }
                MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                    that().isSelectButtonDisabled = true
                    //stackPane.cursor = Cursor.DEFAULT
                    that().isAddElementButtonVisible = true
                    that().isEditPointButtonVisible = false
                    that().isMoveElementsButtonVisible = false
                    that().isActionOkButtonVisible = false
                    that().isActionCancelButtonVisible = false
                }
                MapWorkMode.ACTION_ADD.toString() -> {
                    disableToolbar( that() )
                    that().isActionOkButtonVisible = false
                    that().isActionCancelButtonVisible = true
                }
                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    disableToolbar( that() )
                    that().isActionOkButtonVisible = true
                    that().isActionCancelButtonVisible = true
                }
                MapWorkMode.ACTION_MOVE.toString() -> {
                    disableToolbar( that() )
                    that().isActionOkButtonVisible = false
                    that().isActionCancelButtonVisible = true
                }
            }
            //--- извращение для правильного сохранения enum в .data (а то в следующий раз в setMode не узнает)
            that().curMode = MapWorkMode.valueOf( newMode.toString() )
        },
        "zoomIn" to {
            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            //--- проверить масштаб
            val newScale = checkXyScale(
                minScale = xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin,
                maxScale = xyResponse.documentConfig.alElementConfig.maxBy { it.second.scaleMax }!!.second.scaleMax,
                itScaleAlign = xyResponse.documentConfig.itScaleAlign,
                curScale = viewCoord.scale,
                newScale = viewCoord.scale / 2,
                isAdaptive = false
            )

            val newViewCoord = XyViewCoord( viewCoord )
            newViewCoord.scale = newScale

            that().refreshView( null, newViewCoord )
        },
        "zoomOut" to {
            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            //--- проверить масштаб
            val newScale = checkXyScale(
                minScale = xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin,
                maxScale = xyResponse.documentConfig.alElementConfig.maxBy { it.second.scaleMax }!!.second.scaleMax,
                itScaleAlign = xyResponse.documentConfig.itScaleAlign,
                curScale = viewCoord.scale,
                newScale = viewCoord.scale * 2,
                isAdaptive = false
            )

            val newViewCoord = XyViewCoord( viewCoord )
            newViewCoord.scale = newScale

            that().refreshView( null, newViewCoord )
        },
        "startAdd" to { elementConfig: XyElementConfig ->
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            that().addElement = getXyEmptyElementData( scaleKoef, elementConfig )
            that().setMode( MapWorkMode.ACTION_ADD )
        },
        "startEditPoint" to {
            //--- в старой версии мы предварительно прятали из модели текущую (адаптированную под текущий масштаб и координаты)
            //--- версию addElement, чтобы не мешала загрузке и работе с полной версией со всеми негенерализованными точками.
            //--- учитывая, что интерактив у нас сейчас идёт только с зонами, нарисованными вручную и точки там далеки друг от друга и не подвержены генерализации,
            //--- можно считать, что загрузка полной копии редактируемого элемента не нужна
            that().setMode( MapWorkMode.ACTION_EDIT_POINT )
        },
        "startMoveElements" to {
            that().setMode( MapWorkMode.ACTION_MOVE )
        },
        "actionOk" to {
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().viewCoord.unsafeCast<XyViewCoord>()
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when( curMode.toString() ) {
                MapWorkMode.ACTION_ADD.toString() -> {
                    val addElement = that().addElement.unsafeCast<XyElementData>()
                    addElement.doAddElement( that(), xyResponse.documentConfig.name, xyResponse.startParamID, scaleKoef, viewCoord )
                    that().addElement = null
                }
                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    val editElement = that().editElement.unsafeCast<XyElementData>()
                    editElement.doEditElementPoint( that(), xyResponse.documentConfig.name, xyResponse.startParamID, scaleKoef, viewCoord )
                }
            }
            //that().refreshView( null, null ) - делается внути методов doAdd/doEdit/doMove по завершении операций
            that().setMode( MapWorkMode.SELECT_FOR_ACTION )
        },
        "actionCancel" to {
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when( curMode.toString() ) {
                MapWorkMode.DISTANCER.toString() -> {
                    //--- включить кнопки, но кнопку линейки выключить обратно
                    enableToolbar( that() )
                    that().isDistancerButtonDisabled = true
                    that().arrDistancerLine = arrayOf<XyElementData>()
                    that().arrDistancerDist = arrayOf<Double>()
                    that().arrDistancerText = arrayOf<XyElementData>()
                    that().distancerSumText = null
                    that().isActionCancelButtonVisible = false
                }
                MapWorkMode.ACTION_ADD.toString() -> {
                    that().addElement = null
                    that().refreshView( null, null )
                    that().setMode( MapWorkMode.SELECT_FOR_ACTION )
                }
                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    that().editElement = null
                    that().refreshView( null, null )
                    that().setMode( MapWorkMode.SELECT_FOR_ACTION )
                }
            }
            null
        }
    )

    this.mounted = {

//    var mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта
//        //--- на текущий момент MapSurfer - наиболее правильная карта
//        val bitmapMapMode = appContainer.getUserProperty( iCoreAppContainer.UP_BITMAP_MAP_MODE )
//        mapBitmapTypeName = if( bitmapMapMode.isNullOrEmpty() ) XyBitmapType.MS else bitmapMapMode

        //--- подготовка данных для меню добавления
        that().arrAddEC = xyResponse.documentConfig.alElementConfig.filter { it.second.descrForAction.isNotEmpty() }.map { it.second }.toTypedArray()

        doXyMounted( that(), xyResponse, tabIndex, "map_title", "map_toolbar", startExpandKoef, xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }!!.second.scaleMin )

        that().setMode( MapWorkMode.PAN )
    }

    this.data = {
        getXyComponentData().add( json(
            "curMode" to MapWorkMode.PAN,

            "isPanButtonDisabled" to true,
            "isZoomButtonDisabled" to false,
            "isDistancerButtonDisabled" to false,
            "isSelectButtonDisabled" to false,

            "isZoomInButtonDisabled" to false,
            "isZoomOutButtonDisabled" to false,

            "isRefreshButtonDisabled" to false,

            "isMouseDown" to false,

            "panPointOldX" to 0,
            "panPointOldY" to 0,
            "panDX" to 0,
            "panDY" to 0,

            "arrDistancerLine" to arrayOf<XyElementData>(),
            "arrDistancerDist" to arrayOf<Double>(),
            "arrDistancerText" to arrayOf<XyElementData>(),
            "distancerSumText" to null,

            "mouseRect" to MouseRectData( false, 0, 0, 0, 0, 1 ),

            "arrAddEC" to arrayOf<XyElementConfig>(),

            "isAddElementButtonVisible" to false,
            "isEditPointButtonVisible" to false,
            "isMoveElementsButtonVisible" to false,

            "isActionOkButtonVisible" to false,
            "isActionCancelButtonVisible" to false,

            "addElement" to null,
            "editElement" to null,
            "editPointIndex" to -1,

            "arrMoveElement" to null,
            "moveStartPoint" to null,
            "moveEndPoint" to null
        ) )
    }
}

//--- преобразование экранных координат в мировые
private fun mouseToReal( scaleKoef: Double, scale:Int, screenCoord: Int ): Int = ( screenCoord * scale / scaleKoef ).roundToInt()

private fun disableToolbar( that: dynamic ) {
    that.isPanButtonDisabled = true
    that.isZoomButtonDisabled = true
    that.isDistancerButtonDisabled = true
    that.isSelectButtonDisabled = true

    that.isZoomInButtonDisabled = true
    that.isZoomOutButtonDisabled = true

    that.isRefreshButtonDisabled = true
}

private fun enableToolbar( that: dynamic ) {
    that.isPanButtonDisabled = false
    that.isZoomButtonDisabled = false
    that.isDistancerButtonDisabled = false
    that.isSelectButtonDisabled = false

    that.isZoomInButtonDisabled = false
    that.isZoomOutButtonDisabled = false

    that.isRefreshButtonDisabled = false
}

private fun editOnePoint( that: dynamic ) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    val editElement = that.editElement.unsafeCast<XyElementData>()
    val editPointIndex = that.editPointIndex.unsafeCast<Int>()

    //--- с крайними точками незамкнутой полилинии нечего доделывать
    //if( editElement.type != XyElementDataType.POLYGON && ( editPointIndex == 0 || editPointIndex == editElement.alPoint!!.lastIndex ) ) return

    //--- берем передвигаемую, предыдущую и последующую точки
    val p0 = editElement.alPoint!![ editPointIndex ]
    val p1 = editElement.alPoint[ if( editPointIndex == 0 ) editElement.alPoint.lastIndex else editPointIndex - 1 ]
    val p2 = editElement.alPoint[ if( editPointIndex == editElement.alPoint.lastIndex ) 0 else editPointIndex + 1 ]

    //--- если рабочая точка достаточно близка к отрезку,
    //--- то считаем, что рабочая точка (почти :) лежит на отрезке,
    //--- соединяющем предыдущую и последущую точки, и ее можно удалить за ненадобностью
    val isRemovable = XyLine.distanceSeg( p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble(), p0.x.toDouble(), p0.y.toDouble() ) <= scaleKoef * 2

    //--- если точку можно удалить и элемент не является замкнутым или кол-во точек у него больше 3-х
    //--- ( т.е. даже если элемент замкнутый, то после удаления точки еще 3 точки у него останутся )
//    if( isRemovable && ( !actionElement!!.element.itClosed || actionElement!!.element.alPoint.size > 3 ) )

    //--- сейчас работаем только с полигонами. Если сейчас больше трёх точек, значит после удаления останется как минимум 3 точки, что достаточно.
    if( isRemovable && editElement.alPoint.size > 3 )
        editElement.removePoint( editPointIndex )
}

private fun doMoveElements( that: dynamic, documentTypeName: String, startParamID: String, scaleKoef: Double, viewCoord: XyViewCoord ) {
    val arrMoveElement = that.arrMoveElement.unsafeCast<Array<XyElementData>>()
    val moveStartPoint = that.moveStartPoint.unsafeCast<XyPoint>()
    val moveEndPoint = that.moveEndPoint.unsafeCast<XyPoint>()

    val xyActionRequest = XyActionRequest(
        documentTypeName = documentTypeName,
        action = XyAction.MOVE_ELEMENTS,
        startParamID = startParamID,

        alActionElementIds = arrMoveElement.map { it.elementID!! },
        dx = ( ( moveEndPoint.x - moveStartPoint.x ) * viewCoord.scale / scaleKoef ).roundToInt(),
        dy = ( ( moveEndPoint.y - moveStartPoint.y ) * viewCoord.scale / scaleKoef ).roundToInt()
    )

    that.`$root`.setWait( true )
    invokeXy(
        xyActionRequest,
        {
            that.`$root`.setWait( false )
            that.refreshView( that, null )
        }
    )
}