package foatto.mms.core_mms.xy.server.document

import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.XyDocumentConfig
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.document.sdcXyScheme
import foatto.mms.core_mms.calc.ObjectState
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigEnergoAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.iMMSApplication
import foatto.sql.CoreAdvancedConnection
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class sdcMMSSchemeAnalogueIndicator : sdcXyScheme() {

    companion object {
        //--- arrange in ascending order of the level of location
        const val TYPE_SCHEME_AI_TITLE_TEXT = "mms_scheme_ai_title_text"
        const val TYPE_SCHEME_AI_INDICATOR = "mms_scheme_ai_indicator"
        const val TYPE_SCHEME_AI_ARROW = "mms_scheme_ai_arrow"
        const val TYPE_SCHEME_AI_VALUE_TEXT = "mms_scheme_ai_value_text"
        const val TYPE_SCHEME_AI_DESCR_TEXT = "mms_scheme_ai_descr_text"

//        private const val OBJECT_SCHEME_SIZE = 1024 * 1024 * 1024
//        private const val OBJECT_SCHEME_ELEMENT_SIZE_BASE = OBJECT_SCHEME_SIZE / 128 // 64 - крупновато

        private const val TEXT_COLOR = 0xFF_00_00_00.toInt()
        private const val INDICATOR_COLOR = 0xFF_00_00_00.toInt()
        private const val ARROW_COLOR = 0xFF_00_00_00.toInt()

//        private const val TEXT_BACK_COLOR = 0xFF_FF_FF_D0.toInt()   // hsl(60,100%,90.8%)
//        private const val TEXT_BORDER_COLOR = 0xFF_CC_CC_00.toInt()   // hsl(60,100%,40%)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        aApplication: iApplication,
        aConn: CoreAdvancedConnection,
        aChmSession: ConcurrentHashMap<String, Any>,
        aUserConfig: UserConfig,
        aDocumentConfig: XyDocumentConfig
    ) {

        //--- schemes

        hmInAliasElementType["mms_sensor"] = arrayOf(
            TYPE_SCHEME_AI_TITLE_TEXT,
            TYPE_SCHEME_AI_INDICATOR,
            TYPE_SCHEME_AI_ARROW,
            TYPE_SCHEME_AI_VALUE_TEXT,
            TYPE_SCHEME_AI_DESCR_TEXT,
        )

        hmOutElementTypeAlias[TYPE_SCHEME_AI_TITLE_TEXT] = "mms_sensor"
        hmOutElementTypeAlias[TYPE_SCHEME_AI_INDICATOR] = "mms_sensor"
        hmOutElementTypeAlias[TYPE_SCHEME_AI_ARROW] = "mms_sensor"
        hmOutElementTypeAlias[TYPE_SCHEME_AI_VALUE_TEXT] = "mms_sensor"
        hmOutElementTypeAlias[TYPE_SCHEME_AI_DESCR_TEXT] = "mms_sensor"

        super.init(aApplication, aConn, aChmSession, aUserConfig, aDocumentConfig)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getCoords(startParamId: String): XyActionResponse {
//        val sd = chmSession[AppParameter.XY_START_DATA + startParamId] as XyStartData
//
//        //--- разбор входных параметров
//        val alObjectParamData = parseObjectParam(true /*isStartObjectsDefined*/, sd, mutableSetOf())
//
//        var prjX1 = Int.MAX_VALUE
//        var prjY1 = Int.MAX_VALUE
//        var prjX2 = Int.MIN_VALUE
//        var prjY2 = Int.MIN_VALUE
//
//        //--- отдельная обработка динамических объектов
//        for (objectParamData in alObjectParamData) {
//            val alResult = getElementList(1, objectParamData, mutableMapOf())
//            for (e in alResult) {
//                for (p in e.alPoint) {
//                    prjX1 = min(prjX1, p.x)
//                    prjY1 = min(prjY1, p.y)
//                    prjX2 = max(prjX2, p.x)
//                    prjY2 = max(prjY2, p.y)
//                }
//            }
//        }
//
//        return XyActionResponse(
//            minCoord = XyPoint(prjX1 - GRID_STEP, prjY1 - GRID_STEP),
//            maxCoord = XyPoint(prjX2 + GRID_STEP, prjY2 + GRID_STEP * 2),
//        )
        return XyActionResponse(
            minCoord = XyPoint(0, 0),
            maxCoord = XyPoint(6 * GRID_STEP, 7 * GRID_STEP),
        )
    }

    override fun getOneElement(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun clickElement(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun addElement(xyActionRequest: XyActionRequest, userID: Int) = XyActionResponse()
    override fun editElementPoint(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun moveElements(xyActionRequest: XyActionRequest) = XyActionResponse()

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadDynamicElements(
        scale: Int,
        objectParamData: XyStartObjectParsedData,
        alElement: MutableList<XyElement>,
        hmParams: MutableMap<String, String>,
    ) {
        alElement.addAll(getElementList(scale, objectParamData, hmParams))
    }

    private fun getElementList(
        scale: Int,
        objectParamData: XyStartObjectParsedData,
        hmParams: MutableMap<String, String>,
    ): List<XyElement> {

        val rs = conn.executeQuery(
            """
                SELECT object_id , sensor_type , port_num 
                FROM MMS_sensor 
                WHERE id = ${objectParamData.objectId}
            """
        )
        rs.next()
        val objectId = rs.getInt(1)
        val sensorType = rs.getInt(2)
        val portNum = rs.getInt(3)
        rs.close()

        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
        val objectState = ObjectState.getState(conn, objectConfig)

        val alResult = mutableListOf<XyElement>()

        val sca = (objectConfig.hmSensorConfig[sensorType]?.get(portNum) as? SensorConfigAnalogue)!!
        val scea = sca as? SensorConfigEnergoAnalogue

        /*
                const val SENSOR_LIQUID_FLOW_CALC = 3
                const val SENSOR_WEIGHT = 5
                const val SENSOR_TURN = 6
                const val SENSOR_PRESSURE = 7
                const val SENSOR_TEMPERATURE = 8
                const val SENSOR_VOLTAGE = 9
                const val SENSOR_POWER = 10
        
                //--- контрольные величины - графики
                const val SENSOR_DENSITY = 11
                const val SENSOR_MASS_FLOW = 12
                const val SENSOR_VOLUME_FLOW = 13
        */

        val sensorDescr = sca.descr
        var sensorValue: Double? = null
        var descrText = ""

        when (sensorType) {
            SensorConfig.SENSOR_LIQUID_LEVEL -> {
                (sca as? SensorConfigLiquidLevel)?.let { scll ->
                    sensorValue = objectState.tmLiquidLevel[sensorDescr]
                }
            }

            SensorConfig.SENSOR_ENERGO_VOLTAGE -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoVoltageValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_CURRENT -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoCurrentValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_POWER_KOEF -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoPowerKoefValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoPowerActiveValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoPowerReactiveValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_POWER_FULL -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoPowerFullValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoTransformKoefCurrentValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }

            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE -> {
                val phase = scea?.phase
                sensorValue = objectState.tmEnergoTransformKoefVoltageValue[sensorDescr]?.get(phase)
                descrText = when (phase) {
                    0 -> "ABC"
                    1 -> "A"
                    2 -> "B"
                    3 -> "C"
                    else -> "-"
                }
            }
        }

        XyElement(TYPE_SCHEME_AI_TITLE_TEXT, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(3 * GRID_STEP, 1 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorDescr
            toolTipText = sensorDescr
            textColor = TEXT_COLOR
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = false
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        XyElement(TYPE_SCHEME_AI_INDICATOR, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(3 * GRID_STEP, 4 * GRID_STEP))
            radius = 2 * GRID_STEP
            startAngle = 180
            endAngle = 0
            fillColor = null
            drawColor = INDICATOR_COLOR
            lineWidth = 1
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        //--- нет значения - нет стрелки
        sensorValue?.let { sv ->
            val dv = sca.maxView - sca.minView
            if (dv > 0) {
                //val degree = 180 * sensorValue / dv
                //val radian = degree * PI / 180
                //--- итого:
                val radian = sv / dv * PI
                val dx = -cos(radian) * 2 * GRID_STEP
                val dy = -sin(radian) * 2 * GRID_STEP
                XyElement(TYPE_SCHEME_AI_ARROW, -getRandomInt(), objectId).apply {
                    isReadOnly = true
                    alPoint = listOf(
                        XyPoint(3 * GRID_STEP, 4 * GRID_STEP),
                        XyPoint(3 * GRID_STEP + dx, 4 * GRID_STEP + dy),
                    )
                    fillColor = null
                    drawColor = ARROW_COLOR
                    lineWidth = 1
                }.let { xyElement ->
                    alResult.add(xyElement)
                }
            }
        }

        XyElement(TYPE_SCHEME_AI_VALUE_TEXT, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(3 * GRID_STEP, 5 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorValue?.let { sv ->
                getSplittedDouble(sv, 1)
            } ?: "-"
            toolTipText = descrText
            textColor = TEXT_COLOR
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = false
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        XyElement(TYPE_SCHEME_AI_DESCR_TEXT, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(3 * GRID_STEP, 6 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = descrText
            toolTipText = descrText
            textColor = TEXT_COLOR
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = false
        }.let { xyElement ->
            alResult.add(xyElement)
        }

//            //--- выводим время последних данных
//            val label = XyElement(TEXT, -getRandomInt(), objectConfig.objectId).apply {
//                //typeName = TYPE_STATE_W_TEXT;
//                isReadOnly = true
//                alPoint = listOf(XyPoint(GRID_STEP * 6, GRID_STEP * 26))
//                anchorX = XyElement.Anchor.LT
//                anchorY = XyElement.Anchor.LT
//                alignX = XyElement.Align.LT
//                alignY = XyElement.Align.LT
//                fillColor = 0xFF_F0_F0_F0.toInt()
//                text = objectState.lastDataTime?.let { lastDataTime ->
//                    "По состоянию на ${DateTime_YMDHMS(zoneId, lastDataTime)}"
//                } ?: "(нет данных)"
//                textColor = TEXT_COLOR
//                fontSize = iCoreAppContainer.BASE_FONT_SIZE
//                isFontBold = false
//            }
//            alResult.add(label)

        return alResult
    }

}
