package foatto.core_web

import foatto.core.app.xy.*
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyDocumentConfig
import foatto.core.link.XyResponse
import org.w3c.dom.events.MouseEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Date
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.max
import kotlin.math.roundToInt

fun getXyElementTemplate( tabIndex: Int, specificSvg: String ) =
"""
    <svg id="svg_body_$tabIndex"
         width="100%"
         v-bind:height="svg_height"
         v-bind:viewBox="viewBoxBody"
         v-on:mousedown.prevent="onMousePressed( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
         v-on:mousemove="onMouseMove( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
         v-on:mouseup.prevent="onMouseReleased( false, ${'$'}event.offsetX, ${'$'}event.offsetY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
         v-on:mousewheel.prevent="onMouseWheel( ${'$'}event )"
         v-on:touchstart.prevent="onMousePressed( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
         v-on:touchmove.prevent="onMouseMove( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
         v-on:touchend.stop="onMouseReleased( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
    >

        <template v-for="arrElement in arrXyElement">
            <template v-for="element in arrElement">

                <circle v-if="element.type == '${XyElementDataType.CIRCLE}'"
                        v-bind:cx="element.x"
                        v-bind:cy="element.y"
                        v-bind:r="element.radius"
                        v-bind:stroke="element.stroke"
                        v-bind:fill="element.fill"
                        v-bind:stroke-width="element.strokeWidth"
                        v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                        v-bind:transform="element.transform"
                        v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                        v-on:mouseleave="onMouseOut()"
                />

                <ellipse v-else-if="element.type == '${XyElementDataType.ELLIPSE}'"
                         v-bind:cx="element.x"
                         v-bind:cy="element.y"
                         v-bind:rx="element.rx"
                         v-bind:ry="element.ry"
                         v-bind:stroke="element.stroke"
                         v-bind:fill="element.fill"
                         v-bind:stroke-width="element.strokeWidth"
                         v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                         v-bind:transform="element.transform"
                         v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                         v-on:mouseleave="onMouseOut()"
                />

                <image v-else-if="element.type == '${XyElementDataType.IMAGE}'"
                       v-bind:x="element.x"
                       v-bind:y="element.y"
                       v-bind:width="element.width"
                       v-bind:height="element.height"
                       v-bind:transform="element.transform"
                       v-bind:xlink:href="element.url"
                       v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                       v-on:mouseleave="onMouseOut()"
                />

                <line v-else-if="element.type == '${XyElementDataType.LINE}'"
                      v-bind:x1="element.x1"
                      v-bind:y1="element.y1"
                      v-bind:x2="element.x2"
                      v-bind:y2="element.y2"
                      v-bind:stroke="element.stroke"
                      v-bind:stroke-width="element.strokeWidth"
                      v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                      v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onMouseOut()"
                />

                <path v-else-if="element.type == '${XyElementDataType.PATH}'"
                      v-bind:d="element.points"
                      v-bind:stroke="element.stroke"
                      v-bind:fill="element.fill"
                      v-bind:stroke-width="element.strokeWidth"
                      v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                      v-bind:transform="element.transform"
                      v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onMouseOut()"
                />

                <polyline v-else-if="element.type == '${XyElementDataType.POLYLINE}'"
                          v-bind:points="element.points"
                          v-bind:stroke="element.stroke"
                          v-bind:fill="element.fill"
                          v-bind:stroke-width="element.strokeWidth"
                          v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                          v-bind:transform="element.transform"
                          v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                          v-on:mouseleave="onMouseOut()"
                />

                <polygon v-else-if="element.type == '${XyElementDataType.POLYGON}'"
                         v-bind:points="element.points"
                         v-bind:stroke="element.itSelected ? '$COLOR_XY_ZONE_BORDER' : element.stroke"
                         v-bind:fill="element.fill"
                         v-bind:stroke-width="element.strokeWidth"
                         v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                         v-bind:transform="element.transform"
                         v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                         v-on:mouseleave="onMouseOut()"
                />

                <rect v-else-if="element.type == '${XyElementDataType.RECT}'"
                      v-bind:x="element.x"
                      v-bind:y="element.y"
                      v-bind:width="element.width"
                      v-bind:height="element.height"
                      v-bind:stroke="element.stroke"
                      v-bind:fill="element.fill"
                      v-bind:stroke-width="element.strokeWidth"
                      v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                      v-bind:rx="element.rx"
                      v-bind:ry="element.ry"
                      v-bind:transform="element.transform"
                      v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onMouseOut()"
                />

            </template>
        </template>
""" +

specificSvg +

"""
    </svg>

    <template v-for="arrElement in arrXyElement">
        <template v-for="element in arrElement">
            <template v-if="element.type == '${XyElementDataType.TEXT}'">
                <div v-show="element.isVisible"
                     v-bind:style="[element.pos, element.style]"
                     v-on:mouseenter="onMouseOver( ${'$'}event, element )"
                     v-on:mouseleave="onMouseOut()"
                     v-html="element.text"
                >
                </div>
            </template>
        </template>
    </template>

    <div v-show="tooltipVisible"
         v-bind:style="[style_tooltip_text, style_tooltip_pos]"
         v-html="tooltipText"
    >
    </div>

"""

fun doXyMounted( that: dynamic, xyResponse: XyResponse, tabIndex: Int, titleBarNamePrefix: String, toolBarNamePrefix: String, startExpandKoef: Double, curScale: Int ) {
    that.documentConfig = xyResponse.documentConfig
    that.startParamID = xyResponse.startParamID
    that.fullTitle = xyResponse.fullTitle
    that.parentObjectID = xyResponse.parentObjectID
    that.parentObjectInfo = xyResponse.parentObjectInfo

    that.`$root`.addTabInfo( tabIndex, xyResponse.shortTitle, xyResponse.fullTitle )

    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    //--- принудительная установка полной высоты svg-элементов
    //--- (BUG: иначе высота либо равна 150px - если не указывать высоту,
    //--- либо равно width, если указать height="100%")
    val svgTabPanel = document.getElementById( "tab_panel" )!!
    val svgTitlebar = document.getElementById( "${titleBarNamePrefix}_$tabIndex" )!!
    val svgToolbar = document.getElementById( "${toolBarNamePrefix}_$tabIndex" )!!

    that.svg_height = kotlin.browser.window.innerHeight - ( svgTabPanel.clientHeight + svgTitlebar.clientHeight + svgToolbar.clientHeight )

    that.`$root`.setWait( true )
    invokeXy(
        XyActionRequest(
            documentTypeName = xyResponse.documentConfig.name,
            action = XyAction.GET_COORDS,
            startParamID = xyResponse.startParamID ),
        { xyActionResponse: XyActionResponse ->

            val svgBodyElement = document.getElementById( "svg_body_$tabIndex" )!!
            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            setXyViewBoxBody( that, intArrayOf( 0, 0, svgBodyWidth, svgBodyHeight ) )

            val newViewCoord = getXyCoordsDone(
                scaleKoef = scaleKoef,
                startExpandKoef = startExpandKoef,
                viewWidth = svgBodyWidth,
                viewHeight = svgBodyHeight,
                curScale = curScale,
                minCoord = xyActionResponse.minCoord!!,
                maxCoord = xyActionResponse.maxCoord!!
            )

            //--- именно до refreshView, чтобы не сбросить сразу после включения
            that.`$root`.setWait( false )
            that.refreshView( that, newViewCoord )
        }
    )
}

fun readXyElements(
    documentConfig: XyDocumentConfig,
    viewCoord: XyViewCoord,
    scaleKoef: Double,
    alElement: Array<XyElement>
): Array<Array<XyElementData>> {

    val hmLayer = mutableMapOf<Int,MutableList<XyElementData>>()
    alElement.forEach { element ->
        val elementConfig = documentConfig.alElementConfig.find { it.first == element.typeName }!!.second
        val alLayer = hmLayer.getOrPut( elementConfig.layer ) { mutableListOf() }

        readXyElementData( scaleKoef, viewCoord, elementConfig, element, alLayer )
    }

    return hmLayer.toList().sortedBy { it.first }.map { it.second.toTypedArray() }.toTypedArray()
}

fun getXyViewBoxBody( that: dynamic ): IntArray {
    val sViewBox = that.viewBoxBody.unsafeCast<String>()
    return sViewBox.split( ' ' ).map { it.toInt() }.toIntArray()
}

fun setXyViewBoxBody( that: dynamic, arrViewBox: IntArray ) {
    that.viewBoxBody = "${arrViewBox[ 0 ]} ${arrViewBox[ 1 ]} ${arrViewBox[ 2 ]} ${arrViewBox[ 3 ]}"
}

fun getXyCoordsDone(scaleKoef: Double, startExpandKoef: Double, viewWidth: Int, viewHeight: Int, curScale: Int, minCoord: XyPoint, maxCoord: XyPoint ): XyViewCoord {
    var x1 = minCoord.x
    var y1 = minCoord.y
    var x2 = maxCoord.x
    var y2 = maxCoord.y

    val scale: Int
    val tmpW = x2 - x1
    val tmpH = y2 - y1
    //--- если пришли граничные координаты только одной точки,
    //--- то оставим текущий масштаб
    if( tmpW == 0 && tmpH == 0 ) scale = curScale
    else {
        //--- прибавим по краям startExpandKoef, чтобы искомые элементы не тёрлись об края экрана
        x1 -= ( tmpW * startExpandKoef ).toInt()
        y1 -= ( tmpH * startExpandKoef ).toInt()
        x2 += ( tmpW * startExpandKoef ).toInt()
        y2 += ( tmpH * startExpandKoef ).toInt()
        //--- масштаб вычисляется исходя из размеров docView (аналогично zoomBox)
        scale = calcXyScale( scaleKoef, viewWidth, viewHeight, x1, y1, x2, y2 )
    }
    return XyViewCoord( scale, x1, y1, x2, y2 )
}

fun calcXyScale( scaleKoef: Double, viewWidth: Int, viewHeight: Int, x1: Int, y1: Int, x2: Int, y2: Int ): Int =
    max( ( x2 - x1 ) * scaleKoef / viewWidth, ( y2 - y1 ) * scaleKoef / viewHeight ).roundToInt()

fun getXyViewCoord( aScale: Int, aViewWidth: Int, aViewHeight: Int, aCenterX: Int, aCenterY: Int, scaleKoef: Double ): XyViewCoord {
    val vc = XyViewCoord()

    vc.scale = aScale

    val rw = ( aViewWidth * aScale / scaleKoef ).roundToInt()
    val rh = ( aViewHeight * aScale / scaleKoef ).roundToInt()

    vc.x1 = aCenterX - rw / 2
    vc.y1 = aCenterY - rh / 2
    //--- чтобы избежать неточностей из-за целочисленного деления нечетных чисел пополам,
    //--- правую/нижнюю границу получим в виде ( t1 + rw ), а не в виде ( newCenterX + rw / 2 )
    vc.x2 = vc.x1 + rw
    vc.y2 = vc.y1 + rh

    return vc
}

//--- проверка масштаба на минимальный/максимальный и на кратность степени двойки
fun checkXyScale( minScale: Int, maxScale: Int, itScaleAlign: Boolean, curScale: Int, newScale: Int, isAdaptive: Boolean ): Int {

    if( newScale < minScale ) return minScale
    if( newScale > maxScale ) return maxScale

    //--- нужно ли выравнивание масштаба к степени двойки?
    if( itScaleAlign ) {
        //--- ПРОТЕСТИРОВАНО: нельзя допускать наличие масштабов, не являющихся степенью двойки,
        //--- иначе при приведении (растягивании/сжимании) произвольного масштаба к выровненному (степени 2)
        //--- получается битмап-карта отвратного качества

        //--- адаптивный алгоритм - "докручивает" масштаб до ожидаемого пользователем
        if( isAdaptive ) {
            //--- если идёт процесс увеличения масштаба (удаление от пользователя),
            //--- то поможем ему - округлим масштаб в бОльшую сторону
            if( newScale >= curScale ) {
                var scale = minScale
                while( scale <= maxScale ) {
                    if( newScale <= scale ) return scale
                    scale *= 2
                }
            }
            //--- иначе (если идёт процесс уменьшения масштаба - приближение к пользователю),
            //--- то поможем ему - округлим масштаб в меньшую сторону
            else {
                var scale = maxScale
                while( scale >= minScale ) {
                    if( newScale >= scale ) return scale
                    scale /= 2
                }
            }
        }
        //--- обычный алгоритм - просто даёт больший или равный масштаб, чтобы всё гарантированно уместилось
        else {
            var scale = minScale
            while( scale <= maxScale ) {
                if( newScale <= scale ) return scale
                scale *= 2
            }
        }
    }
    else return newScale
    //--- такого быть не должно, но всё-таки для проверки вернём 0, чтобы получить деление на 0
    return 0   //XyConfig.MAX_SCALE;
}

fun setXyTextOffset( that: dynamic, svgBodyLeft: Int, svgBodyTop: Int ) {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    val arrViewBoxBody = getXyViewBoxBody( that )

    for( arrLayer in arrXyElement ) {
        for( xyElement in arrLayer ) {
            if( xyElement.type == XyElementDataType.TEXT ) {
                val newX = xyElement.x!! - arrViewBoxBody[ 0 ]
                val newY = xyElement.y!! - arrViewBoxBody[ 1 ]

                xyElement.isVisible = newX >= 0 && newY >= 0 && newX < arrViewBoxBody[ 2 ] && newY < arrViewBoxBody[ 3 ]

                xyElement.pos = json(
                    "left" to "${svgBodyLeft + newX}px",
                    "top" to "${svgBodyTop + newY}px"
                )
            }
        }
    }
}

fun onXyMouseOver( that: dynamic, mouseEvent: MouseEvent, xyElement: XyElementData ) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

    if( ! xyElement.tooltip.isNullOrBlank() ) {
        val tooltipX = mouseEvent.clientX + ( 16 * scaleKoef ).roundToInt()
        val tooltipY = mouseEvent.clientY + ( 16 * scaleKoef ).roundToInt()

        that.tooltipVisible = true
        that.tooltipText = xyElement.tooltip.replace( "\n", "<br>" )
        that.style_tooltip_pos = json( "left" to "${tooltipX}px", "top" to "${tooltipY}px" )
        that.tooltipOffTime = Date().getTime() + 3000
    }
    else {
        that.tooltipVisible = false
    }
}

fun onXyMouseOut( that: dynamic ) {
    //--- через 3 сек выключить тултип, если не было других активаций тултипов
    //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
    //--- причём после ухода с графика других mouseleave не вызывается.
    window.setTimeout( {
        val tooltipOffTime = that.tooltipOffTime.unsafeCast<Double>()
        if( Date().getTime() > tooltipOffTime ) {
            that.tooltipVisible = false
        }
    }, 3000 )
}

fun getXyElements( that: dynamic, xyResponse: XyResponse, scaleKoef: Double, newView: XyViewCoord, mapBitmapTypeName: String, svgBodyLeft: Int, svgBodyTop: Int ) {
    that.`$root`.setWait( true )
    invokeXy(
        XyActionRequest(
            documentTypeName = xyResponse.documentConfig.name,
            action = XyAction.GET_ELEMENTS,
            startParamID = xyResponse.startParamID,
            viewCoord = newView,
            bitmapTypeName = mapBitmapTypeName
        ),
        { xyActionResponse: XyActionResponse ->

            //--- сбрасываем горизонтальный и вертикальный скроллинг/смещение
            val arrViewBoxBody = getXyViewBoxBody( that )
            setXyViewBoxBody( that, intArrayOf( 0, 0, arrViewBoxBody[ 2 ], arrViewBoxBody[ 3 ] ) )

            that.arrXyElement = readXyElements(
                documentConfig = xyResponse.documentConfig,
                viewCoord = newView,
                scaleKoef = scaleKoef,
                alElement = xyActionResponse.alElement!!
            )

            setXyTextOffset( that, svgBodyLeft, svgBodyTop )

            that.`$root`.setWait( false )
        }
    )
}

//--- Возвращает список выбранных элементов
//fun getXySelectedElementList( that: dynamic ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            if( xyElement.itSelected )
//                alResult.add( xyElement )
//        }
//    }
//    return alResult.asReversed()
//}

//fun getXyElementList( that: dynamic, x: Int, y: Int ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            //--- небольшой хак: список элементов нужен только для интерактива, поэтмоу прежде чем тратить время на проверки геометрии - проверяем, а надо ли вообще проверять
//            if( !xyElement.itReadOnly && xyElement.isContains( x, y ))
//                alResult.add( xyElement )
//        }
//    }
//
//    return alResult.asReversed()
//}

fun getXyElementList( that: dynamic, rect: XyRect): List<XyElementData> {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    val alResult = mutableListOf<XyElementData>()
    arrXyElement.forEach { arrXyElementIn ->
        arrXyElementIn.forEach { xyElement ->
            //--- небольшой хак: список элементов нужен только для интерактива, поэтмоу прежде чем тратить время на проверки геометрии - проверяем, а надо ли вообще проверять
            if( !xyElement.itReadOnly && xyElement.isIntersects( rect ) )
                alResult.add( xyElement )
        }
    }

    return alResult.asReversed()
}

fun xyDeselectAll( that: dynamic ) {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    arrXyElement.forEach { arrXyElementIn ->
        arrXyElementIn.forEach { xyElement ->
            xyElement.itSelected = false
        }
    }
}

fun getXyClickRect( mouseX: Int, mouseY: Int ): XyRect = XyRect( mouseX - MIN_USER_RECT_SIZE / 2, mouseY - MIN_USER_RECT_SIZE / 2, MIN_USER_RECT_SIZE, MIN_USER_RECT_SIZE )

fun getXyComponentData(): Json {
    return json(
        "documentConfig" to null,
        "startParamID" to "",
        "fullTitle" to "",
        "parentObjectID" to 0,
        "parentObjectInfo" to "",

        "svg_height" to "100%",

        "viewCoord" to XyViewCoord( 1, 0, 0, 1, 1 ),        // XyViewCoord( 1, 0, 0, 0, 0 ), в StateControl ???
        "arrXyElement" to arrayOf<Array<XyElementData>>(),

        "viewBoxBody" to "0 0 1 1",
        "tooltipOffTime" to 0.0,

        "tooltipVisible" to false,
        "tooltipText" to "",

        "style_header" to json(
            "border-top" to if( !styleIsNarrowScreen ) "none" else "1px solid $COLOR_BUTTON_BORDER"
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
        "style_title" to json (
            "font-size" to styleControlTitleTextFontSize(),
            "padding" to styleControlTitlePadding()
        ),
        "style_text_button" to json(
            "background" to COLOR_BUTTON_BACK,
            "border" to "1px solid $COLOR_BUTTON_BORDER",
            "border-radius" to BORDER_RADIUS,
            "font-size" to styleCommonButtonFontSize(),
            "padding" to styleTextButtonPadding(),//styleCommonEditorPadding(),
            "margin" to styleCommonMargin(),
            "cursor" to "pointer"
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
        "style_tooltip_text" to json(
            "position" to "absolute",
            "color" to COLOR_XY_LABEL_TEXT,
            "background" to COLOR_XY_LABEL_BACK,
            "border" to "1px solid $COLOR_XY_LABEL_BORDER",
            "border-radius" to BORDER_RADIUS,
            "padding" to styleControlTooltipPadding(),
            "user-select" to if( styleIsNarrowScreen ) "none" else "auto"
        ),
        "style_tooltip_pos" to json(
            "left" to "",
            "top" to ""
        )
    )

}