package foatto.mms.core_mms.xy.server.document

import foatto.core.app.STATE_ELEMENT_MOVE_DESCR
import foatto.core.app.STATE_ELEMENT_MOVE_ENABLED
import foatto.core.app.STATE_ELEMENT_MOVE_ID
import foatto.core.app.STATE_ELEMENT_MOVE_X
import foatto.core.app.STATE_ELEMENT_MOVE_Y
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.XyDocumentConfig
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.core.util.getStringFromIterable
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.document.sdcXyState
import foatto.core_server.ds.CoreTelematicFunction
import foatto.mms.core_mms.cObject
import foatto.mms.core_mms.calc.ObjectState
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.core_mms.sensor.config.SensorConfigSignal
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.mms.core_mms.sensor.config.SignalConfig
import foatto.mms.iMMSApplication
import foatto.sql.CoreAdvancedConnection
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class sdcMMSState : sdcXyState() {

    companion object {
        //--- arrange in ascending order of the level of location
        const val TYPE_STATE_SUM_GROUP_BOX_25D = "mms_state_sum_group_box_25d"
        const val TYPE_STATE_SUM_GROUP_TEXT_25D = "mms_state_sum_group_text_25d"
        const val TYPE_STATE_GROUP_BOX_25D = "mms_state_group_box_25d"
        const val TYPE_STATE_GROUP_TEXT_25D = "mms_state_group_text_25d"
        const val TYPE_STATE_LL_BOTTOM_25D = "mms_state_ll_bottom_25d"
        const val TYPE_STATE_LL_VOLUME_25D = "mms_state_ll_volume_25d"
        const val TYPE_STATE_LL_LEVEL_25D = "mms_state_ll_level_25d"
        const val TYPE_STATE_LL_TANK_WALL_25D = "mms_state_ll_tank_wall_25d"
        const val TYPE_STATE_LL_TANK_TOP_25D = "mms_state_ll_tank_top_25d"
        const val TYPE_STATE_LL_TEXT_25D = "mms_state_ll_text_25d"
        const val TYPE_STATE_W_FIGURE_25D = "mms_state_w_figure_25d"
        const val TYPE_STATE_W_TEXT_25D = "mms_state_w_text_25d"

        const val TYPE_STATE_S_FIGURE_25D = "mms_state_s_figure_25d"
        const val TYPE_STATE_S_TEXT_25D = "mms_state_s_text_25d"

        private const val CRITICAL_LL_PERCENT = 0.1

        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        private const val GRID_STEP = 1024 * 1024

        private const val OBJECT_SCHEME_SIZE = 1024 * 1024 * 1024
        private const val OBJECT_SCHEME_ELEMENT_SIZE_BASE = OBJECT_SCHEME_SIZE / 128 // 64 - крупновато

        //--- binding an object and scheme file URL
        private val chmObjectSchemeUrl = ConcurrentHashMap<Int, String>()

        //--- binding an object and image sizes
        private val chmObjectSchemeSize = ConcurrentHashMap<Int, Pair<Int, Int>>()

        //--- binding an element and a command on it
        private val chmElementCommand = ConcurrentHashMap<Int, Int>()

        //--- binding an element and a port number on it (for the subsequent determination of the deviceID, which is redundant to determine when rendering)
        private val chmElementPort = ConcurrentHashMap<Int, Int>()
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

        hmInAliasElementType["mms_object"] = arrayOf(
            TYPE_STATE_SUM_GROUP_BOX_25D, TYPE_STATE_SUM_GROUP_TEXT_25D, TYPE_STATE_GROUP_BOX_25D, TYPE_STATE_GROUP_TEXT_25D,
            TYPE_STATE_LL_BOTTOM_25D, TYPE_STATE_LL_VOLUME_25D, TYPE_STATE_LL_LEVEL_25D, TYPE_STATE_LL_TANK_WALL_25D, TYPE_STATE_LL_TANK_TOP_25D, TYPE_STATE_LL_TEXT_25D,
            TYPE_STATE_W_FIGURE_25D, TYPE_STATE_W_TEXT_25D,
            TYPE_STATE_S_FIGURE_25D, TYPE_STATE_S_TEXT_25D
        )

        hmOutElementTypeAlias[TYPE_STATE_SUM_GROUP_BOX_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_SUM_GROUP_TEXT_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_GROUP_BOX_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_GROUP_TEXT_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_BOTTOM_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_VOLUME_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_LEVEL_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_TANK_WALL_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_TANK_TOP_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_LL_TEXT_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_W_FIGURE_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_W_TEXT_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_S_FIGURE_25D] = "mms_object"
        hmOutElementTypeAlias[TYPE_STATE_S_TEXT_25D] = "mms_object"

        super.init(aApplication, aConn, aChmSession, aUserConfig, aDocumentConfig)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getCoords(startParamId: String): XyActionResponse {
        val sd = chmSession[AppParameter.XY_START_DATA + startParamId] as XyStartData

        //--- разбор входных параметров
        val alObjectParamData = parseObjectParam(true /*isStartObjectsDefined*/, sd, mutableSetOf())

        //--- check object scheme file existing for first/once object
        val objectId = alObjectParamData.first().objectId
        var schemeFileId = 0
        val rs = conn.executeQuery(" SELECT scheme_file_id FROM MMS_object WHERE id = $objectId ")
        if (rs.next()) {
            schemeFileId = rs.getInt(1)
        }
        rs.close()
        val fileList = application.getFileList(conn, schemeFileId)
        for ((_, url) in fileList) {
            val file = File(application.rootDirName, url)
            if (file.exists() && (url.endsWith(".png", true) || url.endsWith(".jpg", true))) {
                chmObjectSchemeUrl[objectId] = url

                val bi = ImageIO.read(file)
                val imageWidth = bi.width
                val imageHeight = bi.height
                val schemeWidth = if (imageWidth >= imageHeight) {
                    OBJECT_SCHEME_SIZE
                } else {
                    (1.0 * OBJECT_SCHEME_SIZE * imageWidth / imageHeight).toInt()
                }
                val schemeHeight = if (imageHeight >= imageWidth) {
                    OBJECT_SCHEME_SIZE
                } else {
                    (1.0 * OBJECT_SCHEME_SIZE * imageHeight / imageWidth).toInt()
                }
                chmObjectSchemeSize[objectId] = Pair(schemeWidth, schemeHeight)
                break
            }
        }

        return chmObjectSchemeUrl[objectId]?.let {
            val (schemeWidth, schemeHeight) = chmObjectSchemeSize[objectId]!!
            XyActionResponse(
                minCoord = XyPoint(0, 0),
                maxCoord = XyPoint(schemeWidth, schemeHeight),
            )
        } ?: run {

            var prjX1 = Int.MAX_VALUE
            var prjY1 = Int.MAX_VALUE
            var prjX2 = Int.MIN_VALUE
            var prjY2 = Int.MIN_VALUE

            //--- получить данные по правам доступа
            val isRemoteControlPermission = userConfig.userPermission["mms_object"]?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

            //--- отдельная обработка динамических объектов
            for (objectParamData in alObjectParamData) {
                val alResult = getElementList(1, objectParamData, isRemoteControlPermission, mutableMapOf())
                for (e in alResult) {
                    for (p in e.alPoint) {
                        prjX1 = min(prjX1, p.x)
                        prjY1 = min(prjY1, p.y)
                        prjX2 = max(prjX2, p.x)
                        prjY2 = max(prjY2, p.y)
                    }
                }
            }

            XyActionResponse(
                minCoord = XyPoint(prjX1 - GRID_STEP, prjY1 - GRID_STEP),
                maxCoord = XyPoint(prjX2 + GRID_STEP, prjY2 + GRID_STEP * 2),
            )
        }
    }

    override fun getOneElement(xyActionRequest: XyActionRequest) = XyActionResponse()

    override fun clickElement(xyActionRequest: XyActionRequest): XyActionResponse {
        val elementId = xyActionRequest.elementId!!
        val objectId = xyActionRequest.objectId!!

        val commandID = chmElementCommand[elementId]
        if (commandID != 0) {
            var deviceID = 0
            val deviceIndex = chmElementPort[elementId]!! / CoreTelematicFunction.MAX_PORT_PER_DEVICE
            val rs = conn.executeQuery(
                """
                    SELECT id 
                    FROM MMS_device 
                    WHERE object_id = $objectId 
                    AND device_index = $deviceIndex
                """
            )
            if (rs.next()) {
                deviceID = rs.getInt(1)
            }
            rs.close()

            if (deviceID != 0) {
                conn.executeUpdate(
                    """
                        INSERT INTO MMS_device_command_history ( id , 
                            user_id , device_id , object_id , 
                            command_id , edit_time , 
                            for_send , send_time ) VALUES ( 
                            ${conn.getNextIntId("MMS_device_command_history", "id")} , 
                            ${userConfig.userId} , $deviceID , $objectId , 
                            $commandID , ${getCurrentTimeInt()} , 
                            1 , 0 ) 
                    """
                )
            }
        }

        return XyActionResponse()
    }

    override fun addElement(xyActionRequest: XyActionRequest, userID: Int) = XyActionResponse()
    override fun editElementPoint(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun moveElements(xyActionRequest: XyActionRequest): XyActionResponse {
        val sensorId = xyActionRequest.elementId
        val x = xyActionRequest.dx
        val y = xyActionRequest.dy

        conn.executeUpdate(
            """
                UPDATE MMS_sensor SET 
                    scheme_x = $x , 
                    scheme_y = $y   
                    WHERE id = $sensorId
            """
        )

        return XyActionResponse()
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadDynamicElements(
        scale: Int,
        objectParamData: XyStartObjectParsedData,
        alElement: MutableList<XyElement>,
        hmParams: MutableMap<String, String>,
    ) {
        //--- получить данные по правам доступа
        val isRemoteControlPermission = userConfig.userPermission["mms_object"]?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

        alElement.addAll(getElementList(scale, objectParamData, isRemoteControlPermission, hmParams))

        hmParams[STATE_ELEMENT_MOVE_ENABLED] = (userConfig.userPermission["mms_object"]?.contains(cObject.PERM_SCHEME_SENSOR_MOVE) ?: false).toString()
    }

    private fun getElementList(
        scale: Int,
        objectParamData: XyStartObjectParsedData,
        isRemoteControlPermission: Boolean,
        hmParams: MutableMap<String, String>,
    ): List<XyElement> {
        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectParamData.objectId)
        val objectState = ObjectState.getState(conn, objectConfig)

        val alResult = mutableListOf<XyElement>()

        //--- или глюк или мои кривые руки,
        //--- но выражение chmObjectSchemeUrl[objectConfig.objectId]?.let выполняется и у себя, и во внешнем блоке
        //--- посему заменил на обычный if ( ... != null)
        val schemeFileUrl = chmObjectSchemeUrl[objectConfig.objectId]
        if (schemeFileUrl != null) {
            val (schemeWidth, schemeHeight) = chmObjectSchemeSize[objectConfig.objectId]!!
            val alElementMoveId = mutableListOf<String>()
            val alElementMoveDescr = mutableListOf<String>()
            val alElementMoveX = mutableListOf<String>()
            val alElementMoveY = mutableListOf<String>()

            val imageElement = XyElement(BITMAP, -getRandomInt(), 0)
            //imageElement.init(timeZone)
            imageElement.isReadOnly = true
            imageElement.alPoint = listOf(XyPoint(0, 0))
            imageElement.imageWidth = schemeWidth
            imageElement.imageHeight = schemeHeight
            imageElement.imageName = schemeFileUrl
            alResult.add(imageElement)

            objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.let { hmSCLL ->
                for (portNum in hmSCLL.keys) {
                    val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
                    addLL(
                        objectId = objectParamData.objectId,
                        objectState = objectState,
                        sc = sca,
                        scale = scale,
                        x = if (sca.schemeX != 0) {
                            sca.schemeX + OBJECT_SCHEME_ELEMENT_SIZE_BASE * 2
                        } else {
                            schemeWidth / 2
                        },
                        y = if (sca.schemeY != 0) {
                            sca.schemeY + OBJECT_SCHEME_ELEMENT_SIZE_BASE * 4
                        } else {
                            schemeHeight / 2
                        },
                        sizeBase = OBJECT_SCHEME_ELEMENT_SIZE_BASE,
                        //isElementMoveable = true,
                        alResult = alResult,
                    )
                    alElementMoveId += sca.id.toString()
                    alElementMoveDescr += sca.descr + " [" + SensorConfig.hmSensorDescr[sca.sensorType] + "]"
                    alElementMoveX += sca.schemeX.toString()
                    alElementMoveY += sca.schemeY.toString()
                }
            }

            objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]?.let { hmSCW ->
                for (portNum in hmSCW.keys) {
                    val scw = hmSCW[portNum] as SensorConfigWork
                    addW(
                        objectId = objectParamData.objectId,
                        objectState = objectState,
                        sc = scw,
                        isRemoteControlPermission = isRemoteControlPermission,
                        hmSCS = objectConfig.hmSensorConfig[SensorConfig.SENSOR_SIGNAL],
                        x = if (scw.schemeX != 0) {
                            scw.schemeX
                        } else {
                            schemeWidth / 2
                        },
                        y = if (scw.schemeY != 0) {
                            scw.schemeY
                        } else {
                            schemeHeight / 2
                        },
                        sizeBase = OBJECT_SCHEME_ELEMENT_SIZE_BASE,
                        //isElementMoveable = true,
                        alResult = alResult,
                    )
                    alElementMoveId += scw.id.toString()
                    alElementMoveDescr += scw.descr + " [" + SensorConfig.hmSensorDescr[scw.sensorType] + "]"
                    alElementMoveX += scw.schemeX.toString()
                    alElementMoveY += scw.schemeY.toString()
                }
            }

//            objectConfig.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]?.let { hmSCS ->
//                for (portNum in hmSCS.keys) {
//                    val scs = hmSCS[portNum] as SensorConfigSignal
//                    addS(
//                        objectId = objectParamData.objectId,
//                        objectState = objectState,
//                        sc = scs,
//                        scale = scale,
//                        x = if (scs.schemeX != 0) scs.schemeX else schemeWidth / 2,
//                        y = if (scs.schemeY != 0) scs.schemeY else schemeHeight / 2,
//                        alResult = alResult,
//                    )
//                }
//            }

            //--- выводим время последних данных
            val label = XyElement(TEXT, -getRandomInt(), objectConfig.objectId).apply {
                //typeName = TYPE_STATE_W_TEXT;
                isReadOnly = true
                //--- именно сначала деление, потом умножение, т.к. значение schemeHeight близко к пределу переполнения Int-числа (2^30)
                alPoint = listOf(XyPoint(schemeWidth / 4, schemeHeight / 11 * 10))
                anchorX = XyElement.Anchor.LT
                anchorY = XyElement.Anchor.LT
                alignX = XyElement.Align.LT
                alignY = XyElement.Align.LT
                fillColor = 0xFF_F0_F0_F0.toInt()
                text = objectState.lastDataTime?.let { lastDataTime ->
                    "По состоянию на ${DateTime_YMDHMS(zoneId, lastDataTime)}"
                } ?: "(нет данных)"
                textColor = 0xFF_00_00_00.toInt()
                fontSize = iCoreAppContainer.BASE_FONT_SIZE
                isFontBold = false
            }
            alResult.add(label)
            hmParams[STATE_ELEMENT_MOVE_ID] = alElementMoveId.joinToString("\n")
            hmParams[STATE_ELEMENT_MOVE_DESCR] = alElementMoveDescr.joinToString("\n")
            hmParams[STATE_ELEMENT_MOVE_X] = alElementMoveX.joinToString("\n")
            hmParams[STATE_ELEMENT_MOVE_Y] = alElementMoveY.joinToString("\n")
        } else {
            //--- составляем иерархию ёмкостей и оборудования

            val tmSumGroup = sortedMapOf<String, SortedMap<String, MutableList<SensorConfig>>>()

            objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.let { hmSCLL ->
                for (portNum in hmSCLL.keys) {
                    val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
                    val tmGroup = tmSumGroup.getOrPut("") { TreeMap() }
                    val alLL = tmGroup.getOrPut(sca.group) { mutableListOf() }
                    alLL.add(sca)
                }
            }

            objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]?.let { hmSCW ->
                for (portNum in hmSCW.keys) {
                    val scw = hmSCW[portNum] as SensorConfigWork
                    val tmGroup = tmSumGroup.getOrPut("") { TreeMap() }
                    val alW = tmGroup.getOrPut(scw.group) { mutableListOf() }
                    alW.add(scw)
                }
            }

            val hmSCS = objectConfig.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]
            if (hmSCS != null) {
                for (portNum in hmSCS.keys) {
                    val scs = hmSCS[portNum] as SensorConfigSignal
                    val tmGroup = tmSumGroup.getOrPut("") { TreeMap() }
                    val alS = tmGroup.getOrPut(scs.group) { mutableListOf() }
                    alS.add(scs)
                }
            }

            var x = GRID_STEP * 8
            for ((sumGroup, tmGroup) in tmSumGroup) {
                val sumGroupX = x
                for ((group, alSC) in tmGroup) {
                    val groupX = x
                    var xLL = x + GRID_STEP * 6     // центр дна ёмкости-цилиндра
                    var xW = x + GRID_STEP * 2
                    var xS = x + GRID_STEP
                    for (sc in alSC) {
                        when (sc) {
                            is SensorConfigLiquidLevel -> {
                                addLL(
                                    objectId = objectParamData.objectId,
                                    objectState = objectState,
                                    sc = sc,
                                    scale = scale,
                                    x = xLL,
                                    y = GRID_STEP * 3,
                                    sizeBase = GRID_STEP,
                                    //isElementMoveable = false,
                                    alResult = alResult,
                                )
                                xLL += GRID_STEP * 8
                            }

                            is SensorConfigWork -> {
                                addW(
                                    objectId = objectParamData.objectId,
                                    objectState = objectState,
                                    sc = sc,
                                    isRemoteControlPermission = isRemoteControlPermission,
                                    hmSCS = hmSCS,
                                    x = xW,
                                    y = GRID_STEP * 8,
                                    sizeBase = GRID_STEP,
                                    //isElementMoveable = false,
                                    alResult = alResult,
                                )
                                xW += GRID_STEP * 8
                            }

                            is SensorConfigSignal -> {
                                addS(
                                    objectId = objectParamData.objectId,
                                    objectState = objectState,
                                    sc = sc,
                                    scale = scale,
                                    x = xS,
                                    y = GRID_STEP * 16,
                                    alResult = alResult,
                                )
                                xS += GRID_STEP * 8
                            }
                        }
                    }
                    x = max(max(xLL, xW), xS)

                    addGroupLabel25D(
                        objectId = objectParamData.objectId,
                        x = groupX - GRID_STEP,
                        y = GRID_STEP * 1,
                        limitWidth = x - groupX - GRID_STEP * 2,
                        limitHeight = GRID_STEP * 21,
                        text = group,
                        alResult = alResult
                    )
                }
                addSumGroupLabel25D(
                    objectId = objectParamData.objectId,
                    x = sumGroupX - GRID_STEP * 2,
                    y = 0,
                    limitWidth = x - sumGroupX,
                    limitHeight = GRID_STEP * 25,
                    text = sumGroup,
                    alResult = alResult
                )
                //--- дополнительный промежуток между суммовыми/большими группами
                x += GRID_STEP
            }
            //--- выводим время последних данных
            val label = XyElement(TEXT, -getRandomInt(), objectConfig.objectId).apply {
                //typeName = TYPE_STATE_W_TEXT;
                isReadOnly = true
                alPoint = listOf(XyPoint(GRID_STEP * 6, GRID_STEP * 28))
                anchorX = XyElement.Anchor.LT
                anchorY = XyElement.Anchor.LT
                alignX = XyElement.Align.LT
                alignY = XyElement.Align.LT
                fillColor = 0xFF_F0_F0_F0.toInt()
                text = objectState.lastDataTime?.let { lastDataTime ->
                    "По состоянию на ${DateTime_YMDHMS(zoneId, lastDataTime)}"
                } ?: "(нет данных)"
                textColor = 0xFF_00_00_00.toInt()
                fontSize = iCoreAppContainer.BASE_FONT_SIZE
                isFontBold = false
            }
            alResult.add(label)
        }

        return alResult
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun addLL(
        objectId: Int,
        objectState: ObjectState,
        sc: SensorConfigLiquidLevel,
        scale: Int,
        x: Int,
        y: Int,
        sizeBase: Int,
//        isElementMoveable: Boolean,
        alResult: MutableList<XyElement>,
    ) {
        val liquidError = objectState.tmLiquidError[sc.descr]
        val liquidLevel = objectState.tmLiquidLevel[sc.descr]
        val curLevel = if (liquidError == null && liquidLevel != null) {
            liquidLevel
        } else {
            0.0
        }
        addLL25D(
            objectId = objectId,
            sc = sc,
            scale = scale,
            x = x,
            y = y,
            sizeBase = sizeBase,
            curLevel = curLevel,
            tankName = sc.descr + (if (liquidError == null) "" else '\n') + (liquidError ?: ""),
//            isElementMoveable = isElementMoveable,
            alResult = alResult
        )
    }

    private fun addW(
        objectId: Int,
        objectState: ObjectState,
        sc: SensorConfigWork,
        isRemoteControlPermission: Boolean,
        hmSCS: MutableMap<Int, SensorConfig>?,
        x: Int,
        y: Int,
        sizeBase: Int,
//        isElementMoveable: Boolean,
        alResult: MutableList<XyElement>,
    ) {
        val workState = objectState.tmWorkState[sc.descr]
        var commandId = 0
        var toolTip = sc.descr
        //--- выясняем возможность управления объектом
        if (isRemoteControlPermission) {
            //--- оборудование сейчас включено и есть команда выключения и сигналы разрешают выключение
            if (workState != null && workState && sc.cmdOffId != 0 && getSignalEnabled(objectState, hmSCS, sc.signalOff)) {
                commandId = sc.cmdOffId
                toolTip = "Отключить $toolTip"
            } else if (workState != null && !workState && sc.cmdOnId != 0 && getSignalEnabled(objectState, hmSCS, sc.signalOn)) {
                commandId = sc.cmdOnId
                toolTip = "Включить $toolTip"
            }
        }
        val alElementId = addW25D(
            objectId = objectId,
            x = x,
            y = y,
            sizeBase = sizeBase,
            state = if (workState == null) -1 else if (workState) 1 else 0,
            isControlEnabled = commandId != 0,
            text = sc.descr,
            toolTip = toolTip,
//            isElementMoveable = isElementMoveable,
            alResult = alResult
        )
        for (elementId in alElementId) {
            //--- пропишем/обнулим команду на этот элемент
            chmElementCommand[elementId] = commandId
            //--- пропишем/обнулим номер порта датчика на этот элемент
            //--- (для последующего определения device_id для отправки команды)
            chmElementPort[elementId] = sc.portNum

//            if (isElementMoveable) {
//                chmElementSensor[elementId] = sc.id
//            }
        }
    }

    private fun addS(
        objectId: Int,
        objectState: ObjectState,
        sc: SensorConfigSignal,
        scale: Int,
        x: Int,
        y: Int,
        alResult: MutableList<XyElement>,
    ) {
        val signalState = objectState.tmSignalState[sc.descr]
        addS25D(
            objectId = objectId,
            scale = scale,
            x = x,
            y = y,
            state = if (signalState == null) -1 else if (signalState) 1 else 0,
            text = sc.descr,
            alResult = alResult
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- метка/текст группы
    private fun addSumGroupLabel25D(objectId: Int, x: Int, y: Int, limitWidth: Int, limitHeight: Int, text: String, alResult: MutableList<XyElement>) {

        drawCube(
            objectId = objectId,
            elementType = TYPE_STATE_SUM_GROUP_BOX_25D,
            isReadOnly = true,
            //isElementMoveable = false,
            toolTip = "",
            x = x,
            y = y,
            w = limitWidth,
            h = limitHeight,
            dw = GRID_STEP * 4,
            dh = GRID_STEP * 2,
            lineWidth = 0,
            drawColor = 0,
            topFillColor = 0xFF_F0_F0_F0.toInt(),
            frontFillColor = 0xFF_E0_E0_E0.toInt(),
            sideFillColor = 0xFF_D0_D0_D0.toInt(),
            alResult = alResult
        )

        val label = XyElement(TYPE_STATE_SUM_GROUP_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y + limitHeight))

            this.text = text
            toolTipText = text
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

            //--- рисуем рамку текста и одновременно ограничиваем вывод текста по ширине
            this.limitWidth = limitWidth
            this.limitHeight = GRID_STEP * 2   // компромиссное решение: текст иногда выходит за нижние границы блока, зато он не обрезается по высоте на небольших дисплеях

            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.CC
            alignY = XyElement.Align.LT
        }
        alResult.add(label)
    }

    //--- метка/текст группы
    private fun addGroupLabel25D(objectId: Int, x: Int, y: Int, limitWidth: Int, limitHeight: Int, text: String, alResult: MutableList<XyElement>) {

        drawCube(
            objectId = objectId,
            elementType = TYPE_STATE_GROUP_BOX_25D,
            isReadOnly = true,
            //isElementMoveable = false,
            toolTip = "",
            x = x,
            y = y,
            w = limitWidth,
            h = limitHeight,
            dw = GRID_STEP * 7 / 2,
            dh = GRID_STEP * 2,
            lineWidth = 0,
            drawColor = 0,
            topFillColor = 0xFF_E0_E0_E0.toInt(),
            frontFillColor = 0xFF_D0_D0_D0.toInt(),
            sideFillColor = 0xFF_C0_C0_C0.toInt(),
            alResult = alResult
        )

        val label = XyElement(TYPE_STATE_GROUP_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y + limitHeight))

            this.text = text
            toolTipText = text
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

            //--- рисуем рамку текста и одновременно ограничиваем вывод текста по ширине
            this.limitWidth = limitWidth
            this.limitHeight = GRID_STEP * 2

            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.CC
            alignY = XyElement.Align.LT
        }
        alResult.add(label)
    }

    //--- объём в литрах ---
    private fun addLL25D(
        objectId: Int,
        sc: SensorConfigLiquidLevel,
        scale: Int,
        x: Int,
        y: Int,
        sizeBase: Int,
        curLevel: Double,
        tankName: String,
//        isElementMoveable: Boolean,
        alResult: MutableList<XyElement>
    ) {

        val percent = curLevel / if (sc.maxView == 0.0) {
            curLevel
        } else {
            sc.maxView
        }
        val totalVolumeText = getSplittedDouble(sc.maxView, 0, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
        val currentVolumeText = " ${getSplittedDouble(curLevel, 0, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)}"

        //--- используем логарифмическую шкалу показа относительных ёмкостей,
        //--- т.к. нельзя использовать линейную шкалу - объёмы могут быть от 100 до 50 000 л
        val scaledMaxLevel = log10(sc.maxView)
        //--- высота ёмкости (* sizeBase * 2 - многовато - ёмкости похожи на мензурки)
        val tankHeight = (scaledMaxLevel * sizeBase).roundToInt()
        //--- собственно уровень жидкости/топлива
        val levelHeight = (tankHeight * percent).roundToInt()
        //--- цвет жидкости и поверхности жидкости зависит от общего объёма
        val tankFillColor = (if (sc.maxView < 1000) 0xFF_00_FF_FF else if (sc.maxView < 10000) 0xFF_00_C0_FF else 0xFF_00_80_FF).toInt()
        val tankFillColor2 = (if (sc.maxView < 1000) 0xFF_00_E0_E0 else if (sc.maxView < 10000) 0xFF_00_A0_E0 else 0xFF_00_60_E0).toInt()
        //--- цвет рамки ёмкости зависит от заполненности
        val tankDrawColor = (if (percent < CRITICAL_LL_PERCENT) 0xFF_FF_00_00 else 0xFF_00_00_00).toInt()

        //--- дно ёмкости
        XyElement(TYPE_STATE_LL_BOTTOM_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y))
            toolTipText = tankName

            markerType = XyElement.MarkerType.CIRCLE
            markerSize = sizeBase * 4 / scale
            markerSize2 = sizeBase * 2 / scale
            drawColor = tankDrawColor
            fillColor = tankFillColor
            lineWidth = 2

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- объём жидкости
        XyElement(TYPE_STATE_LL_VOLUME_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = true
            alPoint = listOf(
                XyPoint(x - sizeBase * 2, y),
                XyPoint(x - sizeBase * 2, y - levelHeight),
                XyPoint(x + sizeBase * 2, y - levelHeight),
                XyPoint(x + sizeBase * 2, y)
            )

            //drawColor = 0xFF_00_00_00 ); - без рамки
            fillColor = tankFillColor
            lineWidth = 0

            //isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- уровень жидкости
        XyElement(TYPE_STATE_LL_LEVEL_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y - levelHeight))
            toolTipText = tankName

            markerType = XyElement.MarkerType.CIRCLE
            markerSize = sizeBase * 4 / scale
            markerSize2 = sizeBase * 2 / scale
            fillColor = tankFillColor2
            lineWidth = 0

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- стенки ёмкости
        XyElement(TYPE_STATE_LL_TANK_WALL_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = false
            alPoint = listOf(XyPoint(x - sizeBase * 2, y), XyPoint(x - sizeBase * 2, y - tankHeight))

            drawColor = tankDrawColor
            lineWidth = 2

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        XyElement(TYPE_STATE_LL_TANK_WALL_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true

            isClosed = false
            alPoint = listOf(XyPoint(x + sizeBase * 2, y), XyPoint(x + sizeBase * 2, y - tankHeight))

            drawColor = tankDrawColor
            lineWidth = 2

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- верх ёмкости
        XyElement(TYPE_STATE_LL_TANK_TOP_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y - tankHeight))
            toolTipText = tankName

            markerType = XyElement.MarkerType.CIRCLE
            markerSize = sizeBase * 4 / scale
            markerSize2 = sizeBase * 2 / scale
            drawColor = tankDrawColor
            lineWidth = 2

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- общая ёмкость
        XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x - sizeBase * 2, y - tankHeight - sizeBase * 4))
            //--- НЕ ограничиваем вывод текста по ширине
            //topLevelLabel.limitWidth = sizeBase * 4
            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.LT
            alignY = XyElement.Align.LT

            text = totalVolumeText
            toolTipText = totalVolumeText
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- текущий уровень
        XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x + sizeBase * 2, y - levelHeight))
            //--- НЕ ограничиваем вывод текста по ширине
            //currentLevelLabel.limitWidth = sizeBase * 4
            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.LT
            alignY = XyElement.Align.LT

            text = currentVolumeText
            toolTipText = currentVolumeText
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }

        //--- наименование ёмкости
        XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x - sizeBase * 2, y + sizeBase * 2))
            //--- ограничиваем вывод текста по ширине
            limitWidth = sizeBase * 4
            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.LT
            alignY = XyElement.Align.LT

            text = tankName
            toolTipText = tankName
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
//            if (isElementMoveable) {
//                chmElementSensor[xyElement.elementId] = sc.id
//            }
        }
    }

    private fun addW25D(
        objectId: Int,
        x: Int,
        y: Int,
        sizeBase: Int,
        state: Int,
        isControlEnabled: Boolean,
        text: String,
        toolTip: String,
//        isElementMoveable: Boolean,
        alResult: MutableList<XyElement>
    ): List<Int> {

        val alElementId = drawCube(
            objectId = objectId,
            elementType = TYPE_STATE_W_FIGURE_25D,
            isReadOnly = !isControlEnabled,
            //isElementMoveable = isElementMoveable,
            toolTip = toolTip,
            x = x,
            y = y,
            w = sizeBase * 4,
            h = sizeBase * 2,
            dw = sizeBase,
            dh = sizeBase * 4,
            lineWidth = if (state < 0) 2 else 0,
            drawColor = if (state < 0) 0xFF_FF_00_00.toInt() else 0,
            topFillColor = if (state > 0) 0xFF_00_FF_00.toInt() else 0xFF_D0_D0_D0.toInt(),
            frontFillColor = if (state > 0) 0xFF_00_E0_00.toInt() else 0xFF_C0_C0_C0.toInt(),
            sideFillColor = if (state > 0) 0xFF_00_C0_00.toInt() else 0xFF_B0_B0_B0.toInt(),
            alResult = alResult
        )

        XyElement(TYPE_STATE_W_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, y + sizeBase * 7))
            //--- ограничиваем вывод текста по ширине
            limitWidth = sizeBase * 4
            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.LT
            alignY = XyElement.Align.LT

            this.text = text
            toolTipText = text
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true

//            isMoveable = isElementMoveable
        }.let { xyElement ->
            alResult.add(xyElement)
            alElementId.add(xyElement.elementId)
        }

        return alElementId
    }

    private fun addS25D(objectId: Int, scale: Int, x: Int, y: Int, state: Int, text: String, alResult: MutableList<XyElement>) {
        val marker = XyElement(TYPE_STATE_S_FIGURE_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x + GRID_STEP * 2, y + GRID_STEP * 2))
            toolTipText = text

            markerType = XyElement.MarkerType.CIRCLE
            markerSize = GRID_STEP * 2 / scale
            drawColor = if (state < 0) 0xFF_FF_00_00.toInt() else 0xFF_00_00_00.toInt()
            fillColor = if (state > 0) 0xFF_00_FF_00.toInt() else 0xFF_A0_A0_A0.toInt()
            lineWidth = 2
        }
        alResult.add(marker)

        //--- метка/текст оборудования
        val textY = y + GRID_STEP * 4

        val label = XyElement(TYPE_STATE_W_TEXT_25D, -getRandomInt(), objectId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x, textY))
            //--- ограничиваем вывод текста по ширине
            limitWidth = GRID_STEP * 4
            anchorX = XyElement.Anchor.LT
            anchorY = XyElement.Anchor.LT
            alignX = XyElement.Align.LT
            alignY = XyElement.Align.LT

            this.text = text
            toolTipText = text
            textColor = 0xFF_00_00_00.toInt()
            fontSize = iCoreAppContainer.BASE_FONT_SIZE
            isFontBold = true
        }
        alResult.add(label)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun drawCube(
        objectId: Int,
        elementType: String,
        isReadOnly: Boolean,
//        isElementMoveable: Boolean,
        toolTip: String,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        dw: Int,
        dh: Int,
        lineWidth: Int,
        drawColor: Int,
        topFillColor: Int,
        frontFillColor: Int,
        sideFillColor: Int,
        alResult: MutableList<XyElement>
    ): MutableList<Int> {
        //--- top panel
        val top = XyElement(elementType, -getRandomInt(), objectId).apply {
            this.isReadOnly = isReadOnly
            toolTipText = toolTip

            isClosed = true
            alPoint = listOf(
                XyPoint(x, y + h),
                XyPoint(x + dw, y),
                XyPoint(x + w + dw, y),
                XyPoint(x + w, y + h)
            )

            this.lineWidth = lineWidth
            this.drawColor = drawColor
            fillColor = topFillColor
//
//            isMoveable = isElementMoveable
        }
        alResult.add(top)

        //--- front panel
        val front = XyElement(elementType, -getRandomInt(), objectId).apply {
            this.isReadOnly = isReadOnly
            toolTipText = toolTip

            isClosed = true
            alPoint = listOf(
                XyPoint(x, y + h),
                XyPoint(x + w, y + h),
                XyPoint(x + w, y + h + dh),
                XyPoint(x, y + h + dh)
            )

            this.lineWidth = lineWidth
            this.drawColor = drawColor
            fillColor = frontFillColor
//
//            isMoveable = isElementMoveable
        }
        alResult.add(front)

        //--- side panel
        val side = XyElement(elementType, -getRandomInt(), objectId).apply {
            this.isReadOnly = isReadOnly
            toolTipText = toolTip

            isClosed = true
            alPoint = listOf(
                XyPoint(x + w, y + h),
                XyPoint(x + w + dw, y),
                XyPoint(x + w + dw, y + dh),
                XyPoint(x + w, y + h + dh)
            )

            this.lineWidth = lineWidth
            this.drawColor = drawColor
            fillColor = sideFillColor
//
//            isMoveable = isElementMoveable
        }
        alResult.add(side)

        return mutableListOf(top.elementId, front.elementId, side.elementId)
    }

    private fun getSignalEnabled(objectState: ObjectState, hmSCS: Map<Int, SensorConfig>?, signalConfig: SignalConfig): Boolean {
        //--- если разрешителей не прописано - то можно по умолчанию
        if (signalConfig.alPort.isEmpty()) {
            return true
        } else if (hmSCS != null) {
            for (portNum in signalConfig.alPort) {
                val scs = hmSCS[portNum] as SensorConfigSignal
                val signalState = objectState.tmSignalState[scs.descr]
                //--- если состояние хотя бы одного сигнала не определено - выходим с отрицательным результатом
                if (signalState == null) {
                    return false
                }
                //--- если не было досрочных выходов
                else {
                    if (signalConfig.and) {
                        //--- достаточно одного облома
                        if (portNum > 0 && !signalState || portNum < 0 && signalState) {
                            return false
                        }
                    } else {
                        //--- достаточно одного успеха
                        if (portNum > 0 && signalState || portNum < 0 && !signalState) {
                            return true
                        }
                    }
                }
            }
            //--- если прописаны разрешающие/запрещающие порты/сигналы
            return signalConfig.and
        } else {
            return false
        }
    }

}
