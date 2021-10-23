package foatto.ts.core_ts.xy.server.document

import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.XyDocumentConfig
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.document.sdcXyState
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.cObject
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.core_ts.device.cDevice
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class sdcTSState : sdcXyState() {

    companion object {
        //--- arrange in ascending order of the level of location
        const val TYPE_STATE_AXIS = "ts_state_axis"
        const val TYPE_STATE_TEXT = "ts_state_text"
        const val TYPE_STATE_AXIS_POINTER = "ts_state_axis_pointer"
        const val TYPE_STATE_VALUE_BAR = "ts_state_value_bar"

        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        private const val GRID_STEP = 1024 * 1024

        //--- binding an element and a command on it
        private val chmElementCommand = ConcurrentHashMap<Int, String>()

        //--- binding an element and a port number on it (for the subsequent determination of the deviceID, which is redundant to determine when rendering)
        private val chmElementPort = ConcurrentHashMap<Int, Int>()
    }

    override fun init(
        aApplication: iApplication,
        aConn: CoreAdvancedConnection,
        aStm: CoreAdvancedStatement,
        aChmSession: ConcurrentHashMap<String, Any>,
        aUserConfig: UserConfig,
        aDocumentConfig: XyDocumentConfig
    ) {

        //--- schemes

        hmInAliasElementType["ts_object"] = arrayOf(
            TYPE_STATE_AXIS,
            TYPE_STATE_AXIS_POINTER,
            TYPE_STATE_TEXT,
            TYPE_STATE_VALUE_BAR,
        )

        hmOutElementTypeAlias[TYPE_STATE_AXIS] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_AXIS_POINTER] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_TEXT] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_VALUE_BAR] = "ts_object"

        super.init(aApplication, aConn, aStm, aChmSession, aUserConfig, aDocumentConfig)
    }

    override fun getCoords(startParamId: String): XyActionResponse {
        val sd = chmSession[AppParameter.XY_START_DATA + startParamId] as XyStartData

        var prjX1 = Int.MAX_VALUE
        var prjY1 = Int.MAX_VALUE
        var prjX2 = Int.MIN_VALUE
        var prjY2 = Int.MIN_VALUE

        //--- разбор входных параметров
        val alObjectParamData = parseObjectParam(true /*isStartObjectsDefined*/, sd, mutableSetOf())

        //--- получить данные по правам доступа
        val isRemoteControlPermission = userConfig.userPermission["ts_object"]?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

        //--- отдельная обработка динамических объектов
        for (objectParamData in alObjectParamData) {
            val alResult = getElementList(1, objectParamData, isRemoteControlPermission)
            for (e in alResult) {
                for (p in e.alPoint) {
                    prjX1 = min(prjX1, p.x)
                    prjY1 = min(prjY1, p.y)
                    prjX2 = max(prjX2, p.x)
                    prjY2 = max(prjY2, p.y)
                }
            }
        }

        return XyActionResponse(
            minCoord = XyPoint(prjX1 - GRID_STEP, prjY1 - GRID_STEP),
            maxCoord = XyPoint(prjX2 + GRID_STEP, prjY2 + GRID_STEP * 2)
        )
    }

    override fun getOneElement(xyActionRequest: XyActionRequest) = XyActionResponse()

    override fun clickElement(xyActionRequest: XyActionRequest): XyActionResponse {
        val elementId = xyActionRequest.elementId!!
        val objectId = xyActionRequest.objectId!!

        val command = chmElementCommand[elementId]
        if (!command.isNullOrBlank()) {
            var deviceID = 0
            val deviceIndex = chmElementPort[elementId]!! / AbstractTelematicHandler.MAX_PORT_PER_DEVICE
            val rs = stm.executeQuery(
                """
                    SELECT id FROM TS_device WHERE object_id = $objectId AND device_index = $deviceIndex
                """
            )
            if (rs.next()) {
                deviceID = rs.getInt(1)
            }
            rs.close()

            if (deviceID != 0) {
                stm.executeUpdate(
                    """
                        INSERT INTO TS_device_command_history ( id , 
                            user_id , device_id , object_id , 
                            command , create_time , 
                            send_status , send_time ) VALUES ( 
                            ${stm.getNextID("TS_device_command_history", "id")} , 
                            ${userConfig.userId} , $deviceID , $objectId , 
                            '$command' , ${getCurrentTimeInt()} , 
                            0 , 0 )  
                    """
                )
            }
        }

        return XyActionResponse()
    }

    override fun addElement(xyActionRequest: XyActionRequest, userID: Int) = XyActionResponse()
    override fun editElementPoint(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun moveElements(xyActionRequest: XyActionRequest) = XyActionResponse()

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadDynamicElements(scale: Int, objectParamData: XyStartObjectParsedData, alElement: MutableList<XyElement>) {
        //--- получить данные по правам доступа
        val isRemoteControlPermission = userConfig.userPermission["ts_object"]?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

        alElement.addAll(getElementList(scale, objectParamData, isRemoteControlPermission))
    }

    private fun getElementList(scale: Int, objectParamData: XyStartObjectParsedData, isRemoteControlPermission: Boolean): List<XyElement> {
        val objectConfig = (application as iTSApplication).getObjectConfig(userConfig, objectParamData.objectId)
        val objectState = ObjectState.getState(stm, objectConfig)

        val alResult = mutableListOf<XyElement>()

        //--- left textual column

        var x = GRID_STEP * 0
        var y = GRID_STEP * 0

        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.lastDateTime?.let { lastDataTime ->
                DateTime_YMDHMS(zoneId, lastDataTime)
            } ?: "(нет данных)",
            tooltip = "Время последних данных",
            textColor = 0xFF_00_00_00.toInt()
        )
        y += GRID_STEP / 2
        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Уровень сигнала: " + (
                objectState.tmSignalLevel.values.firstOrNull()?.let { signalLevel ->
                    getSplittedDouble(signalLevel, 0, true, '.') + " %"
                } ?: "(нет данных)"
                ),
            textColor = 0xFF_00_00_00.toInt()
        )

        y += GRID_STEP

        //--- left state indicators

        val curState = objectState.tmStateValue.values.firstOrNull()

        alResult += createStateText(objectConfig.objectId, x, y, "Подъём", SensorConfigState.STATE_UP, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Спуск", SensorConfigState.STATE_DOWN, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Парковка", SensorConfigState.STATE_PARKING, curState)

        y += GRID_STEP

        alResult += createStateText(objectConfig.objectId, x, y, "Ожидание следующей чистки", SensorConfigState.STATE_WAIT_CLEAN, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Ожидание включения ЭЦН", SensorConfigState.STATE_WAIT_ECN, curState)

        y += GRID_STEP

        alResult += createStateText(objectConfig.objectId, x, y, "Непроход вниз", SensorConfigState.STATE_UNPASS_DOWN, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Непроход вверх", SensorConfigState.STATE_UNPASS_UP, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Обрыв проволоки", SensorConfigState.STATE_WIRE_RUNOUT, curState)
        y += GRID_STEP / 2
        alResult += createStateText(objectConfig.objectId, x, y, "Ошибка привода", SensorConfigState.STATE_DRIVE_PROTECT, curState)

        //--- middle textual column

        x += GRID_STEP * 6
        y = GRID_STEP * 0

        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Глубина [м]",
            incFontSize = 4,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )
        y += GRID_STEP / 2
        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmDepthValue.values.firstOrNull()?.let { depth ->
                getSplittedDouble(depth, 0, true, '.')
            } ?: "(неизвестно)",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )
        y += GRID_STEP * 2

        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Скорость [м/ч]",
            incFontSize = 4,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )
        y += GRID_STEP / 2
        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmSpeedValue.values.firstOrNull()?.let { speed ->
                getSplittedDouble(speed, 0, true, '.')
            } ?: "(неизвестно)",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )
        y += GRID_STEP * 2

        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Нагрузка [%]",
            incFontSize = 4,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )
        y += GRID_STEP / 2
        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmLoadValue.values.firstOrNull()?.let { load ->
                getSplittedDouble(load, 0, true, '.')
            } ?: "(неизвестно)",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            limitWidth = GRID_STEP * 2,
            anchorX = XyElement.Anchor.RB,
            alignX = XyElement.Align.RB,
        )

        y += GRID_STEP * 2

        alResult += createText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Следующая чистка начнётся: " + (
                objectState.tmNextCleaningDateTime.values.firstOrNull()?.let { nextDataTime ->
                    DateTime_YMDHMS(zoneId, nextDataTime)
                } ?: "(нет данных)"),
        )

        //--- first right depth axis block

        x += GRID_STEP * 2
        y = GRID_STEP * 0

        val DEPTH_SCALE_COUNT = 5

        objectConfig.hmSensorConfig[SensorConfig.SENSOR_DEPTH]?.entries?.firstOrNull()?.let { (portNum, sc) ->
            val sca = sc as SensorConfigAnalogue

            alResult += createText(objectConfig.objectId, x, y, "Глубина [м]", null, 0xFF_00_00_00.toInt())
            y += GRID_STEP

            alResult += createAxis(objectConfig.objectId, x, y, DEPTH_SCALE_COUNT)
            (0..DEPTH_SCALE_COUNT).forEach { i ->
                alResult += createNotchLine(objectConfig.objectId, x, y + GRID_STEP * i)
                alResult += createNotchText(
                    objectId = objectConfig.objectId,
                    x = x,
                    y = y + GRID_STEP * i,
                    text = getSplittedDouble(sca.maxView - (sca.maxView - sca.minView) / DEPTH_SCALE_COUNT * i, 0, true, '.')
                )
            }

            objectState.tmDepthValue.values.firstOrNull()?.let { depth ->
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (depth - sca.minView) / (sca.maxView - sca.minView) * (downY - y)
                alResult += createValueBar(objectConfig.objectId, x, topY.toInt(), x + GRID_STEP / 2, downY, 0xFF_00_FF_00.toInt())
            }

            objectState.tmSetupValue[cDevice.CLEANING_DEPTH_SHOW_POS]?.let { cleaningDepth ->
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (cleaningDepth.toDouble() - sca.minView) / (sca.maxView - sca.minView) * (downY - y)
                alResult += createArrow(objectConfig.objectId, scale, x - GRID_STEP / 4, topY.toInt(), 0, 0xFF_FF_00_00.toInt(), "Глубина очистки [м]")
                alResult += createText(
                    objectId = objectConfig.objectId,
                    x = x - GRID_STEP * 3 / 4,
                    y = topY.toInt() - GRID_STEP / 8,
                    text = getSplittedDouble(cleaningDepth.toDouble(), 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    limitWidth = GRID_STEP,
                    anchorX = XyElement.Anchor.RB,
                    alignX = XyElement.Align.RB,
                )
            }

            objectState.tmSetupValue[cDevice.PARKING_DEPTH_SHOW_POS]?.let { parkingDepth ->
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (parkingDepth.toDouble() - sca.minView) / (sca.maxView - sca.minView) * (downY - y)
                alResult += createArrow(objectConfig.objectId, scale, x + GRID_STEP * 3 / 4, topY.toInt(), 180, 0xFF_00_00_FF.toInt(), "Глубина парковки скребка [м]")
                alResult += createText(
                    objectId = objectConfig.objectId,
                    x = x + GRID_STEP,
                    y = topY.toInt() - GRID_STEP / 8,
                    text = getSplittedDouble(parkingDepth.toDouble(), 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    limitWidth = GRID_STEP,
                    anchorX = XyElement.Anchor.LT,
                    alignX = XyElement.Align.LT,
                )
            }
        }

        //--- second right load axis block

        x += GRID_STEP * 3
        y = GRID_STEP * 0

        val LOAD_SCALE_COUNT = 5

        objectConfig.hmSensorConfig[SensorConfig.SENSOR_LOAD]?.entries?.firstOrNull()?.let { (portNum, sc) ->
            val sca = sc as SensorConfigAnalogue

            alResult += createText(objectConfig.objectId, x, y, "Нагрузка [%]", null, 0xFF_00_00_00.toInt())
            y += GRID_STEP

            alResult += createAxis(objectConfig.objectId, x, y, LOAD_SCALE_COUNT)
            (0..LOAD_SCALE_COUNT).forEach { i ->
                alResult += createNotchLine(objectConfig.objectId, x, y + GRID_STEP * i)
                alResult += createNotchText(objectConfig.objectId, x, y + GRID_STEP * i, getSplittedDouble(sca.maxView - (sca.maxView - sca.minView) / LOAD_SCALE_COUNT * i, 0, true, '.'))
            }

            objectState.tmLoadValue.values.firstOrNull()?.let { load ->
                val downY = y + GRID_STEP * LOAD_SCALE_COUNT
                val topY = downY - (load - sca.minView) / (sca.maxView - sca.minView) * (downY - y)
                alResult += createValueBar(objectConfig.objectId, x, topY.toInt(), x + GRID_STEP / 2, downY, 0xFF_00_FF_FF.toInt())
            }

            objectState.tmSetupValue[cDevice.DRIVE_LOAD_RESTRICT_SHOW_POS]?.let { driveLoadRestict ->
                val downY = y + GRID_STEP * LOAD_SCALE_COUNT
                val topY = downY - (driveLoadRestict.toDouble() - sca.minView) / (sca.maxView - sca.minView) * (downY - y)
                alResult += createArrow(objectConfig.objectId, scale, x - GRID_STEP / 4, topY.toInt(), 0, 0xFF_FF_00_00.toInt(), "Ограничение нагрузки на привод [%]")
                alResult += createText(
                    objectId = objectConfig.objectId,
                    x = x - GRID_STEP * 3 / 4,
                    y = topY.toInt() - GRID_STEP / 8,
                    text = getSplittedDouble(driveLoadRestict.toDouble(), 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    limitWidth = GRID_STEP,
                    anchorX = XyElement.Anchor.RB,
                    alignX = XyElement.Align.RB,
                )
            }
        }

        //--- command button block

        x += GRID_STEP * 3
        y = GRID_STEP * 0

        if (isRemoteControlPermission) {
            alResult += createClickableText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "\nСлепой подъём\n\n",
                tooltip = "Вы хотите запустить слепой подъем?",
                backColor = SensorConfigState.COLOR_PURPLE_BRIGHT,
            ).also { clickableText ->
                //--- set command to this element
                chmElementCommand[clickableText.elementId] = "clstart"
                //--- set sensor port_num (for define device_id over device_index over port_num)
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alResult += createClickableText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "\nНачать спуск\n\n",
                tooltip = "Вы хотите начать спуск?",
                backColor = SensorConfigState.COLOR_GREEN_BRIGHT,
            ).also { clickableText ->
                chmElementCommand[clickableText.elementId] = "cldesc"
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alResult += createClickableText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "\nПерезапуск станции\n\n",
                tooltip = "Вы хотите перезапустить УДС?",
                backColor = SensorConfigState.COLOR_BLUE_BRIGHT,
            ).also { clickableText ->
                chmElementCommand[clickableText.elementId] = "reset"
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alResult += createClickableText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "\nОстановить работу\n\n",
                tooltip = "Вы хотите остановить работу станции?",
                backColor = SensorConfigState.COLOR_ORANGE_BRIGHT,
            ).also { clickableText ->
                chmElementCommand[clickableText.elementId] = "stop"
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
        }

        return alResult
    }

    private fun createStateText(objectId: Int, x: Int, y: Int, text: String, forState: Int, curState: Int?) = createText(
        objectId = objectId,
        x = x,
        y = y,
        text = text,
        textColor = if (forState == curState) {
            0xFF_00_00_00.toInt()
        } else {
            0xFF_80_80_80.toInt()
        },
        fillColor = if (forState == curState) {
            SensorConfigState.hmStateInfo[forState]?.brightColor ?: SensorConfigState.COLOR_UNKNOWN_BRIGHT
        } else {
            SensorConfigState.hmStateInfo[forState]?.darkColor ?: SensorConfigState.COLOR_UNKNOWN_DARK
        },
        drawColor = SensorConfigState.hmStateInfo[forState]?.darkColor ?: SensorConfigState.COLOR_UNKNOWN_DARK,
        isBold = forState == curState,
        limitWidth = GRID_STEP * 3,
    )

    private fun createAxis(objectId: Int, x: Int, y: Int, scaleCount: Int) =
        XyElement(TYPE_STATE_AXIS, -getRandomInt(), objectId).apply {
            itReadOnly = true

            itClosed = false
            alPoint = arrayOf(XyPoint(x, y), XyPoint(x, y + GRID_STEP * scaleCount))

            drawColor = 0xFF_00_00_00.toInt()
            lineWidth = 1
        }

    private fun createNotchLine(objectId: Int, x: Int, y: Int) =
        XyElement(TYPE_STATE_AXIS, -getRandomInt(), objectId).apply {
            itReadOnly = true

            itClosed = false
            alPoint = arrayOf(XyPoint(x, y), XyPoint(x + GRID_STEP / 2, y))

            drawColor = 0xFF_00_00_00.toInt()
            lineWidth = 1
        }

    private fun createNotchText(objectId: Int, x: Int, y: Int, text: String) = createText(
        objectId = objectId,
        x = x,
        y = y,
        text = text,
        textColor = 0xFF_00_00_00.toInt()
    )

    private fun createValueBar(objectId: Int, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) =
        XyElement(TYPE_STATE_VALUE_BAR, -getRandomInt(), objectId).apply {
            itReadOnly = true

            itClosed = true
            alPoint = arrayOf(XyPoint(x1, y1), XyPoint(x2, y1), XyPoint(x2, y2), XyPoint(x1, y2))

            drawColor = color
            fillColor = color

            lineWidth = 1
        }

    private fun createArrow(objectId: Int, scale: Int, x: Int, y: Int, angle: Int, color: Int, toolTipText: String) =
        XyElement(TYPE_STATE_AXIS_POINTER, -getRandomInt(), objectId).apply {
            itReadOnly = true
            alPoint = arrayOf(XyPoint(x, y))
            this.toolTipText = toolTipText

            markerType = XyElement.MarkerType.ARROW
            markerSize = GRID_STEP / 4 / scale
            rotateDegree = angle.toDouble()

            drawColor = color
            fillColor = color
            lineWidth = 1
        }

    private fun createClickableText(objectId: Int, x: Int, y: Int, text: String, tooltip: String?, backColor: Int) =
        createText(
            objectId = objectId,
            x = x,
            y = y,
            text = text,
            tooltip = tooltip,
            fillColor = backColor,
            isBold = true,
            limitWidth = GRID_STEP * 3,
            alignX = XyElement.Align.CC,
        ).apply {
            itReadOnly = false
        }

    private fun createText(
        objectId: Int,
        x: Int,
        y: Int,
        text: String,
        tooltip: String? = null,
        textColor: Int = 0xFF_00_00_00.toInt(),
        fillColor: Int = 0,
        drawColor: Int = 0,
        incFontSize: Int? = null,
        isBold: Boolean = false,
        limitWidth: Int = 0,
        limitHeight: Int = 0,
        anchorX: XyElement.Anchor = XyElement.Anchor.LT,
        anchorY: XyElement.Anchor = XyElement.Anchor.LT,
        alignX: XyElement.Align = XyElement.Align.LT,
        alignY: XyElement.Align = XyElement.Align.LT,
    ) =
        XyElement(TYPE_STATE_TEXT, -getRandomInt(), objectId).apply {
            this.itReadOnly = true
            this.alPoint = arrayOf(XyPoint(x, y))

            this.text = text
            this.toolTipText = tooltip ?: text
            this.textColor = textColor
            
            this.drawColor = drawColor
            this.fillColor = fillColor
            
            this.fontSize = iCoreAppContainer.BASE_FONT_SIZE + (incFontSize ?: 0)
            this.itFontBold = isBold

            this.limitWidth = limitWidth
            this.limitHeight = limitHeight

            this.anchorX = anchorX
            this.anchorY = anchorY
            this.alignX = alignX
            this.alignY = alignY

        }

}