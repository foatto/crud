package foatto.core_server.app.xy.server.document

import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.config.XyBitmapType
import foatto.core.app.xy.geom.XyPoint
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AsyncFileSaver
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core.util.getStringFromIterable
import foatto.core_server.app.AppParameter
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.XyProperty
import foatto.sql.CoreAdvancedResultSet
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

abstract class sdcXyMap : sdcXyAbstract() {

    companion object {

        //--- специально чтобы не забыть - координаты точки в виде двух 4-байтовых чисел, занимают 8 символов
        val POINT_SIZE_IN_BIN = 2 * 4

        //--- папка для складывания заданий загрузчику битмапов
        val BITMAP_LOADER_JOB_DIR = "bl"

        //--- общий список обработанных битмапов (данный список позволяет сэкономить время на проверке наличия уже созданных файлов или
        //--- на неудачной попытке пересоздания только что неполучившегося файла)
        //--- key = server_url
        //--- value: для успешно созданных файлов - время обновления (по умолчанию - через год после создания)
        //---        для неполучающихся (по разным причинам) - время следующей попытки создания (по умолчанию - через неделю)
        private val chmWorkedBitmap = ConcurrentHashMap<String, Int>()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getElements(xyActionRequest: XyActionRequest): XyActionResponse {

        val xyStartDataID = xyActionRequest.startParamId
        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData

        val viewCoord = xyActionRequest.viewCoord!!

        //--- разбор входных параметров
        val hsReadOnlyObject = mutableSetOf<Int>()
        val alObjectParamData = parseObjectParam(false, sd, hsReadOnlyObject)

        //--- оптимизированная массовая загрузка статических объектов (без временнЫх параметров, например, зоны)

        val alElementData = loadStaticElement(alObjectParamData, viewCoord)
        if (alElementData.isNotEmpty()) {
            //--- подготовка IN-списка по elementId
            val sbelementId = StringBuilder()
            for ((edo, _) in alElementData) {
                sbelementId.append(if (sbelementId.isEmpty()) "" else " , ").append(edo.elementId)
            }

            val sPoint = " SELECT element_id , prj_x , prj_y FROM XY_point " +
                " WHERE element_id IN ( $sbelementId ) " +
                //--- ограничение по координатам, если есть (при загрузке одиночного элемента (например, для редактирования) они не указываются)
                //--- также ограничения по координатам не используются для статических бизнес-элементов (зоны, например), которые грузятся полностью
                //--- для бизнес-элементов ограничение по координатам при загрузке не применяется
                //if( ! ( prjX1 == 0 && prjY1 == 0 && prjX2 == 0 && prjY2 == 0 ) )
                //    sbPoint.append( " AND prj_x >= " ).append( prjX1 )
                //           .append( " AND prj_y >= " ).append( prjY1 )
                //           .append( " AND prj_x <= " ).append( prjX2 )
                //           .append( " AND prj_y <= " ).append( prjY2 );
                " ORDER BY element_id , sort_id "

            val sProperty = " SELECT element_id , property_name , property_value FROM XY_property " +
                " WHERE element_id IN ( $sbelementId ) " +
                " ORDER BY element_id "

            //--- загрузка двух наборов - point и property
            val rsPoint = conn.executeQuery(sPoint)
            val rsProperty = conn.executeQuery(sProperty)

            loadPointsAndProperties(sd, viewCoord.scale, rsPoint, rsProperty, hsReadOnlyObject, alElementData)

            rsPoint.close()
            rsProperty.close()
        }

        val alElement = alElementData.map { it.first }.toMutableList()

        //--- загрузка динамических объектов
        for (objectParamData in alObjectParamData) {
            if (objectParamData.begTime != 0) {
                loadDynamicElements(viewCoord.scale, objectParamData, alElement, mutableMapOf())
            }
        }
        //AdvancedLogger.debug( "load/write dynamic elements [obj] : " + ( System.currentTimeMillis() - begTime ) );

        //--- вывод элементов растровых карт:
        //--- грузим все виды карт, показываем только требуемые
        for (name in XyBitmapType.hmTypeScaleZ.keys) {
            outBitmapElements(name, viewCoord, if (name == xyActionRequest.bitmapTypeName) alElement else null)
        }

        //AdvancedLogger.info( "Doc Size = " + arrByte.length );
        //AdvancedLogger.info( "------------------------------------------------------------" );

        return XyActionResponse(arrElement = alElement.toTypedArray())
    }

    override fun getOneElement(xyActionRequest: XyActionRequest): XyActionResponse {

        val xyStartDataID = xyActionRequest.startParamId
        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData

        val viewCoord = xyActionRequest.viewCoord!!
        val elementId = xyActionRequest.elementId!!

        //--- список для одного элемента делается в целях совместимости с loadPointsAndProperties
        val alElementData = mutableListOf<Pair<XyElement, XyPoint>>()

        val rs = conn.executeQuery(
            " SELECT type_name , id , object_id , prj_x1 , prj_y1 , point_data " +
                " FROM XY_element " +
                " WHERE id = $elementId "
        )
        rs.next()
        alElementData.add(loadElement(rs, viewCoord))
        rs.close()

        val sPoint = " SELECT element_id , prj_x , prj_y FROM XY_point " +
            " WHERE element_id = $elementId " +
            " ORDER BY sort_id "

        val sProperty = " SELECT element_id , property_name , property_value FROM XY_property " +
            " WHERE element_id = $elementId "

        //--- загрузка двух наборов - point и property
        val rsPoint = conn.executeQuery(sPoint)
        val rsProperty = conn.executeQuery(sProperty)

        loadPointsAndProperties(sd, viewCoord.scale, rsPoint, rsProperty, emptySet(), alElementData)

        rsPoint.close()
        rsProperty.close()

        return XyActionResponse(element = alElementData.first().first)
    }

    override fun editElementPoint(xyActionRequest: XyActionRequest): XyActionResponse {
        putElement(xyActionRequest.xyElement!!, false)

        return XyActionResponse()
    }

    override fun moveElements(xyActionRequest: XyActionRequest): XyActionResponse {
        val selementId = getStringFromIterable(xyActionRequest.alActionElementIds!!, " , ")
        val dx = xyActionRequest.dx!!
        val dy = xyActionRequest.dy!!

        conn.executeUpdate(
            " UPDATE XY_element SET " +
                " prj_x1 = prj_x1 + ( $dx ) , " +
                " prj_y1 = prj_y1 + ( $dy ) , " +
                " prj_x2 = prj_x2 + ( $dx ) , " +
                " prj_y2 = prj_y2 + ( $dy ) " +
                " WHERE id IN ( $selementId ) "
        )

        return XyActionResponse()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun loadStaticElement(alObjectParamData: List<XyStartObjectParsedData>, viewCoord: XyViewCoord): List<Pair<XyElement, XyPoint>> {
        val alElementData = mutableListOf<Pair<XyElement, XyPoint>>()

        val alTypeName = mutableSetOf<String>()
        val alobjectId = mutableSetOf<Int>()

        //--- статические бизнес-объекты
        for (objectParamData in alObjectParamData) {
            if (objectParamData.begTime == 0) {
                for (typeName in objectParamData.hsType)
                    alTypeName.add(typeName)
                alobjectId.add(objectParamData.objectId)
            }
        }

        if (alTypeName.isNotEmpty()) {
            val s = " SELECT type_name , id , object_id , prj_x1 , prj_y1 , point_data " +
                " FROM XY_element " +
                " WHERE type_name IN ( ${getStringFromIterable(alTypeName, " , ", " '", "' ")} ) " +
                //if(sbobjectId.length > 0)
                " AND object_id IN ( ${getStringFromIterable(alobjectId, " , ")} ) " +
                " AND prj_x1 <= ${viewCoord.x2} " +
                " AND prj_y1 <= ${viewCoord.y2} " +
                " AND prj_x2 >= ${viewCoord.x1} " +
                " AND prj_y2 >= ${viewCoord.y1} " +
                " ORDER BY id "
            ///*if( AppServlet.xyLog != null )*/ AppServlet.xyLog.debug( sb.toString() );

            val rs = conn.executeQuery(s)
            while (rs.next())
                alElementData.add(loadElement(rs, viewCoord))
            rs.close()
        }

        return alElementData
    }

    private fun loadElement(rs: CoreAdvancedResultSet, viewCoord: XyViewCoord): Pair<XyElement, XyPoint> {
        val xyElement = XyElement(rs.getString(1), rs.getInt(2), rs.getInt(3))
        val prjXY = XyPoint(rs.getInt(4), rs.getInt(5))
        val bbPoint = rs.getByteBuffer(6, ByteOrder.BIG_ENDIAN)

        var lastPoint = XyPoint(0, 0)
        val alPoint = mutableListOf<XyPoint>()
        while (bbPoint.hasRemaining()) {
            lastPoint = loadPoint(lastPoint, viewCoord.scale, prjXY.x + bbPoint.getInt(), prjXY.y + bbPoint.getInt(), alPoint)
        }
        xyElement.alPoint = alPoint.toTypedArray()
        return Pair(xyElement, prjXY)
    }

    private fun loadPointsAndProperties(
        sd: XyStartData, scale: Int,
        rsPoint: CoreAdvancedResultSet, rsProperty: CoreAdvancedResultSet,
        hsReadOnlyObject: Set<Int>,
        alElementData: List<Pair<XyElement, XyPoint>>
    ) {
        //--- установка на первые строки ResultSet'ов
        rsPoint.next()
        rsProperty.next()
        for ((xyElement, prjXY) in alElementData) {
            var lastPoint = xyElement.alPoint.lastOrNull() ?: XyPoint(0, 0)
            val alPoint = xyElement.alPoint.toMutableList()
            while (!rsPoint.isAfterLast) {
                val pointelementId = rsPoint.getInt(1)
                if (pointelementId != xyElement.elementId) break        // кончились точки данного элемента
                lastPoint = loadPoint(lastPoint, scale, prjXY.x + rsPoint.getInt(2), prjXY.y + rsPoint.getInt(3), alPoint)
                rsPoint.next()
            }
            xyElement.alPoint = alPoint.toTypedArray()

            while (!rsProperty.isAfterLast) {
                val propertyelementId = rsProperty.getInt(1)
                if (propertyelementId != xyElement.elementId) break        // кончились св-ва данного элемента
                loadProperty(xyElement, rsProperty.getString(2), rsProperty.getString(3))
                rsProperty.next()
            }

            //--- отметим элементы "только для чтения/просмотра"
            xyElement.itReadOnly = hsReadOnlyObject.contains(xyElement.objectId)
            //--- отметим актуальные (isStart) объекты
            if (xyElement.objectId != 0)
                for (sod in sd.alStartObjectData)
                    if (sod.objectId == xyElement.objectId) {
                        xyElement.itActual = sod.isStart
                        break
                    }
        }
    }

    private fun loadPoint(lastPoint: XyPoint, aScale: Int, aX: Int, aY: Int, alPoint: MutableList<XyPoint>) =
    //--- первая точка доступна для всех масштабов
        //--- (точность вычисления расстояния не важна, используем проекционные координаты)
        if (aScale == 0 || alPoint.isEmpty() || XyPoint.distance(aX.toDouble(), aY.toDouble(), lastPoint.x.toDouble(), lastPoint.y.toDouble()) > aScale * XyElement.GEN_KOEF) {
            val p = XyPoint(aX, aY)
            alPoint.add(p)
            p
        } else lastPoint

    private fun loadProperty(xyElement: XyElement, propName: String, propValue: String) {
        when (propName) {
            XyProperty.IS_CLOSED -> xyElement.itClosed = propValue.toBoolean()
            XyProperty.LINE_WIDTH -> xyElement.lineWidth = propValue.toInt()
            XyProperty.DRAW_COLOR -> xyElement.drawColor = propValue.toInt()
            XyProperty.FILL_COLOR -> xyElement.fillColor = propValue.toInt()
            XyProperty.IMAGE_ANCHOR_X -> xyElement.anchorX = XyElement.Anchor.valueOf(propValue)  // } catch( t: Throwable ) { XyElement.Anchor.CC }
            XyProperty.IMAGE_ANCHOR_Y -> xyElement.anchorY = XyElement.Anchor.valueOf(propValue)  // } catch( t: Throwable ) { XyElement.Anchor.CC }
            XyProperty.ROTATE_DEGREE -> xyElement.rotateDegree = propValue.toDouble()   //OrNull() ?: 0.0
            XyProperty.TOOL_TIP_TEXT -> xyElement.toolTipText = propValue
            XyProperty.IMAGE_NAME -> xyElement.imageName = propValue
            XyProperty.IMAGE_WIDTH -> xyElement.imageWidth = propValue.toInt()      //OrNull() ?: 0
            XyProperty.IMAGE_HEIGHT -> xyElement.imageHeight = propValue.toInt()    //OrNull() ?: 0
            XyProperty.MARKER_TYPE -> xyElement.markerType = XyElement.MarkerType.valueOf(propValue)  //} catch( t: Throwable ) { XyElement.MarkerType.CIRCLE }
            XyProperty.MARKER_SIZE -> xyElement.markerSize = propValue.toInt()
            XyProperty.MARKER_SIZE_2 -> xyElement.markerSize2 = propValue.toInt()
            XyProperty.TEXT -> xyElement.text = propValue
            XyProperty.TEXT_COLOR -> xyElement.textColor = propValue.toInt()
            XyProperty.FONT_SIZE -> xyElement.fontSize = propValue.toInt()
            XyProperty.FONT_BOLD -> xyElement.itFontBold = propValue.toBoolean()
            XyProperty.ARROW_POS -> xyElement.arrowPos = XyElement.ArrowPos.valueOf(propValue)
            XyProperty.ARROW_LEN -> xyElement.arrowLen = propValue.toInt()
            XyProperty.ARROW_HEIGHT -> xyElement.arrowHeight = propValue.toInt()
            XyProperty.ARROW_LINE_WIDTH -> xyElement.arrowLineWidth = propValue.toInt()
        }
    }

    private fun outBitmapElements(bmTypeName: String, viewCoord: XyViewCoord, alElement: MutableList<XyElement>?) {

        val zoomLevel = XyBitmapType.hmTypeScaleZ[bmTypeName]?.get(viewCoord.scale) ?: return
        if (zoomLevel == -1) return

        //--- опытным путём - 3840х2160 даёт максимум 16x9=144 заданий х 12 байт = 1728 байт
        val bbTask = AdvancedByteBuffer(2048)

        //--- мировой размер битмапа для текущего масштаба в метрах
        val bmRealSize = XyBitmapType.BLOCK_SIZE * viewCoord.scale
        //--- выравнивание запрашиваемой области по размеру битмапа в большую сторону
        val x1 = (viewCoord.x1 / bmRealSize - 1) * bmRealSize
        val y1 = (viewCoord.y1 / bmRealSize - 1) * bmRealSize
        val x2 = (viewCoord.x2 / bmRealSize + 1) * bmRealSize
        val y2 = (viewCoord.y2 / bmRealSize + 1) * bmRealSize

        var y = y1
        while (y <= y2) {
            var x = x1
            while (x <= x2) {
                val blockX = x / XyBitmapType.BLOCK_SIZE / viewCoord.scale
                val blockY = y / XyBitmapType.BLOCK_SIZE / viewCoord.scale
                //--- при высоких масштабах можем залезть за край земли
                if (blockX < 0 || blockY < 0) {
                    x += bmRealSize
                    continue
                }

                //val serverURL = "${XyBitmapType.BITMAP_DIR}$bmTypeName/$zoomLevel/$blockY/$blockX.${XyBitmapType.BITMAP_EXT}"

                //--- специфично для MAPNIK
                val arrPrefixOSM = charArrayOf('a', 'b', 'c')
                val sbServerURL = StringBuilder("http://a.tile.openstreetmap.org")
                sbServerURL.setCharAt(7, arrPrefixOSM[getRandomInt() % 3])
                val serverURL = "$sbServerURL/$zoomLevel/$blockX/$blockY.png"

                //--- если выходной поток задан, пишем в него
                //--- (null может быть при запуске загрузки других типов карт)
                if (alElement != null) {
                    val imageElement = XyElement(BITMAP, -getRandomInt(), 0)
                    //imageElement.init(timeZone)
                    imageElement.itReadOnly = true
                    imageElement.alPoint = arrayOf(XyPoint(x, y))
                    imageElement.imageWidth = bmRealSize
                    imageElement.imageHeight = bmRealSize
                    imageElement.imageName = serverURL

                    alElement.add(imageElement)
                }

                //--- если этот файл уже запрашивался, пропускаем, он вероятно уже создан
                if (chmWorkedBitmap[serverURL] != null) {
                    x += bmRealSize
                    continue
                }
                chmWorkedBitmap[serverURL] = getCurrentTimeInt()

                bbTask.putInt(zoomLevel) // в принципе, хватило бы и байта, да смысла нет жадничать
                bbTask.putInt(blockX)
                bbTask.putInt(blockY)
                x += bmRealSize
            }
            y += bmRealSize
        }

        bbTask.flip()

        if (bbTask.hasRemaining())
        //--- приоритет запишем в начале файла
            AsyncFileSaver.put("$BITMAP_LOADER_JOB_DIR/${if (alElement == null) '1' else '0'}_${bmTypeName}_${getRandomInt()}", bbTask)
    }

    protected fun putElement(xyElement: XyElement, isAddElement: Boolean) {
        //--- определим крайние координаты
        val minX = xyElement.alPoint.minByOrNull { it.x }!!.x
        val minY = xyElement.alPoint.minByOrNull { it.y }!!.y
        val maxX = xyElement.alPoint.maxByOrNull { it.x }!!.x
        val maxY = xyElement.alPoint.maxByOrNull { it.y }!!.y

        //--- сначала запишем в hex-поле, сколько влезет
        val maxHexPointCount = conn.dialect.binaryFieldMaxSize / POINT_SIZE_IN_BIN
        val hexPointCount = xyElement.alPoint.size.coerceAtMost(maxHexPointCount)
        val bbPoint = AdvancedByteBuffer(hexPointCount * POINT_SIZE_IN_BIN)
        for (i in 0 until hexPointCount) {
            val p = xyElement.alPoint[i]

            bbPoint.putInt(p.x - minX)
            bbPoint.putInt(p.y - minY)
        }
        bbPoint.flip()

        if (isAddElement) {
            conn.executeUpdate(
                " INSERT INTO XY_element ( id , type_name , object_id , prj_x1 , prj_y1 , prj_x2 , prj_y2 , point_data ) VALUES ( " +
                    " ${xyElement.elementId} , '${xyElement.typeName}' , ${xyElement.objectId} , $minX , $minY , $maxX , $maxY , ${conn.getHexValue(bbPoint)} ) "
            )
        } else {
            conn.executeUpdate(
                " UPDATE XY_element SET prj_x1 = $minX , prj_y1 = $minY , prj_x2 = $maxX , prj_y2 = $maxY , point_data = ${conn.getHexValue(bbPoint)} " +
                    " WHERE id = ${xyElement.elementId} "
            )
            conn.executeUpdate(" DELETE FROM XY_point WHERE element_id = ${xyElement.elementId} ")
        }
        //--- запись точек, не влезших в hex-поле, если таковые есть
        for (i in maxHexPointCount until xyElement.alPoint.size) {
            val p = xyElement.alPoint[i]
            conn.executeUpdate(
                " INSERT INTO XY_point ( element_id , sort_id , prj_x , prj_y ) VALUES ( " +
                    //--- запись относительных координат в точки позволит перемещать элемент простым смещением prjX..Y_1..2 без попутного апдейта этих точек
                    " ${xyElement.elementId} , ${i - maxHexPointCount} , ${p.x - minX} , ${p.y - minY} ) "
            )
        }
    }
}

//        //--- удалить элементы
//        else if( action.equals( XyParameter.ACTION_DELETE_ELEMENT ) ) return doDeleteElement( alConn, hmParam );
//        //--- повернуть элемент
//        else if( action.equals( XyParameter.ACTION_ROTATE_ELEMENT ) ) return doRotateElement( alConn, hmParam );
//        //--- изменить текст элемента
//        else if( action.equals( XyParameter.ACTION_EDIT_ELEMENT_TEXT ) ) return doEditElementText( alConn, hmParam );
//        //--- копировать элемент
//        else if( action.equals( XyParameter.ACTION_COPY_ELEMENT ) ) return doCopyElement( alConn, hmParam );
//        //--- изменить тип элементов
//        else if( action.equals( XyParameter.ACTION_CHANGE_TYPE_ELEMENT ) ) return doChangeTypeOfElement( alConn, hmParam );
//        //--- добавить к родительскому объекту
//        else if( action.equals( XyParameter.ACTION_LINK_TO_PARENT ) ) return doLinkToParentObject( alConn, hmParam );
//        //--- удалить из родительского объекта
//        else if( action.equals( XyParameter.ACTION_UNLINK_FROM_PARENT ) ) return doUnlinkFromParentObject( alConn, hmParam );
