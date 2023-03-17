package foatto.ts.core_ts.xy.server.document

import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.XyDocumentConfig
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.document.sdcXyState
import foatto.core_server.ds.CoreTelematicFunction
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.cObject
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.core_ts.device.cDevice
import foatto.ts.core_ts.device.cDeviceCommandHistory
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication
import foatto.ts_core.app.DeviceCommand
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class sdcTSState : sdcXyState() {

    companion object {
        //--- arrange in ascending order of the level of location
        const val TYPE_STATE_AXIS = "ts_state_axis"
        const val TYPE_STATE_TEXT_BACK = "ts_state_text_back"
        const val TYPE_STATE_TEXT_TEXT = "ts_state_text_text"
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
        aChmSession: ConcurrentHashMap<String, Any>,
        aUserConfig: UserConfig,
        aDocumentConfig: XyDocumentConfig
    ) {

        //--- schemes

        hmInAliasElementType["ts_object"] = arrayOf(
            TYPE_STATE_AXIS,
            TYPE_STATE_AXIS_POINTER,
            TYPE_STATE_TEXT_BACK,
            TYPE_STATE_TEXT_TEXT,
            TYPE_STATE_VALUE_BAR,
        )

        hmOutElementTypeAlias[TYPE_STATE_AXIS] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_AXIS_POINTER] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_TEXT_BACK] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_TEXT_TEXT] = "ts_object"
        hmOutElementTypeAlias[TYPE_STATE_VALUE_BAR] = "ts_object"

        super.init(aApplication, aConn, aChmSession, aUserConfig, aDocumentConfig)
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
            val alResult = mutableListOf<XyElement>()
            getElementList(1, objectParamData, isRemoteControlPermission, alResult, mutableMapOf())
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
            minCoord = XyPoint(prjX1 - GRID_STEP * 1, prjY1 - GRID_STEP),
            maxCoord = XyPoint(prjX2 + GRID_STEP * 3, prjY2 + GRID_STEP)
        )
    }

    override fun getOneElement(xyActionRequest: XyActionRequest) = XyActionResponse()

    override fun clickElement(xyActionRequest: XyActionRequest): XyActionResponse {
        val elementId = xyActionRequest.elementId!!
        val objectId = xyActionRequest.objectId!!

        val command = chmElementCommand[elementId]
        if (!command.isNullOrBlank()) {
            var deviceId = 0
            val deviceIndex = chmElementPort[elementId]!! / CoreTelematicFunction.MAX_PORT_PER_DEVICE
            val rs = conn.executeQuery(
                """
                    SELECT id FROM TS_device WHERE object_id = $objectId AND device_index = $deviceIndex
                """
            )
            if (rs.next()) {
                deviceId = rs.getInt(1)
            }
            rs.close()

            if (deviceId != 0) {
                cDeviceCommandHistory.addDeviceCommand(
                    conn = conn,
                    userId = userConfig.userId,
                    deviceId = deviceId,
                    objectId = objectId,
                    command = command,
                )
            }
        }

        return XyActionResponse()
    }

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
        //--- получить данные по правам доступа
        val isRemoteControlPermission = userConfig.userPermission["ts_object"]?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

        getElementList(scale, objectParamData, isRemoteControlPermission, alElement, hmParams)
    }

    private fun getElementList(
        scale: Int,
        objectParamData: XyStartObjectParsedData,
        isRemoteControlPermission: Boolean,
        alElement: MutableList<XyElement>,
        hmParams: MutableMap<String, String>,
    ) {
        val objectConfig = (application as iTSApplication).getObjectConfig(userConfig, objectParamData.objectId)
        val objectState = ObjectState.getState(conn, objectConfig)

        //--- left textual column

        var x = GRID_STEP * 0
        var y = GRID_STEP * 0

        alElement += createTextText(
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
        alElement += createTextText(
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

        if (curState == SensorConfigState.STATE_STOPPED_BY_SERVER) {
            //!!! предусмотреть кастомизацию сообщение в зависимости от типа объекта/устройства
            hmParams[STATE_ALERT_MESSAGE] = "Работа УДС-Техно остановлена.\nТребуется выезд на скважину для перезагрузки станции."
        }
        else if (curState == SensorConfigState.STATE_BLOCKED_BY_SERVER) {
            //!!! предусмотреть кастомизацию сообщение в зависимости от типа объекта/устройства
            hmParams[STATE_ALERT_MESSAGE] = "Работа УДС-Техно заблокирована."
        }

        alElement += createStateText(objectConfig.objectId, scale, x, y, "Подъём", SensorConfigState.STATE_UP, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Спуск", SensorConfigState.STATE_DOWN, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Парковка", SensorConfigState.STATE_PARKING, curState)

        y += GRID_STEP

        alElement += createStateText(objectConfig.objectId, scale, x, y, "Ожидание следующей чистки", SensorConfigState.STATE_WAIT_CLEAN, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Ожидание включения ЭЦН", SensorConfigState.STATE_WAIT_ECN, curState)

        y += GRID_STEP

        alElement += createStateText(objectConfig.objectId, scale, x, y, "Непроход вниз", SensorConfigState.STATE_UNPASS_DOWN, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Непроход вверх", SensorConfigState.STATE_UNPASS_UP, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Обрыв проволоки", SensorConfigState.STATE_WIRE_RUNOUT, curState)
        y += GRID_STEP * 3 / 4
        alElement += createStateText(objectConfig.objectId, scale, x, y, "Ошибка привода", SensorConfigState.STATE_DRIVE_PROTECT, curState)

        //--- middle textual column

        x += GRID_STEP * 13
        y = GRID_STEP * 0

        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Глубина [м]",
            incFontSize = 4,
            anchorX = XyElement.Anchor.RB,
        )
        y += GRID_STEP
        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmDepthValue.values.firstOrNull()?.let { depth ->
                getSplittedDouble(depth, 0, true, '.')
            } ?: "-",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            anchorX = XyElement.Anchor.RB,
        )
        y += GRID_STEP * 2

        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Скорость [м/ч]",
            incFontSize = 4,
            anchorX = XyElement.Anchor.RB,
        )
        y += GRID_STEP
        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmSpeedValue.values.firstOrNull()?.let { speed ->
                getSplittedDouble(speed, 0, true, '.')
            } ?: "-",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            anchorX = XyElement.Anchor.RB,
        )
        y += GRID_STEP * 2

        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = "Нагрузка [%]",
            incFontSize = 4,
            anchorX = XyElement.Anchor.RB,
        )
        y += GRID_STEP
        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x,
            y = y,
            text = objectState.tmLoadValue.values.firstOrNull()?.let { load ->
                getSplittedDouble(load, 0, true, '.')
            } ?: "-",
            textColor = 0xFF_80_00_00.toInt(),
            incFontSize = 32,
            isBold = true,
            anchorX = XyElement.Anchor.RB,
        )

        y += GRID_STEP * 2

        alElement += createTextText(
            objectId = objectConfig.objectId,
            x = x - GRID_STEP * 3,
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

            alElement += createTextText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "Глубина [м]",
                tooltip = null,
                textColor = 0xFF_00_00_00.toInt()
            )
            y += GRID_STEP

            alElement += createAxis(objectConfig.objectId, x, y, DEPTH_SCALE_COUNT)
            (0..DEPTH_SCALE_COUNT).forEach { i ->
                alElement += createNotchLine(objectConfig.objectId, x, y + GRID_STEP * i)
                alElement += createNotchText(
                    objectId = objectConfig.objectId,
                    x = x + 4 * scale,
                    y = y + GRID_STEP * i + 4 * scale,
                    text = getSplittedDouble(sca.maxStateView - (sca.maxStateView - sca.minStateView) / DEPTH_SCALE_COUNT * i, 0, true, '.')
                )
            }

            objectState.tmDepthValue.values.firstOrNull()?.let { depth ->
                //--- скорректированное значение, чтобы указатель не уходил за края шкалы
                val correctedDepth = getCorrectedValue(sca, depth)
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (correctedDepth - sca.minStateView) / (sca.maxStateView - sca.minStateView) * (downY - y)
                alElement += createValueBar(objectConfig.objectId, x, topY.toInt(), x + GRID_STEP / 2, downY, 0xFF_00_FF_00.toInt())
            }

            objectState.tmSetupValue[cDevice.CLEANING_DEPTH_SHOW_POS]?.let { cleaningDepthStr ->
                val cleaningDepth = cleaningDepthStr.replace(" ", "").toDouble()
                //--- скорректированное значение, чтобы указатель не уходил за края шкалы
                val correctedCleaningDepth = getCorrectedValue(sca, cleaningDepth)
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (correctedCleaningDepth - sca.minStateView) / (sca.maxStateView - sca.minStateView) * (downY - y)
                alElement += createArrow(
                    objectId = objectConfig.objectId,
                    scale = scale,
                    x = x - GRID_STEP / 4,
                    y = topY.toInt(),
                    angle = 0,
                    color = 0xFF_FF_00_00.toInt(),
                    toolTipText = "Глубина очистки [м]"
                )
                alElement += createTextText(
                    objectId = objectConfig.objectId,
                    x = x - GRID_STEP / 2,
                    y = topY.toInt(),
                    text = getSplittedDouble(cleaningDepth, 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    anchorX = XyElement.Anchor.RB,
                    anchorY = XyElement.Anchor.CC,
                )
            }

            objectState.tmSetupValue[cDevice.PARKING_DEPTH_SHOW_POS]?.let { parkingDepthStr ->
                val parkingDepth = parkingDepthStr.replace(" ", "").toDouble()
                //--- скорректированное значение, чтобы указатель не уходил за края шкалы
                val correctedParkingDepth = getCorrectedValue(sca, parkingDepth)
                val downY = y + GRID_STEP * DEPTH_SCALE_COUNT
                val topY = downY - (correctedParkingDepth - sca.minStateView) / (sca.maxStateView - sca.minStateView) * (downY - y)
                alElement += createArrow(
                    objectId = objectConfig.objectId,
                    scale = scale,
                    x = x - GRID_STEP / 4,
                    y = topY.toInt(),
                    angle = 0,
                    color = 0xFF_00_00_FF.toInt(),
                    toolTipText = "Глубина парковки скребка [м]"
                )
                alElement += createTextText(
                    objectId = objectConfig.objectId,
                    x = x - GRID_STEP / 2,
                    //x = x + GRID_STEP * 8 / 4,
                    y = topY.toInt(),
                    text = getSplittedDouble(parkingDepth, 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    anchorX = XyElement.Anchor.RB,
                    //anchorX = XyElement.Anchor.LT,
                    anchorY = XyElement.Anchor.CC,
                )
            }
        }

        //--- second right load axis block

        x += GRID_STEP * 4
        y = GRID_STEP * 0

        val LOAD_SCALE_COUNT = 5

        objectConfig.hmSensorConfig[SensorConfig.SENSOR_LOAD]?.entries?.firstOrNull()?.let { (_, sc) ->
            val sca = sc as SensorConfigAnalogue

            alElement += createTextText(
                objectId = objectConfig.objectId,
                x = x,
                y = y,
                text = "Нагрузка [%]",
                tooltip = null,
                dialogQuestion = null,
                textColor = 0xFF_00_00_00.toInt()
            )
            y += GRID_STEP

            alElement += createAxis(objectConfig.objectId, x, y, LOAD_SCALE_COUNT)
            (0..LOAD_SCALE_COUNT).forEach { i ->
                alElement += createNotchLine(objectConfig.objectId, x, y + GRID_STEP * i)
                alElement += createNotchText(
                    objectId = objectConfig.objectId,
                    x = x + 4 * scale,
                    y = y + GRID_STEP * i + 4 * scale,
                    text = getSplittedDouble(sca.maxStateView - (sca.maxStateView - sca.minStateView) / LOAD_SCALE_COUNT * i, 0, true, '.')
                )
            }

            objectState.tmLoadValue.values.firstOrNull()?.let { load ->
                //--- скорректированное значение, чтобы указатель не уходил за края шкалы
                val correctedLoad = getCorrectedValue(sca, load)
                val downY = y + GRID_STEP * LOAD_SCALE_COUNT
                val topY = downY - (correctedLoad - sca.minStateView) / (sca.maxStateView - sca.minStateView) * (downY - y)
                alElement += createValueBar(objectConfig.objectId, x, topY.toInt(), x + GRID_STEP / 2, downY, 0xFF_00_FF_FF.toInt())
            }

            objectState.tmSetupValue[cDevice.DRIVE_LOAD_RESTRICT_SHOW_POS]?.let { driveLoadRestictStr ->
                val driveLoadRestict = driveLoadRestictStr.replace(" ", "").toDouble()
                //--- скорректированное значение, чтобы указатель не уходил за края шкалы
                val correctedDriveLoadRestict = getCorrectedValue(sca, driveLoadRestict)
                val downY = y + GRID_STEP * LOAD_SCALE_COUNT
                val topY = downY - (correctedDriveLoadRestict - sca.minStateView) / (sca.maxStateView - sca.minStateView) * (downY - y)
                alElement += createArrow(objectConfig.objectId, scale, x - GRID_STEP / 4, topY.toInt(), 0, 0xFF_FF_00_00.toInt(), "Ограничение нагрузки на привод [%]")
                alElement += createTextText(
                    objectId = objectConfig.objectId,
                    x = x - GRID_STEP / 2,
                    y = topY.toInt(),
                    text = getSplittedDouble(driveLoadRestict, 0, true, '.'),
                    textColor = 0xFF_00_00_00.toInt(),
                    anchorX = XyElement.Anchor.RB,
                    anchorY = XyElement.Anchor.CC,
                )
            }
        }

        //--- command button block

        x += GRID_STEP * 8
        y = GRID_STEP

        if (isRemoteControlPermission) {
            alElement += createClickableText(
                objectId = objectConfig.objectId,
                scale = scale,
                x = x,
                y = y,
                text = "\nПодъём и парковка\n\n",
                dialogQuestion = "Начать подъём и парковку?",
                backColor = SensorConfigState.COLOR_PURPLE_BRIGHT,
            ).onEach { clickableText ->
                //--- set command to this element
                chmElementCommand[clickableText.elementId] = DeviceCommand.CMD_START_BLIND_CLIMB
                //--- set sensor port_num (for define device_id over device_index over port_num)
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alElement += createClickableText(
                objectId = objectConfig.objectId,
                scale = scale,
                x = x,
                y = y,
                text = "\nРабочий цикл\n\n",
                dialogQuestion = "Запустить рабочий цикл?",
                backColor = SensorConfigState.COLOR_GREEN_BRIGHT,
            ).onEach { clickableText ->
                chmElementCommand[clickableText.elementId] = DeviceCommand.CMD_START_DOWN
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alElement += createClickableText(
                objectId = objectConfig.objectId,
                scale = scale,
                x = x,
                y = y,
                text = "\nПерезапуск станции\n\n",
                dialogQuestion = "Перезапустить станцию?",
                backColor = SensorConfigState.COLOR_BLUE_BRIGHT,
            ).onEach { clickableText ->
                chmElementCommand[clickableText.elementId] = DeviceCommand.CMD_RESTART
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
            y += GRID_STEP * 2
            alElement += createClickableText(
                objectId = objectConfig.objectId,
                scale = scale,
                x = x,
                y = y,
                text = "\nОстановить работу\n\n",
                dialogQuestion = "Остановить работу станции?",
                backColor = SensorConfigState.COLOR_ORANGE_BRIGHT,
            ).onEach { clickableText ->
                chmElementCommand[clickableText.elementId] = DeviceCommand.CMD_STOP
                chmElementPort[clickableText.elementId] = 0 // sc.portNum - now used one device per object only
            }
        }
    }

    private fun createStateText(
        objectId: Int,
        scale: Int,
        x: Int,
        y: Int,
        text: String,
        forState: Int,
        curState: Int?
    ) = createFilledText(
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
        incFontSize = 0,
        isBold = forState == curState,
        limitWidth = GRID_STEP * 8,
        limitHeight = (iCoreAppContainer.BASE_FONT_SIZE + 3) * scale,
        borderExpand = 4 * scale,
        anchorX = XyElement.Anchor.LT,
        anchorY = XyElement.Anchor.CC,
    )

    private fun createAxis(objectId: Int, x: Int, y: Int, scaleCount: Int) =
        XyElement(TYPE_STATE_AXIS, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = false
            alPoint = arrayOf(XyPoint(x, y), XyPoint(x, y + GRID_STEP * scaleCount))

            drawColor = 0xFF_00_00_00.toInt()
            lineWidth = 1
        }

    private fun createNotchLine(objectId: Int, x: Int, y: Int) =
        XyElement(TYPE_STATE_AXIS, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = false
            alPoint = arrayOf(XyPoint(x, y), XyPoint(x + GRID_STEP / 2, y))

            drawColor = 0xFF_00_00_00.toInt()
            lineWidth = 1
        }

    private fun createNotchText(objectId: Int, x: Int, y: Int, text: String) = createTextText(
        objectId = objectId,
        x = x,
        y = y,
        text = text,
        textColor = 0xFF_00_00_00.toInt()
    )

    private fun createValueBar(objectId: Int, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) =
        XyElement(TYPE_STATE_VALUE_BAR, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = true
            alPoint = arrayOf(XyPoint(x1, y1), XyPoint(x2, y1), XyPoint(x2, y2), XyPoint(x1, y2))

            drawColor = color
            fillColor = color

            lineWidth = 1
        }

    private fun createArrow(objectId: Int, scale: Int, x: Int, y: Int, angle: Int, color: Int, toolTipText: String) =
        XyElement(TYPE_STATE_AXIS_POINTER, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = arrayOf(XyPoint(x, y))
            this.toolTipText = toolTipText

            markerType = XyElement.MarkerType.ARROW
            markerSize = GRID_STEP / 4 / scale
            rotateDegree = angle.toDouble()

            drawColor = color
            fillColor = color
            lineWidth = 1
        }

    private fun createClickableText(
        objectId: Int,
        scale: Int,
        x: Int,
        y: Int,
        text: String,
        dialogQuestion: String?,
        backColor: Int
    ) =
        createFilledText(
            objectId = objectId,
            x = x,
            y = y,
            text = text,
            dialogQuestion = dialogQuestion,
            fillColor = backColor,
            isBold = true,
            limitWidth = GRID_STEP * 6,
            limitHeight = (iCoreAppContainer.BASE_FONT_SIZE + 48) * scale,
            anchorX = XyElement.Anchor.CC,
            anchorY = XyElement.Anchor.CC,
        ).onEach { element ->
            element.isReadOnly = false
        }

    private fun createFilledText(
        objectId: Int,
        x: Int,
        y: Int,
        text: String,
        tooltip: String? = null,
        dialogQuestion: String? = null,
        textColor: Int = 0xFF_00_00_00.toInt(),
        fillColor: Int = 0,
        drawColor: Int = 0,
        incFontSize: Int? = null,
        isBold: Boolean = false,
        limitWidth: Int = 0,
        limitHeight: Int = 0,
        borderExpand: Int = 0,
        borderShiftY: Int = 0,
        anchorX: XyElement.Anchor = XyElement.Anchor.LT,
        anchorY: XyElement.Anchor = XyElement.Anchor.LT,
    ) = listOf(
        createTextBack(
            objectId = objectId,
            x = x,
            y = y,
            tooltip = tooltip,
            dialogQuestion = dialogQuestion,
            fillColor = fillColor,
            drawColor = drawColor,
            limitWidth = limitWidth,
            limitHeight = limitHeight,
            borderExpand = borderExpand,
            borderShiftY = borderShiftY,
            anchorX = anchorX,
            anchorY = anchorY,
        ),
        createTextText(
            objectId = objectId,
            x = x,
            y = y,
            text = text,
            tooltip = tooltip,
            dialogQuestion = dialogQuestion,
            textColor = textColor,
            incFontSize = incFontSize,
            isBold = isBold,
            anchorX = anchorX,
            anchorY = anchorY,
        )
    )

    private fun createTextBack(
        objectId: Int,
        x: Int,
        y: Int,
        tooltip: String? = null,
        dialogQuestion: String? = null,
        fillColor: Int = 0,
        drawColor: Int = 0,
        limitWidth: Int = 0,
        limitHeight: Int = 0,
        borderExpand: Int = 0,
        borderShiftY: Int = 0,
        anchorX: XyElement.Anchor = XyElement.Anchor.LT,
        anchorY: XyElement.Anchor = XyElement.Anchor.LT,
    ) =
        XyElement(TYPE_STATE_TEXT_BACK, -getRandomInt(), objectId).apply {
            this.isReadOnly = true

            this.toolTipText = tooltip ?: ""

            val newX = when (anchorX.toString()) {
                XyElement.Anchor.LT.toString() -> x
                XyElement.Anchor.RB.toString() -> x - limitWidth
                else -> x - limitWidth / 2
            }

            val newY = when (anchorY.toString()) {
                XyElement.Anchor.LT.toString() -> y
                XyElement.Anchor.RB.toString() -> y - limitHeight
                else -> y - limitHeight / 2
            }

            this.isClosed = true
            this.alPoint = arrayOf(
                XyPoint(newX - borderExpand, newY - borderExpand + borderShiftY),
                XyPoint(newX + limitWidth + borderExpand, newY - borderExpand + borderShiftY),
                XyPoint(newX + limitWidth + borderExpand, newY + limitHeight + borderExpand + borderShiftY),
                XyPoint(newX - borderExpand, newY + limitHeight + borderExpand + borderShiftY),
            )

            this.drawColor = drawColor
            this.fillColor = fillColor

            this.lineWidth = 1

            this.dialogQuestion = dialogQuestion ?: ""
        }

    private fun createTextText(
        objectId: Int,
        x: Int,
        y: Int,
        text: String,
        tooltip: String? = null,
        dialogQuestion: String? = null,
        textColor: Int = 0xFF_00_00_00.toInt(),
        incFontSize: Int? = null,
        isBold: Boolean = false,
        anchorX: XyElement.Anchor = XyElement.Anchor.LT,
        anchorY: XyElement.Anchor = XyElement.Anchor.LT,
    ) =
        XyElement(TYPE_STATE_TEXT_TEXT, -getRandomInt(), objectId).apply {
            this.isReadOnly = true
            this.alPoint = arrayOf(XyPoint(x, y))

            this.text = text
            this.toolTipText = tooltip ?: ""
            this.textColor = textColor

            this.fontSize = iCoreAppContainer.BASE_FONT_SIZE + (incFontSize ?: 0)
            this.isFontBold = isBold

            this.anchorX = anchorX
            this.anchorY = anchorY

            this.dialogQuestion = dialogQuestion ?: ""
        }

    private fun getCorrectedValue(sca: SensorConfigAnalogue, curValue: Double) =
        if (curValue < sca.minStateView) {
            sca.minStateView
        } else if (curValue > sca.maxStateView) {
            sca.maxStateView
        } else {
            curValue
        }

}