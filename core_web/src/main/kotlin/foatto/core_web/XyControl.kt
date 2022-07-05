package foatto.core_web

import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyDocumentConfig
import foatto.core.link.XyResponse
import foatto.core.link.XyServerActionButton
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.MouseEvent
import kotlin.js.Date
import kotlin.js.json
import kotlin.math.max
import kotlin.math.roundToInt

@Suppress("UnsafeCastFromDynamic")
fun getXyElementTemplate(
    tabId: Int,
    withInteractive: Boolean,
    specificSvg: String = "",
) =
    """
    <svg id="xy_svg_body_$tabId"
         width="100%"
         v-bind:height="xy_svg_height"
         v-bind:viewBox="xyViewBoxBody"
         v-on:mousedown.prevent="onXyMousePressed( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
         v-on:mousemove="onXyMouseMove( false, ${'$'}event.offsetX, ${'$'}event.offsetY )"
         v-on:mouseup.prevent="onXyMouseReleased( false, ${'$'}event.offsetX, ${'$'}event.offsetY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
        """ +

        if (withInteractive) {
            """
                v-on:mousewheel.prevent="onXyMouseWheel( ${'$'}event )"
            """
        } else {
            ""
        } +

        """
         v-on:touchstart.prevent="onXyMousePressed( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
         v-on:touchmove.prevent="onXyMouseMove( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY )"
         v-on:touchend.stop="onXyMouseReleased( true, ${'$'}event.changedTouches[0].clientX, ${'$'}event.changedTouches[0].clientY, ${'$'}event.shiftKey, ${'$'}event.ctrlKey, ${'$'}event.altKey )"
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
                        v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                        v-on:mouseleave="onXyMouseOut()"
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
                         v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                         v-on:mouseleave="onXyMouseOut()"
                />

                <image v-else-if="element.type == '${XyElementDataType.IMAGE}'"
                       v-bind:x="element.x"
                       v-bind:y="element.y"
                       v-bind:width="element.width"
                       v-bind:height="element.height"
                       v-bind:transform="element.transform"
                       v-bind:xlink:href="element.url"
                       v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                       v-on:mouseleave="onXyMouseOut()"
                />

                <line v-else-if="element.type == '${XyElementDataType.LINE}'"
                      v-bind:x1="element.x1"
                      v-bind:y1="element.y1"
                      v-bind:x2="element.x2"
                      v-bind:y2="element.y2"
                      v-bind:stroke="element.stroke"
                      v-bind:stroke-width="element.strokeWidth"
                      v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                      v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onXyMouseOut()"
                />

                <path v-else-if="element.type == '${XyElementDataType.PATH}'"
                      v-bind:d="element.points"
                      v-bind:stroke="element.stroke"
                      v-bind:fill="element.fill"
                      v-bind:stroke-width="element.strokeWidth"
                      v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                      v-bind:transform="element.transform"
                      v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onXyMouseOut()"
                />

                <polyline v-else-if="element.type == '${XyElementDataType.POLYLINE}'"
                          v-bind:points="element.points"
                          v-bind:stroke="element.stroke"
                          v-bind:fill="element.fill"
                          v-bind:stroke-width="element.strokeWidth"
                          v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                          v-bind:transform="element.transform"
                          v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                          v-on:mouseleave="onXyMouseOut()"
                />

                <polygon v-else-if="element.type == '${XyElementDataType.POLYGON}'"
                         v-bind:points="element.points"
                         v-bind:stroke="element.itSelected ? '$COLOR_XY_ZONE_BORDER' : element.stroke"
                         v-bind:fill="element.fill"
                         v-bind:stroke-width="element.strokeWidth"
                         v-bind:stroke-dasharray="element.itSelected ? element.strokeDash : ''"
                         v-bind:transform="element.transform"
                         v-bind:style="element.style"
                         v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                         v-on:mouseleave="onXyMouseOut()"
                         v-on:mousedown.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
                         v-on:touchstart.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
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
                      v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onXyMouseOut()"
                />

                <text v-else-if="element.type == '${XyElementDataType.SVG_TEXT}'"
                      v-bind:x="element.x"
                      v-bind:y="element.y"
                      v-bind:fill="element.stroke"
                      v-bind:text-anchor="element.hAnchor"
                      v-bind:dominant-baseline="element.vAnchor"
                      v-bind:transform="element.transform"
                      v-bind:style="element.style"
                      v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                      v-on:mouseleave="onXyMouseOut()"
                      v-on:mousedown.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
                      v-on:touchstart.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
                >
                    {{ element.text }}
                </text>
            </template>
        </template>
""" +

        specificSvg +

        """
    </svg>

    <template v-for="arrElement in arrXyElement">
        <template v-for="element in arrElement">
            <template v-if="element.type == '${XyElementDataType.HTML_TEXT}'">
                <div v-show="element.isVisible"
                     v-bind:style="[element.pos, element.style]"
                     v-on:mouseenter="onXyMouseOver( ${'$'}event, element )"
                     v-on:mouseleave="onXyMouseOut()"
                     v-on:mousedown.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
                     v-on:touchstart.prevent="element.itReadOnly ? null : onXyTextPressed( ${'$'}event, element )"
                     v-html="element.text"
                >
                </div>
            </template>
        </template>
    </template>

    <div v-show="xyTooltipVisible"
         v-bind:style="[style_xy_tooltip_text, style_xy_tooltip_pos]"
         v-html="xyTooltipText"
    >
    </div>

"""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class XySvgCoords(
    val bodyLeft: Int,
    val bodyTop: Int,
    val bodyWidth: Int,
    val bodyHeight: Int,
)

fun defineXySvgCoords(
    tabId: Int,
    elementPrefix: String,
    arrAddElements: Array<Element>,
): XySvgCoords {

    val menuBarElement = document.getElementById(MENU_BAR_ID)

    val svgTabPanel = document.getElementById("tab_panel")!!
    val svgXyTitle = document.getElementById("${elementPrefix}_title_$tabId")!!
    val svgXyToolbar = document.getElementById("${elementPrefix}_toolbar_$tabId")!!

    val svgBodyElement = document.getElementById("xy_svg_body_$tabId")!!

    val menuBarWidth = menuBarElement?.clientWidth ?: 0

    return XySvgCoords(
        bodyLeft = menuBarWidth,  //svgBodyElement.clientLeft - BUG: всегда даёт 0
        //--- svgBodyElement.clientTop - BUG: всегда даёт 0
        bodyTop = svgTabPanel.clientHeight + svgXyTitle.clientHeight + svgXyToolbar.clientHeight + arrAddElements.sumOf { it.clientHeight },
        bodyWidth = svgBodyElement.clientWidth,
        bodyHeight = svgBodyElement.clientHeight,
    )
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun doXyMounted(
    that: dynamic,
    xyResponse: XyResponse,
    tabId: Int,
    elementPrefix: String,
    startExpandKoef: Double,
    isCentered: Boolean,
    curScale: Int,
) {
    that.`$root`.setTabInfo(tabId, xyResponse.shortTitle, xyResponse.fullTitle)
    that.arrTitle = xyResponse.fullTitle.split('\n').filter { it.isNotBlank() }.toTypedArray()

    doXySpecificComponentMounted(
        that = that,
        xyResponse = xyResponse,
        tabId = tabId,
        elementPrefix = elementPrefix,
        startExpandKoef = startExpandKoef,
        isCentered = isCentered,
        curScale = curScale,
        svgHeight = null,
        arrAddElements = emptyArray(),
    )
}

fun doXySpecificComponentMounted(
    that: dynamic,
    xyResponse: XyResponse,
    tabId: Int,
    elementPrefix: String,
    startExpandKoef: Double,
    isCentered: Boolean,
    curScale: Int,
    svgHeight: Int?,
    arrAddElements: Array<Element>,
) {
    that.xyDocumentConfig = xyResponse.documentConfig
    that.xyStartParamId = xyResponse.startParamId

    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    //--- принудительная установка полной высоты svg-элементов
    //--- (BUG: иначе высота либо равна 150px - если не указывать высоту,
    //--- либо равно width, если указать height="100%")
    val svgCoords = defineXySvgCoords(tabId, elementPrefix, arrAddElements)
    that.xy_svg_height = svgHeight ?: (window.innerHeight - svgCoords.bodyTop)

    that.`$root`.setWait(true)
    invokeXy(
        XyActionRequest(
            documentTypeName = xyResponse.documentConfig.name,
            action = XyAction.GET_COORDS,
            startParamId = xyResponse.startParamId
        ),
        { xyActionResponse: XyActionResponse ->

            val svgBodyElement = document.getElementById("xy_svg_body_$tabId")!!
            val svgBodyWidth = svgBodyElement.clientWidth
            val svgBodyHeight = svgBodyElement.clientHeight

            setXyViewBoxBody(that, arrayOf(0, 0, svgBodyWidth, svgBodyHeight))

            val newViewCoord = getXyCoordsDone(
                scaleKoef = scaleKoef,
                startExpandKoef = startExpandKoef,
                isCentered = isCentered,
                viewWidth = svgBodyWidth,
                viewHeight = svgBodyHeight,
                curScale = curScale,
                minCoord = xyActionResponse.minCoord!!,
                maxCoord = xyActionResponse.maxCoord!!
            )

            //--- именно до xyRefreshView, чтобы не сбросить сразу после включения
            that.`$root`.setWait(false)
            that.xyRefreshView(that, newViewCoord, true) as Unit
        }
    )
}

fun readXyElements(
    documentConfig: XyDocumentConfig,
    viewCoord: XyViewCoord,
    scaleKoef: Double,
    arrElement: Array<XyElement>
): Array<Array<XyElementData>> {

    val hmLayer = mutableMapOf<Int, MutableList<XyElementData>>()
    arrElement.forEach { element ->
        val elementConfig = documentConfig.alElementConfig.find { it.first == element.typeName }!!.second
        val alLayer = hmLayer.getOrPut(elementConfig.layer) { mutableListOf() }

        readXyElementData(scaleKoef, viewCoord, elementConfig, element, alLayer)
    }

    return hmLayer.toList().sortedBy { it.first }.map { it.second.toTypedArray() }.toTypedArray()
}

fun getXyViewBoxBody(that: dynamic): IntArray {
    val sViewBox = that.xyViewBoxBody.unsafeCast<String>()
    return sViewBox.split(' ').map { it.toInt() }.toIntArray()
}

fun setXyViewBoxBody(that: dynamic, arrViewBox: Array<Int>) {
    that.xyViewBoxBody = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
}

fun getXyCoordsDone(
    scaleKoef: Double,
    startExpandKoef: Double,
    isCentered: Boolean,
    viewWidth: Int,
    viewHeight: Int,
    curScale: Int,
    minCoord: XyPoint,
    maxCoord: XyPoint
): XyViewCoord {

    var x1 = minCoord.x
    var y1 = minCoord.y
    var x2 = maxCoord.x
    var y2 = maxCoord.y

    val tmpW = x2 - x1
    val tmpH = y2 - y1
    //--- если пришли граничные координаты только одной точки,
    //--- то оставим текущий масштаб
    val scale = if (tmpW == 0 && tmpH == 0) {
        curScale
    } else {
        //--- прибавим по краям startExpandKoef, чтобы искомые элементы не тёрлись об края экрана
        x1 -= (tmpW * startExpandKoef).toInt()
        y1 -= (tmpH * startExpandKoef).toInt()
        x2 += (tmpW * startExpandKoef).toInt()
        y2 += (tmpH * startExpandKoef).toInt()
        //--- масштаб вычисляется исходя из размеров docView (аналогично zoomBox)
        calcXyScale(scaleKoef, viewWidth, viewHeight, x1, y1, x2, y2)
    }

    if (isCentered) {
        val fullXyWidth = (viewWidth * scale / scaleKoef).toInt()
        val restXyWidth = fullXyWidth - (x2 - x1)
        x1 -= restXyWidth / 2
        x2 -= restXyWidth / 2

        val fullXyHeight = (viewHeight * scale / scaleKoef).toInt()
        val restXyHeight = fullXyHeight - (y2 - y1)
        y1 -= restXyHeight / 2
        y2 -= restXyHeight / 2
    }

    return XyViewCoord(scale, x1, y1, x2, y2)
}

fun calcXyScale(scaleKoef: Double, viewWidth: Int, viewHeight: Int, x1: Int, y1: Int, x2: Int, y2: Int): Int =
    max((x2 - x1) * scaleKoef / viewWidth, (y2 - y1) * scaleKoef / viewHeight).roundToInt()

fun getXyViewCoord(aScale: Int, aViewWidth: Int, aViewHeight: Int, aCenterX: Int, aCenterY: Int, scaleKoef: Double): XyViewCoord {
    val vc = XyViewCoord()

    vc.scale = aScale

    val rw = (aViewWidth * aScale / scaleKoef).roundToInt()
    val rh = (aViewHeight * aScale / scaleKoef).roundToInt()

    vc.x1 = aCenterX - rw / 2
    vc.y1 = aCenterY - rh / 2
    //--- чтобы избежать неточностей из-за целочисленного деления нечетных чисел пополам,
    //--- правую/нижнюю границу получим в виде ( t1 + rw ), а не в виде ( newCenterX + rw / 2 )
    vc.x2 = vc.x1 + rw
    vc.y2 = vc.y1 + rh

    return vc
}

//--- проверка масштаба на минимальный/максимальный и на кратность степени двойки
fun checkXyScale(minScale: Int, maxScale: Int, itScaleAlign: Boolean, curScale: Int, newScale: Int, isAdaptive: Boolean): Int {

    if (newScale < minScale) return minScale
    if (newScale > maxScale) return maxScale

    //--- нужно ли выравнивание масштаба к степени двойки?
    if (itScaleAlign) {
        //--- ПРОТЕСТИРОВАНО: нельзя допускать наличие масштабов, не являющихся степенью двойки,
        //--- иначе при приведении (растягивании/сжимании) произвольного масштаба к выровненному (степени 2)
        //--- получается битмап-карта отвратного качества

        //--- адаптивный алгоритм - "докручивает" масштаб до ожидаемого пользователем
        if (isAdaptive) {
            //--- если идёт процесс увеличения масштаба (удаление от пользователя),
            //--- то поможем ему - округлим масштаб в бОльшую сторону
            if (newScale >= curScale) {
                var scale = minScale
                while (scale <= maxScale) {
                    if (newScale <= scale) return scale
                    scale *= 2
                }
            }
            //--- иначе (если идёт процесс уменьшения масштаба - приближение к пользователю),
            //--- то поможем ему - округлим масштаб в меньшую сторону
            else {
                var scale = maxScale
                while (scale >= minScale) {
                    if (newScale >= scale) return scale
                    scale /= 2
                }
            }
        }
        //--- обычный алгоритм - просто даёт больший или равный масштаб, чтобы всё гарантированно уместилось
        else {
            var scale = minScale
            while (scale <= maxScale) {
                if (newScale <= scale) return scale
                scale *= 2
            }
        }
    } else return newScale
    //--- такого быть не должно, но всё-таки для проверки вернём 0, чтобы получить деление на 0
    return 0   //XyConfig.MAX_SCALE;
}

fun setXyTextOffset(that: dynamic, svgBodyLeft: Int, svgBodyTop: Int) {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    val arrViewBoxBody = getXyViewBoxBody(that)

    for (arrLayer in arrXyElement) {
        for (xyElement in arrLayer) {
            if (xyElement.type == XyElementDataType.HTML_TEXT) {
                val newX = xyElement.x!! - arrViewBoxBody[0]
                val newY = xyElement.y!! - arrViewBoxBody[1]

                xyElement.isVisible = newX >= 0 && newY >= 0 && newX < arrViewBoxBody[2] && newY /*- xyOffsY*/ < arrViewBoxBody[3]

                xyElement.pos = json(
                    "left" to "${svgBodyLeft + newX}px",
                    "top" to "${svgBodyTop + newY}px",
                )
            }
        }
    }
}

fun onXyMouseOver(that: dynamic, mouseEvent: MouseEvent, xyElement: XyElementData) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()

    if (!xyElement.tooltip.isNullOrBlank()) {
        val tooltipX = mouseEvent.clientX + (16 * scaleKoef).roundToInt()
        val tooltipY = mouseEvent.clientY + (16 * scaleKoef).roundToInt()

        that.xyTooltipVisible = true
        that.xyTooltipText = xyElement.tooltip.replace("\n", "<br>")
        that.style_xy_tooltip_pos = json("left" to "${tooltipX}px", "top" to "${tooltipY}px")
        that.xyTooltipOffTime = Date().getTime() + 3000
    } else {
        that.xyTooltipVisible = false
    }
}

fun onXyMouseOut(that: dynamic) {
    //--- через 3 сек выключить тултип, если не было других активаций тултипов
    //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
    //--- причём после ухода с графика других mouseleave не вызывается.
    window.setTimeout({
        val tooltipOffTime = that.xyTooltipOffTime.unsafeCast<Double>()
        if (Date().getTime() > tooltipOffTime) {
            that.xyTooltipVisible = false
        }
    }, 3000)
}

fun getXyElements(
    that: dynamic,
    xyResponse: XyResponse,
    scaleKoef: Double,
    newView: XyViewCoord,
    mapBitmapTypeName: String,
    svgBodyLeft: Int,
    svgBodyTop: Int,
    withWait: Boolean,
    doAdditionalWork: (aThat: dynamic, xyActionResponse: XyActionResponse) -> Unit = { _: dynamic, _: XyActionResponse -> },
) {
    if (withWait) {
        that.`$root`.setWait(true)
    }
    invokeXy(
        XyActionRequest(
            documentTypeName = xyResponse.documentConfig.name,
            action = XyAction.GET_ELEMENTS,
            startParamId = xyResponse.startParamId,
            viewCoord = newView,
            bitmapTypeName = mapBitmapTypeName
        ),
        { xyActionResponse: XyActionResponse ->

            //--- сбрасываем горизонтальный и вертикальный скроллинг/смещение
            val arrViewBoxBody = getXyViewBoxBody(that)
            setXyViewBoxBody(that, arrayOf(0, 0, arrViewBoxBody[2], arrViewBoxBody[3]))

            that.arrXyElement = readXyElements(
                documentConfig = xyResponse.documentConfig,
                viewCoord = newView,
                scaleKoef = scaleKoef,
                arrElement = xyActionResponse.arrElement!!
            )

            setXyTextOffset(that, svgBodyLeft, svgBodyTop)

            if (withWait) {
                that.`$root`.setWait(false)
            }

            doAdditionalWork(that, xyActionResponse)
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

fun getXyElementList(that: dynamic, rect: XyRect, isCollectEditableOnly: Boolean): List<XyElementData> {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    return arrXyElement.flatten().filter { xyElement ->
        isCollectEditableOnly.xor(xyElement.itReadOnly) && xyElement.isIntersects(rect)
    }.asReversed()
}

fun xyDeselectAll(that: dynamic) {
    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
    arrXyElement.forEach { arrXyElementIn ->
        arrXyElementIn.forEach { xyElement ->
            xyElement.itSelected = false
        }
    }
}

fun getXyClickRect(mouseX: Int, mouseY: Int): XyRect = XyRect(mouseX - MIN_USER_RECT_SIZE / 2, mouseY - MIN_USER_RECT_SIZE / 2, MIN_USER_RECT_SIZE, MIN_USER_RECT_SIZE)

fun getXyComponentData() = json(
    "arrTitle" to arrayOf<String>(),

    "refreshInterval" to 0,
    "refreshHandlerId" to 0,

    "style_header" to json(
        "border-top" to if (!styleIsNarrowScreen) {
            "none"
        } else {
            "1px solid $colorMainBorder"
        }
    ),
    "style_toolbar" to json(
        "display" to "flex",
        "flex-direction" to "row",
        "flex-wrap" to "wrap",
        "justify-content" to "space-between",
        "align-items" to "center",        // "baseline" ?
        "padding" to styleControlPadding(),
        "background" to colorMainBack1
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
        "padding" to styleControlTitlePadding(),
        "display" to "flex",
        "flex-direction" to "column",
    ),
    "style_text_button" to json(
        "background" to colorButtonBack,
        "border" to "1px solid $colorButtonBorder",
        "border-radius" to styleButtonBorderRadius,
        "font-size" to styleCommonButtonFontSize(),
        "padding" to styleTextButtonPadding(),//styleCommonEditorPadding(),
        "margin" to styleCommonMargin(),
        "cursor" to "pointer"
    ),
    "style_icon_button" to json(
        "background" to colorButtonBack,
        "border" to "1px solid $colorButtonBorder",
        "border-radius" to styleButtonBorderRadius,
        "font-size" to styleCommonButtonFontSize(),
        "padding" to styleIconButtonPadding(),
        "margin" to styleCommonMargin(),
        "cursor" to "pointer"
    ),
).add(
    getXySpecificComponentData()
)

fun getXySpecificComponentData() = json(
    "xyDocumentConfig" to null,
    "xyStartParamId" to "",

    "xy_svg_height" to "100%",
    "xyViewBoxBody" to "0 0 1 1",

    "xyViewCoord" to XyViewCoord(1, 0, 0, 1, 1),        // XyViewCoord( 1, 0, 0, 0, 0 ), в StateControl ???
    "arrXyElement" to arrayOf<Array<XyElementData>>(),

    "xyTooltipVisible" to false,
    "xyTooltipText" to "",
    "xyTooltipOffTime" to 0.0,

    "style_xy_tooltip_text" to json(
        "position" to "absolute",
        "color" to COLOR_MAIN_TEXT,
        "background" to COLOR_XY_LABEL_BACK,
        "border" to "1px solid $COLOR_XY_LABEL_BORDER",
        "border-radius" to styleButtonBorderRadius,
        "padding" to styleControlTooltipPadding(),
        "user-select" to if (styleIsNarrowScreen) "none" else "auto"
    ),
    "style_xy_tooltip_pos" to json(
        "left" to "",
        "top" to ""
    ),
)

fun readXyServerActionButton(that: dynamic, arrServerActionButton: Array<XyServerActionButton>) {
    var serverButtonID = 0
    val alServerButton = mutableListOf<XyServerActionButton_>()
    for (sab in arrServerActionButton) {
        val icon = hmTableIcon[sab.icon] ?: ""
        //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
        val caption = if (sab.icon.isNotBlank() && icon.isBlank()) {
            sab.icon
        } else {
            sab.caption.replace("\n", "<br>")
        }
        alServerButton.add(
            XyServerActionButton_(
                id = serverButtonID++,
                caption = caption,
                tooltip = sab.tooltip,
                icon = icon,
                url = sab.url,
                isForWideScreenOnly = sab.isForWideScreenOnly,
            )
        )
    }
    that.arrXyServerButton = alServerButton.toTypedArray()
}

class XyServerActionButton_(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String,
    val isForWideScreenOnly: Boolean,
)
