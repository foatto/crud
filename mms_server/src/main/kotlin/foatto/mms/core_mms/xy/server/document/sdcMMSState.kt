package foatto.mms.core_mms.xy.server.document

import foatto.app.CoreSpringController
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
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.core_server.app.xy.server.document.sdcXyState
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.cObject
import foatto.mms.core_mms.calc.ObjectState
import foatto.mms.core_mms.ds.MMSHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.core_mms.sensor.config.SensorConfigSignal
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.mms.core_mms.sensor.config.SignalConfig
import foatto.sql.CoreAdvancedStatement
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class sdcMMSState : sdcXyState() {

    companion object {
        //--- располагаем по возрастанию уровня расположения
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

        private val CRITICAL_LL_PERCENT = 0.1

        //--- опытным путём выяснено, что при слишком малой величине шага начинаются побочные эффекты от целочисленного деления
        //--- и края схемы могут вплотную примкнуть к краям окна/экрана.
        //--- Шаг в 1 млн. "точек" позволяет уместить до 2000 элементов схемы ( 2 млрд MAX_INTEGER / 1 млн = 2000) с достаточной точностью.
        private val GRID_STEP = 1024 * 1024

        //--- привязка элемента и команды на нём
        private val chmElementCommand = ConcurrentHashMap<Int, Int>()

        //--- привязка элемента и номера порта на нём (для последующего определения deviceID, которое избыточно определять при отрисовке)
        private val chmElementPort = ConcurrentHashMap<Int, Int>()
    }

    override fun init(aAppController: CoreSpringController, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aUserConfig: UserConfig, aDocumentConfig: XyDocumentConfig) {

        //--- схемы ---

        hmInAliasElementType.put(
            "mms_object", arrayOf(
                TYPE_STATE_SUM_GROUP_BOX_25D, TYPE_STATE_SUM_GROUP_TEXT_25D, TYPE_STATE_GROUP_BOX_25D, TYPE_STATE_GROUP_TEXT_25D,
                TYPE_STATE_LL_BOTTOM_25D, TYPE_STATE_LL_VOLUME_25D, TYPE_STATE_LL_LEVEL_25D, TYPE_STATE_LL_TANK_WALL_25D, TYPE_STATE_LL_TANK_TOP_25D, TYPE_STATE_LL_TEXT_25D,
                TYPE_STATE_W_FIGURE_25D, TYPE_STATE_W_TEXT_25D,
                TYPE_STATE_S_FIGURE_25D, TYPE_STATE_S_TEXT_25D
            )
        )

        hmOutElementTypeAlias.put(TYPE_STATE_SUM_GROUP_BOX_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_SUM_GROUP_TEXT_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_GROUP_BOX_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_GROUP_TEXT_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_BOTTOM_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_VOLUME_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_LEVEL_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_TANK_WALL_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_TANK_TOP_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_LL_TEXT_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_W_FIGURE_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_W_TEXT_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_S_FIGURE_25D, "mms_object")
        hmOutElementTypeAlias.put(TYPE_STATE_S_TEXT_25D, "mms_object")

        super.init(aAppController, aStm, aChmSession, aUserConfig, aDocumentConfig)
    }

    override fun getCoords(startParamID: String): XyActionResponse {
        val sd = chmSession[AppParameter.XY_START_DATA + startParamID] as XyStartData

        var prjX1 = Integer.MAX_VALUE
        var prjY1 = Integer.MAX_VALUE
        var prjX2 = Integer.MIN_VALUE
        var prjY2 = Integer.MIN_VALUE

        //--- разбор входных параметров
        val alObjectParamData = parseObjectParam(true /*isStartObjectsDefined*/, sd, mutableSetOf())

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission["mms_object"]
        val isRemoteControlPermission = hsPermission != null && hsPermission.contains(cObject.PERM_REMOTE_CONTROL)

        //--- отдельная обработка динамических объектов
        for(objectParamData in alObjectParamData) {
            val alResult = getElementList(1, objectParamData, isRemoteControlPermission)
            for(e in alResult) {
                for(p in e.alPoint) {
                    prjX1 = Math.min(prjX1, p.x)
                    prjY1 = Math.min(prjY1, p.y)
                    prjX2 = Math.max(prjX2, p.x)
                    prjY2 = Math.max(prjY2, p.y)
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
        val elementID = xyActionRequest.elementID!!
        val objectID = xyActionRequest.objectID!!

        val commandID = chmElementCommand[elementID]
        if(commandID != 0) {
            var deviceID = 0
            val deviceIndex = chmElementPort[elementID]!! / MMSHandler.MAX_PORT_PER_DEVICE
            val rs = stm.executeQuery(" SELECT id FROM MMS_device WHERE object_id = $objectID AND device_index = $deviceIndex")
            if(rs.next()) deviceID = rs.getInt(1)
            rs.close()

            if(deviceID != 0) stm.executeUpdate(
                " INSERT INTO MMS_device_command_history ( id , user_id , device_id , object_id , command_id , edit_time , for_send , send_time ) VALUES ( " +
                    "${stm.getNextID("MMS_device_command_history", "id")} , ${userConfig.userID} , $deviceID , $objectID , $commandID , " +
                    "${getCurrentTimeInt()} , 1 , 0 ) "
            )
        }

        return XyActionResponse()
    }

    override fun addElement(xyActionRequest: XyActionRequest, userID: Int) = XyActionResponse()
    override fun editElementPoint(xyActionRequest: XyActionRequest) = XyActionResponse()
    override fun moveElements(xyActionRequest: XyActionRequest) = XyActionResponse()

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadDynamicElements(scale: Int, objectParamData: XyStartObjectParsedData, alElement: MutableList<XyElement>) {
        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission["mms_object"]
        val isRemoteControlPermission = hsPermission?.contains(cObject.PERM_REMOTE_CONTROL) ?: false

        alElement.addAll(getElementList(scale, objectParamData, isRemoteControlPermission))
    }

    private fun getElementList(scale: Int, objectParamData: XyStartObjectParsedData, isRemoteControlPermission: Boolean): List<XyElement> {
        val objectConfig = ObjectConfig.getObjectConfig(stm, userConfig, objectParamData.objectID)
        val objectState = ObjectState.getState(stm, objectConfig)

        //--- составляем иерархию ёмкостей и оборудования

        val tmSumGroup = TreeMap<String, TreeMap<String, MutableList<SensorConfig>>>()

        val hmSCLL = objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]
        if(hmSCLL != null)
            for(portNum in hmSCLL.keys) {
                val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
                val tmGroup = tmSumGroup.getOrPut(sca.sumGroup) { TreeMap() }
                val alLL = tmGroup.getOrPut(sca.group) { mutableListOf() }
                alLL.add(sca)
            }

        val hmSCW = objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]
        if(hmSCW != null)
            for(portNum in hmSCW.keys) {
                val scw = hmSCW[portNum] as SensorConfigWork
                val tmGroup = tmSumGroup.getOrPut(scw.sumGroup) { TreeMap() }
                val alW = tmGroup.getOrPut(scw.group) { mutableListOf() }
                alW.add(scw)
            }

        val hmSCS = objectConfig.hmSensorConfig[SensorConfig.SENSOR_SIGNAL]
        if(hmSCS != null)
            for(portNum in hmSCS.keys) {
                val scs = hmSCS[portNum] as SensorConfigSignal
                val tmGroup = tmSumGroup.getOrPut(scs.sumGroup) { TreeMap() }
                val alS = tmGroup.getOrPut(scs.group) { mutableListOf() }
                alS.add(scs)
            }

        val alResult = mutableListOf<XyElement>()

        var x = GRID_STEP * 8
        for((sumGroup, tmGroup) in tmSumGroup) {
            val sumGroupX = x
            for((group, alSC) in tmGroup) {
                val groupX = x
                var xLL = x + GRID_STEP * 6     // центр дна ёмкости-цилиндра
                var xW = x + GRID_STEP * 2
                var xS = x + GRID_STEP
                for(sc in alSC) {
                    if (sc is SensorConfigLiquidLevel) {
                        val liquidError = objectState.tmLiquidError[sc.descr]
                        val liquidLevel = objectState.tmLiquidLevel[sc.descr]
                        val curLevel = if (liquidError == null && liquidLevel != null) liquidLevel else 0.0
                        addLL25D(
                            objectParamData.objectID, sc, scale, xLL, GRID_STEP * 3, curLevel,
                            StringBuilder(sc.descr).append(if (liquidError == null) "" else '\n').append(liquidError ?: "").toString(), alResult
                        )
                        xLL += GRID_STEP * 8
                    } else if (sc is SensorConfigWork) {
                        val workState = objectState.tmWorkState[sc.descr]
                        var commandID = 0
                        var toolTip = sc.descr
                        //--- выясняем возможность управления объектом
                        if (isRemoteControlPermission) {
                            //--- оборудование сейчас включено и есть команда выключения и сигналы разрешают выключение
                            if (workState != null && workState && sc.cmdOffID != 0 && getSignalEnabled(objectState, hmSCS!!, sc.signalOff)) {
                                commandID = sc.cmdOffID
                                toolTip = "Отключить $toolTip"
                            } else if (workState != null && !workState && sc.cmdOnID != 0 && getSignalEnabled(objectState, hmSCS!!, sc.signalOn)) {
                                commandID = sc.cmdOnID
                                toolTip = "Включить $toolTip"
                            }
                        }
                        val arrElementID = addW25D(
                            objectParamData.objectID, xW, GRID_STEP * 8, if(workState == null) -1 else if(workState) 1 else 0, commandID != 0,
                            sc.descr, toolTip, alResult
                        )
                        for (elementID in arrElementID) {
                            //--- пропишем/обнулим команду на этот элемент
                            chmElementCommand.put(elementID, commandID)
                            //--- пропишем/обнулим номер порта датчика на этот элемент
                            //--- (для последующего определения device_id для отправки команды)
                            chmElementPort.put(elementID, sc.portNum)
                        }
                        xW += GRID_STEP * 8
                    } else if (sc is SensorConfigSignal) {
                        val signalState = objectState.tmSignalState[sc.descr]
                        addS25D(objectParamData.objectID, scale, xS, GRID_STEP * 16, if (signalState == null) -1 else if (signalState) 1 else 0, sc.descr, alResult)
                        xS += GRID_STEP * 8
                    }
                }
                x = Math.max(Math.max(xLL, xW), xS)

                addGroupLabel25D(
                    objectID = objectParamData.objectID,
                    x = groupX - GRID_STEP,
                    y = GRID_STEP * 1,
                    limitWidth = x - groupX - GRID_STEP * 2,
                    limitHeight = GRID_STEP * 21,
                    text = group,
                    alResult = alResult
                )
            }
            addSumGroupLabel25D(
                objectID = objectParamData.objectID,
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
        val label = XyElement(TEXT, -getRandomInt(), objectConfig.objectID)
        //label.init( timeZone )
        //label.typeName = TYPE_STATE_W_TEXT;
        label.itReadOnly = true
        label.alPoint.add(XyPoint(GRID_STEP * 6, GRID_STEP * 28))
        label.anchorX = XyElement.Anchor.LT
        label.anchorY = XyElement.Anchor.LT
        label.alignX = XyElement.Align.LT
        label.alignY = XyElement.Align.LT
        label.fillColor = 0xFF_F0_F0_F0.toInt()
        label.text = "По состоянию на ${DateTime_YMDHMS(zoneId, objectState.time)}"
        label.textColor = 0xFF_00_00_00.toInt()
        label.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        label.itFontBold = false

        alResult.add(label)

        return alResult
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- метка/текст группы
    private fun addSumGroupLabel25D(objectID: Int, x: Int, y: Int, limitWidth: Int, limitHeight: Int, text: String, alResult: MutableList<XyElement>) {

        drawCube(
            objectID, TYPE_STATE_SUM_GROUP_BOX_25D, true, "",
            x, y, limitWidth, limitHeight, GRID_STEP * 4, GRID_STEP * 2,
            0, 0, 0xFF_F0_F0_F0.toInt(), 0xFF_E0_E0_E0.toInt(), 0xFF_D0_D0_D0.toInt(), alResult
        )

        val label = XyElement(TYPE_STATE_SUM_GROUP_TEXT_25D, -getRandomInt(), objectID)
        //label.init( timeZone )
        label.itReadOnly = true
        label.alPoint.add(XyPoint(x, y + limitHeight))

        label.text = text
        label.toolTipText = text
        label.textColor = 0xFF_00_00_00.toInt()
        label.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        label.itFontBold = true

        //--- рисуем рамку текста и одновременно ограничиваем вывод текста по ширине
        label.limitWidth = limitWidth
        label.limitHeight = GRID_STEP * 2   // компромиссное решение: текст иногда выходит за нижние границы блока, зато он не обрезается по высоте на небольших дисплеях

        label.anchorX = XyElement.Anchor.LT
        label.anchorY = XyElement.Anchor.LT
        label.alignX = XyElement.Align.CC
        label.alignY = XyElement.Align.LT

        alResult.add(label)
    }

    //--- метка/текст группы
    private fun addGroupLabel25D(objectID: Int, x: Int, y: Int, limitWidth: Int, limitHeight: Int, text: String, alResult: MutableList<XyElement>) {

        drawCube(
            objectID, TYPE_STATE_GROUP_BOX_25D, true, "",
            x, y, limitWidth, limitHeight, GRID_STEP * 7 / 2, GRID_STEP * 2,
            0, 0, 0xFF_E0_E0_E0.toInt(), 0xFF_D0_D0_D0.toInt(), 0xFF_C0_C0_C0.toInt(), alResult
        )

        val label = XyElement(TYPE_STATE_GROUP_TEXT_25D, -getRandomInt(), objectID)
        //label.init( timeZone )
        label.itReadOnly = true
        label.alPoint.add(XyPoint(x, y + limitHeight))

        label.text = text
        label.toolTipText = text
        label.textColor = 0xFF_00_00_00.toInt()
        label.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        label.itFontBold = true

        //--- рисуем рамку текста и одновременно ограничиваем вывод текста по ширине
        label.limitWidth = limitWidth
        label.limitHeight = GRID_STEP * 2

        label.anchorX = XyElement.Anchor.LT
        label.anchorY = XyElement.Anchor.LT
        label.alignX = XyElement.Align.CC
        label.alignY = XyElement.Align.LT

        alResult.add(label)
    }

    //--- объём в литрах ---
    private fun addLL25D(objectID: Int, sc: SensorConfigLiquidLevel, scale: Int, x: Int, y: Int, curLevel: Double, tankName: String, alResult: MutableList<XyElement>) {

        val percent = curLevel / if (sc.maxView == 0.0) curLevel else sc.maxView
        val totalVolumeText = "${getSplittedDouble(sc.maxView, 0)}"
        val currentVolumeText = " ${getSplittedDouble(curLevel, 0)}"

        //--- используем логарифмическую шкалу показа относительных ёмкостей,
        //--- т.к. нельзя использовать линейную шкалу - объёмы могут быть от 100 до 50 000 л
        val scaledMaxLevel = Math.log10(sc.maxView)
        //--- высота ёмкости (* GRID_STEP * 2 - многовато - ёмкости похожи на мензурки)
        val tankHeight = Math.round(scaledMaxLevel * GRID_STEP).toInt()
        //--- собственно уровень жидкости/топлива
        val levelHeight = Math.round(tankHeight * percent).toInt()
        //--- цвет жидкости и поверхности жидкости зависит от общего объёма
        val tankFillColor = (if (sc.maxView < 1000) 0xFF_00_FF_FF else if (sc.maxView < 10000) 0xFF_00_C0_FF else 0xFF_00_80_FF).toInt()
        val tankFillColor2 = (if(sc.maxView < 1000) 0xFF_00_E0_E0 else if(sc.maxView < 10000) 0xFF_00_A0_E0 else 0xFF_00_60_E0).toInt()
        //--- цвет рамки ёмкости зависит от заполненности
        val tankDrawColor = if(percent < CRITICAL_LL_PERCENT) 0xFF_FF_00_00.toInt() else 0xFF_00_00_00.toInt()

        //--- дно ёмкости
        val bottom = XyElement(TYPE_STATE_LL_BOTTOM_25D, -getRandomInt(), objectID)
        //bottom.init( timeZone )
        bottom.itReadOnly = true
        bottom.alPoint.add(XyPoint(x, y))
        bottom.toolTipText = tankName

        bottom.markerType = XyElement.MarkerType.CIRCLE
        bottom.markerSize = GRID_STEP * 4 / scale
        bottom.markerSize2 = GRID_STEP * 2 / scale
        bottom.drawColor = tankDrawColor
        bottom.fillColor = tankFillColor
        bottom.lineWidth = 2

        alResult.add(bottom)

        //--- объём жидкости
        val volume = XyElement(TYPE_STATE_LL_VOLUME_25D, -getRandomInt(), objectID)
        //volume.init( timeZone )
        volume.itReadOnly = true

        volume.itClosed = true
        volume.alPoint.add(XyPoint(x - GRID_STEP * 2, y))
        volume.alPoint.add(XyPoint(x - GRID_STEP * 2, y - levelHeight))
        volume.alPoint.add(XyPoint(x + GRID_STEP * 2, y - levelHeight))
        volume.alPoint.add(XyPoint(x + GRID_STEP * 2, y))

        //nameLabel.drawColor = 0xFF_00_00_00 ); - без рамки
        volume.fillColor = tankFillColor
        volume.lineWidth = 0

        alResult.add(volume)

        //--- уровень жидкости
        val level = XyElement(TYPE_STATE_LL_LEVEL_25D, -getRandomInt(), objectID)
        //level.init( timeZone )
        level.itReadOnly = true
        level.alPoint.add(XyPoint(x, y - levelHeight))
        level.toolTipText = tankName

        level.markerType = XyElement.MarkerType.CIRCLE
        level.markerSize = GRID_STEP * 4 / scale
        level.markerSize2 = GRID_STEP * 2 / scale
        level.fillColor = tankFillColor2
        level.lineWidth = 0

        alResult.add(level)

        //--- стенки ёмкости
        val tankWall1 = XyElement(TYPE_STATE_LL_TANK_WALL_25D, -getRandomInt(), objectID)
        //tankWall1.init( timeZone )
        tankWall1.itReadOnly = true

        tankWall1.itClosed = false
        tankWall1.alPoint.add(XyPoint(x - GRID_STEP * 2, y))
        tankWall1.alPoint.add(XyPoint(x - GRID_STEP * 2, y - tankHeight))

        tankWall1.drawColor = tankDrawColor
        tankWall1.lineWidth = 2

        alResult.add(tankWall1)

        val tankWall2 = XyElement(TYPE_STATE_LL_TANK_WALL_25D, -getRandomInt(), objectID)
        //tankWall2.init( timeZone )
        tankWall2.itReadOnly = true

        tankWall2.itClosed = false
        tankWall2.alPoint.add(XyPoint(x + GRID_STEP * 2, y))
        tankWall2.alPoint.add(XyPoint(x + GRID_STEP * 2, y - tankHeight))

        tankWall2.drawColor = tankDrawColor
        tankWall2.lineWidth = 2

        alResult.add(tankWall2)

        //--- верх ёмкости
        val tankTop = XyElement(TYPE_STATE_LL_TANK_TOP_25D, -getRandomInt(), objectID)
        //tankTop.init( timeZone )
        tankTop.itReadOnly = true
        tankTop.alPoint.add(XyPoint(x, y - tankHeight))
        tankTop.toolTipText = tankName

        tankTop.markerType = XyElement.MarkerType.CIRCLE
        tankTop.markerSize = GRID_STEP * 4 / scale
        tankTop.markerSize2 = GRID_STEP * 2 / scale
        tankTop.drawColor = tankDrawColor
        tankTop.lineWidth = 2

        alResult.add(tankTop)

        //--- общая ёмкость
        val topLevelLabel = XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectID)
        //topLevelLabel.init( timeZone )
        topLevelLabel.itReadOnly = true
        topLevelLabel.alPoint.add(XyPoint(x - GRID_STEP * 2, y - tankHeight - GRID_STEP * 4))
        //--- НЕ ограничиваем вывод текста по ширине
        //topLevelLabel.limitWidth = GRID_STEP * 4
        topLevelLabel.anchorX = XyElement.Anchor.LT
        topLevelLabel.anchorY = XyElement.Anchor.LT
        topLevelLabel.alignX = XyElement.Align.LT
        topLevelLabel.alignY = XyElement.Align.LT

        topLevelLabel.text = totalVolumeText
        topLevelLabel.toolTipText = totalVolumeText
        topLevelLabel.textColor = 0xFF_00_00_00.toInt()
        topLevelLabel.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        topLevelLabel.itFontBold = true

        alResult.add(topLevelLabel)

        //--- текущий уровень
        val currentLevelLabel = XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectID)
        //currentLevelLabel.init( timeZone )
        currentLevelLabel.itReadOnly = true
        currentLevelLabel.alPoint.add(XyPoint(x + GRID_STEP * 2, y - levelHeight))
        //--- НЕ ограничиваем вывод текста по ширине
        //currentLevelLabel.limitWidth = GRID_STEP * 4
        currentLevelLabel.anchorX = XyElement.Anchor.LT
        currentLevelLabel.anchorY = XyElement.Anchor.LT
        currentLevelLabel.alignX = XyElement.Align.LT
        currentLevelLabel.alignY = XyElement.Align.LT

        currentLevelLabel.text = currentVolumeText
        currentLevelLabel.toolTipText = currentVolumeText
        currentLevelLabel.textColor = 0xFF_00_00_00.toInt()
        currentLevelLabel.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        currentLevelLabel.itFontBold = true

        alResult.add(currentLevelLabel)

        //--- наименование ёмкости
        val nameLabel = XyElement(TYPE_STATE_LL_TEXT_25D, -getRandomInt(), objectID)
        //nameLabel.init( timeZone )
        nameLabel.itReadOnly = true
        nameLabel.alPoint.add(XyPoint(x - GRID_STEP * 2, y + GRID_STEP * 2))
        //--- ограничиваем вывод текста по ширине
        nameLabel.limitWidth = GRID_STEP * 4
        nameLabel.anchorX = XyElement.Anchor.LT
        nameLabel.anchorY = XyElement.Anchor.LT
        nameLabel.alignX = XyElement.Align.LT
        nameLabel.alignY = XyElement.Align.LT

        nameLabel.text = tankName
        nameLabel.toolTipText = tankName
        nameLabel.textColor = 0xFF_00_00_00.toInt()
        nameLabel.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        nameLabel.itFontBold = true

        alResult.add(nameLabel)
    }

    private fun addW25D(objectID: Int, x: Int, y: Int, state: Int, isControlEnabled: Boolean, text: String, toolTip: String, alResult: MutableList<XyElement>): Array<Int> {

        val arrElementID = drawCube(
            objectID, TYPE_STATE_W_FIGURE_25D, !isControlEnabled, toolTip,
            x, y, GRID_STEP * 4, GRID_STEP * 2, GRID_STEP, GRID_STEP * 4,
            if(state < 0) 2 else 0, if(state < 0) 0xFF_FF_00_00.toInt() else 0,
            if(state > 0) 0xFF_00_FF_00.toInt() else 0xFF_D0_D0_D0.toInt(),
            if(state > 0) 0xFF_00_E0_00.toInt() else 0xFF_C0_C0_C0.toInt(),
            if(state > 0) 0xFF_00_C0_00.toInt() else 0xFF_B0_B0_B0.toInt(),
            alResult
        )

        val label = XyElement(TYPE_STATE_W_TEXT_25D, -getRandomInt(), objectID)
        //label.init( timeZone )
        label.itReadOnly = true
        label.alPoint.add(XyPoint(x, y + GRID_STEP * 7))
        //--- ограничиваем вывод текста по ширине
        label.limitWidth = GRID_STEP * 4
        label.anchorX = XyElement.Anchor.LT
        label.anchorY = XyElement.Anchor.LT
        label.alignX = XyElement.Align.LT
        label.alignY = XyElement.Align.LT

        label.text = text
        label.toolTipText = text
        label.textColor = 0xFF_00_00_00.toInt()
        label.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        label.itFontBold = true

        alResult.add(label)

        return arrElementID
    }

    private fun addS25D(objectID: Int, scale: Int, x: Int, y: Int, state: Int, text: String, alResult: MutableList<XyElement>) {
        val marker = XyElement(TYPE_STATE_S_FIGURE_25D, -getRandomInt(), objectID)
        //marker.init( timeZone )
        marker.itReadOnly = true
        marker.alPoint.add(XyPoint(x + GRID_STEP * 2, y + GRID_STEP * 2))
        marker.toolTipText = text

        marker.markerType = XyElement.MarkerType.CIRCLE
        marker.markerSize = GRID_STEP * 2 / scale
        marker.drawColor = if(state < 0) 0xFF_FF_00_00.toInt() else 0xFF_00_00_00.toInt()
        marker.fillColor = if(state > 0) 0xFF_00_FF_00.toInt() else 0xFF_A0_A0_A0.toInt()
        marker.lineWidth = 2

        alResult.add(marker)

        //--- метка/текст оборудования
        val textY = y + GRID_STEP * 4

        val label = XyElement(TYPE_STATE_W_TEXT_25D, -getRandomInt(), objectID)
        //label.init( timeZone )
        label.itReadOnly = true
        label.alPoint.add(XyPoint(x, textY))
        //--- ограничиваем вывод текста по ширине
        label.limitWidth = GRID_STEP * 4
        label.anchorX = XyElement.Anchor.LT
        label.anchorY = XyElement.Anchor.LT
        label.alignX = XyElement.Align.LT
        label.alignY = XyElement.Align.LT

        label.text = text
        label.toolTipText = text
        label.textColor = 0xFF_00_00_00.toInt()
        label.fontSize = iCoreAppContainer.BASE_FONT_SIZE
        label.itFontBold = true

        alResult.add(label)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun drawCube(
        objectID: Int, elementType: String, isReadOnly: Boolean, toolTip: String,
        x: Int, y: Int, w: Int, h: Int, dw: Int, dh: Int,
        lineWidth: Int, drawColor: Int,
        topFillColor: Int, frontFillColor: Int, sideFillColor: Int,
        alResult: MutableList<XyElement>
    ): Array<Int> {
        //--- top panel
        val top = XyElement(elementType, -getRandomInt(), objectID)
        //top.init( timeZone )
        top.itReadOnly = isReadOnly
        top.toolTipText = toolTip

        top.itClosed = true
        top.alPoint.add(XyPoint(x, y + h))
        top.alPoint.add(XyPoint(x + dw, y))
        top.alPoint.add(XyPoint(x + w + dw, y))
        top.alPoint.add(XyPoint(x + w, y + h))

        top.lineWidth = lineWidth
        top.drawColor = drawColor
        top.fillColor = topFillColor

        alResult.add(top)

        //--- front panel
        val front = XyElement(elementType, -getRandomInt(), objectID)
        //front.init( timeZone )
        front.itReadOnly = isReadOnly
        front.toolTipText = toolTip

        front.itClosed = true
        front.alPoint.add(XyPoint(x, y + h))
        front.alPoint.add(XyPoint(x + w, y + h))
        front.alPoint.add(XyPoint(x + w, y + h + dh))
        front.alPoint.add(XyPoint(x, y + h + dh))

        front.lineWidth = lineWidth
        front.drawColor = drawColor
        front.fillColor = frontFillColor

        alResult.add(front)

        //--- side panel
        val side = XyElement(elementType, -getRandomInt(), objectID)
        //side.init( timeZone )
        side.itReadOnly = isReadOnly
        side.toolTipText = toolTip

        side.itClosed = true
        side.alPoint.add(XyPoint(x + w, y + h))
        side.alPoint.add(XyPoint(x + w + dw, y))
        side.alPoint.add(XyPoint(x + w + dw, y + dh))
        side.alPoint.add(XyPoint(x + w, y + h + dh))

        side.lineWidth = lineWidth
        side.drawColor = drawColor
        side.fillColor = sideFillColor

        alResult.add(side)

        return arrayOf(top.elementID, front.elementID, side.elementID)
    }

    private fun getSignalEnabled(objectState: ObjectState, hmSCS: Map<Int, SensorConfig>, signalConfig: SignalConfig): Boolean {
        //--- если разрешителей не прописано - то можно по умолчанию
        if(signalConfig.alPort.isEmpty()) return true
        else {
            for(portNum in signalConfig.alPort) {
                val scs = hmSCS[portNum] as SensorConfigSignal
                val signalState = objectState.tmSignalState[scs.descr]
                //--- если состояние хотя бы одного сигнала не определено - выходим с отрицательным результатом
                if(signalState == null) return false
                //--- если не было досрочных выходов
                else {
                    if(signalConfig.and) {
                        //--- достаточно одного облома
                        if(portNum > 0 && !signalState || portNum < 0 && signalState) return false
                    } else {
                        //--- достаточно одного успеха
                        if(portNum > 0 && signalState || portNum < 0 && !signalState) return true
                    }
                }
            }
            //--- если прописаны разрешающие/запрещающие порты/сигналы
            return signalConfig.and
        }
    }

}
//--- для тестов:
//7 - "Буровая бригада №1" - ООО `СБК-Техносервис` - тест с сигнальными лампами

//13 - "КАТойл Дриллинг"
//
//"Бригада № 8"

//"Буровая установка ZJ-20"
//
//"Буровая бригада №19"
//"Бригада Гиматудинова"
//"ДГУ 36 кВт"
//"МБУ бр№9"
//"Отчет показаний расхода ДТ на Буровой Установке Т-502"
