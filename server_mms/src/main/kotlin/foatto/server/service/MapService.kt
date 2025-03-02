package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyElement
import foatto.core.model.model.xy.XyProjection
import foatto.core.model.model.xy.XyViewCoord
import foatto.core.model.request.MapActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.MapActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.xy.XyBitmapType
import foatto.core.model.response.xy.XyElementClientType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.map.MapResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.initXyElementConfig
import foatto.server.model.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MapService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) {

    companion object {
        //--- пока минимально используемый масштаб 1:16, что соответствует старому 1:2000 при 4 пикселях на мм
        //--- ограничение исходит из-за отсутствия битмапов выше 18-го уровня ( т.е. для 1:1000 ),
        //--- а 18-й уровень битмапов сооветствует масштабу 1:16
        const val MAP_MIN_SCALE: Int = 16

        //--- пока максимально используемый масштаб 1:512*1024, что соответствует старому 1:32_768_000 при 4 пикселях на мм
        const val MAP_MAX_SCALE: Int = 512 * 1024

        const val ELEMENT_TYPE_ZONE: String = "mms_zone"

        //--- располагаем по возрастанию уровня расположения
        const val TYPE_OBJECT_TRACE: String = "mms_object_trace"
        const val TYPE_OBJECT_PARKING: String = "mms_object_parking"
        const val TYPE_OBJECT_OVER_SPEED: String = "mms_object_over_speed"
        const val TYPE_OBJECT_TRACE_INFO: String = "mms_object_trace_info"
        const val TYPE_OBJECT_INFO: String = "mms_object_info"

        /*
                private const val INDICATOR_BACK_COLOR_NO_DATA = 0xFF_D0_D0_D0.toInt()
                private const val INDICATOR_BACK_COLOR_OFF = 0xFF_FF_E0_E0.toInt()
                private const val INDICATOR_BACK_COLOR_ON = 0xFF_E0_FF_E0.toInt()

                private const val INDICATOR_BORDER_COLOR_NO_DATA = 0xFF_A0_A0_A0.toInt()
                private const val INDICATOR_BORDER_COLOR_OFF = 0xFF_FF_00_00.toInt()
                private const val INDICATOR_BORDER_COLOR_ON = 0xFF_00_B0_00.toInt()

                private const val INDICATOR_BACK_COLOR_OFF = 0xFF_D0_D0_D0.toInt()
                private const val INDICATOR_BACK_COLOR_NEUTRAL = 0xFF_F0_F0_F0.toInt()
                private const val INDICATOR_BACK_COLOR_NORMAL = 0xFF_E0_FF_E0.toInt()
                private const val INDICATOR_BACK_COLOR_CRITICAL = 0xFF_FF_E0_E0.toInt()

                private const val INDICATOR_ARC_COLOR_NEUTRAL = 0xFF_A0_A0_A0.toInt()
                private const val INDICATOR_ARC_COLOR_NORMAL = 0xFF_00_B0_00.toInt()
                private const val INDICATOR_ARC_COLOR_CRITICAL = 0xFF_FF_00_00.toInt()
                private const val INDICATOR_ARC_COLOR_BASE = 0xFF_00_00_00.toInt()

                private const val INDICATOR_MULTIPLICATOR_COLOR = 0xFF_A0_A0_A0.toInt()

                private const val TEXT_COLOR = 0xFF_00_00_00.toInt()
         */
    }

    private fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MAP_MIN_SCALE, maxScale = MAP_MAX_SCALE).apply {
        this[TYPE_OBJECT_TRACE] = XyElementConfig(
            name = TYPE_OBJECT_TRACE,
            clientType = XyElementClientType.TRACE,
            layer = 11,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        this[TYPE_OBJECT_PARKING] = XyElementConfig(
            name = TYPE_OBJECT_PARKING,
            clientType = XyElementClientType.TEXT,
            layer = 12,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        this[TYPE_OBJECT_OVER_SPEED] = XyElementConfig(
            name = TYPE_OBJECT_OVER_SPEED,
            clientType = XyElementClientType.TEXT,
            layer = 13,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        this[TYPE_OBJECT_TRACE_INFO] = XyElementConfig(
            name = TYPE_OBJECT_TRACE_INFO,
            clientType = XyElementClientType.TEXT,
            layer = 14,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        this[TYPE_OBJECT_INFO] = XyElementConfig(
            name = TYPE_OBJECT_INFO,
            clientType = XyElementClientType.MARKER,
            layer = 15,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        //--- прикладные топо-объекты, добавляемые пользователем вручную на карте

        this[ELEMENT_TYPE_ZONE] = XyElementConfig(
            name = ELEMENT_TYPE_ZONE,
            clientType = XyElementClientType.ZONE,
            layer = 10,
            scaleMin = MAP_MIN_SCALE,
            scaleMax = MAP_MAX_SCALE,
            descrForAction = "Геозона",
            isRotatable = false,
            isMoveable = true,
            isEditablePoint = true
        )
    }

    fun map(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }
        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)
        val objectEntity = objectRepository.findByIdOrNull(action.id) ?: return AppResponse(ResponseCode.LOGON_NEED)

        val caption = moduleConfig.caption
        val rows = mutableListOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель" to (objectEntity.model ?: "-"),
        )

        if (action.timeRangeType != 0) {
            rows += "Период" to "за последние " +
                if (action.timeRangeType % 3600 == 0) {
                    "${action.timeRangeType / 3600} час(а,ов)"
                } else if (action.timeRangeType % 60 == 0) {
                    "${action.timeRangeType / 60} минут"
                } else {
                    "${action.timeRangeType} секунд"
                }
        }

        return AppResponse(
            responseCode = ResponseCode.MODULE_MAP,
            map = MapResponse(
                elementConfigs = getElementConfigs(),
                tabCaption = caption,
                headerData = HeaderData(
                    titles = listOf(
                        TitleData(
                            action = null,
                            text = caption,
                            isBold = true,
                        )
                    ),
                    rows = rows,
                ),
                timeRangeType = action.timeRangeType,
                begTime = action.begTime ?: 0,
                endTime = action.endTime ?: 0,
            )
        )
    }

    fun mapAction(mapActionRequest: MapActionRequest): MapActionResponse {
        val actionModule = mapActionRequest.action.module

        val sessionData = SpringApp.getSessionData(mapActionRequest.sessionId) ?: return MapActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return MapActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return MapActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (mapActionRequest.action.type) {
            ActionType.GET_COORDS -> getCoords(mapActionRequest)

            ActionType.GET_ELEMENTS -> {
                MapActionResponse(
                    responseCode = ResponseCode.OK,
                    elements = getElements(mapActionRequest),
                )
            }

            else -> {
                MapActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    private fun getCoords(mapActionRequest: MapActionRequest): MapActionResponse {
        val appAction = mapActionRequest.action

        var isFound = false
        var minPoint = XyPoint(0, 0)
        var maxPoint = XyPoint(0, 0)

        val (begTime, endTime) = getBegEndTime(appAction)

        objectRepository.findByIdOrNull(appAction.id)?.let { objectEntity ->
            sensorRepository.findByObjAndSensorType(objectEntity, SensorConfig.SENSOR_GEO).firstOrNull()?.let { sensorEntity ->
                SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

                ApplicationService.withConnection(entityManager) { conn ->
                    val rs = conn.executeQuery(
                        """
                            SELECT COUNT(ontime_0) , MIN(value_0) , MAX(value_0) , MIN(value_1) , MAX(value_1)
                            FROM MMS_agg_${sensorEntity.id}
                            WHERE ontime_0 BETWEEN $begTime AND $endTime
                        """
                    )
                    if (rs.next() && rs.getInt(1) > 0) {
                        val minWgsX = rs.getDouble(2)
                        val maxWgsX = rs.getDouble(3)
                        val minWgsY = rs.getDouble(4)
                        val maxWgsY = rs.getDouble(5)

                        isFound = true

                        minPoint = XyProjection.wgs_pix(minWgsX, maxWgsY, minPoint)
                        maxPoint = XyProjection.wgs_pix(maxWgsX, minWgsY, maxPoint)
                    }
                    rs.close()
                }
            }
        }

        //--- если элементы, соответствующие стартовым объектам, не нашлись, то возвращаем границы РФ
        return MapActionResponse(
            responseCode = ResponseCode.OK,
            minCoord = if (isFound) {
                minPoint
            } else {
                XyProjection.wgs_pix(20.0, 70.0)
            },
            maxCoord = if (isFound) {
                maxPoint
            } else {
                XyProjection.wgs_pix(180.0, 40.0)
            },
        )
    }

    private fun getElements(mapActionRequest: MapActionRequest): List<XyElement> {
        val appAction = mapActionRequest.action

        val (begTime, endTime) = getBegEndTime(appAction)

        val points = mutableListOf<XyPoint>()
        val drawColors = mutableListOf<Int>()
        val fillColors = mutableListOf<Int>()

        objectRepository.findByIdOrNull(appAction.id)?.let { objectEntity ->
            sensorRepository.findByObjAndSensorType(objectEntity, SensorConfig.SENSOR_GEO).firstOrNull()?.let { sensorEntity ->
                SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

                ApplicationService.withConnection(entityManager) { conn ->
                    val rs = conn.executeQuery(
                        """
                            SELECT value_0 , value_1
                            FROM MMS_agg_${sensorEntity.id}
                            WHERE ontime_0 BETWEEN $begTime AND $endTime
                            ORDER BY ontime_0 
                        """
                    )
                    while (rs.next()) {
                        val wgsX = rs.getDouble(1)
                        val wgsY = rs.getDouble(2)

                        points += XyProjection.wgs_pix(wgsX, wgsY)
                        drawColors += 0xFF_00_00_FF.toInt()
                        fillColors += 0xFF_00_FF_FF.toInt()
                    }
                    rs.close()
                }
            }
        }

        val elements = mutableListOf(
            XyElement(TYPE_OBJECT_TRACE, -getRandomInt(), appAction.id ?: 0).apply {
                isReadOnly = true
                alPoint = points
                fillColor = null
                //drawColor = 0xFF_00_00_FF.toInt()
                lineWidth = 2
                this.drawColors = drawColors
                this.fillColors = fillColors
            }
        )

        outBitmapElements(mapActionRequest.bitmapTypeName!!, mapActionRequest.viewCoord!!, elements)

        return elements
    }

    private fun getBegEndTime(action: AppAction): Pair<Int, Int> {
        val endTime = if (action.timeRangeType == 0) {
            action.endTime ?: getCurrentTimeInt()
        } else {
            getCurrentTimeInt()
        }

        val begTime = if (action.timeRangeType == 0) {
            action.begTime ?: (getCurrentTimeInt() - 86_400)
        } else {
            getCurrentTimeInt() - action.timeRangeType
        }

        return begTime to endTime
    }

    private fun outBitmapElements(bmTypeName: String, viewCoord: XyViewCoord, elements: MutableList<XyElement>?) {
        val zoomLevel = XyBitmapType.hmTypeScaleZ[bmTypeName]?.get(viewCoord.scale) ?: return
        if (zoomLevel == -1) {
            return
        }

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
                if (elements != null) {
                    val imageElement = XyElement(XyElementClientType.BITMAP.name, -getRandomInt(), 0).apply {
                        isReadOnly = true
                        alPoint = listOf(XyPoint(x, y))
                        imageWidth = bmRealSize
                        imageHeight = bmRealSize
                        imageName = serverURL
                    }

                    elements.add(imageElement)
                }

                x += bmRealSize
            }
            y += bmRealSize
        }
    }

}
/*
class XyStartData(
    val alServerActionButton: MutableList<ServerActionButton> = mutableListOf(),
)

class XyStartObjectData(
    val objectId: Int,
    var typeName: String = "",
    var isStart: Boolean = false,
    var isTimed: Boolean = false,
    var isReadOnly: Boolean = false
)

return AppResponse(
    map = MapResponse(
        alServerActionButton = sd.alServerActionButton,
    )
)
 */