package foatto.ts.spring.controllers

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.CompositeResponse
import foatto.core.link.GraphicResponse
import foatto.core.link.MenuData
import foatto.core.link.ResponseCode
import foatto.core.link.XyResponse
import foatto.core.util.AdvancedLogger
import foatto.core_server.app.composite.server.CompositeStartData
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.spring.CoreSpringApp
import foatto.spring.controllers.CoreAppController
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigBase
import foatto.ts.core_ts.sensor.config.SensorConfigSetup
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication
import foatto.ts.spring.repositories.ObjectRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import kotlin.math.min

@RestController
class TSAppController : CoreAppController(), iTSApplication {

    @Value("\${control_enabled_role_id}")
    override val controlEnabledRoleId: String = ""

    @Value("\${companies_parent_id}")
    override val companiesParentId: String = ""

    @Value("\${ts_user_role_id}")
    override val alTSUserRoleId: Array<String> = emptyArray()

//-------------------------------------------------------------------------------------------------

    @PostMapping("/api/app")
    override fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return super.app(appRequest)
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/xy")
    override fun xy(
        @RequestBody
        xyActionRequest: XyActionRequest
    ): XyActionResponse {
        return super.xy(xyActionRequest)
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/graphic")
    override fun graphic(
        @RequestBody
        graphicActionRequest: GraphicActionRequest
    ): GraphicActionResponse {
        return super.graphic(graphicActionRequest)
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getCompositeResponse(compositeStartData: CompositeStartData): AppResponse {
        val shortTitle = compositeStartData.shortTitle.substring(0, min(32000, compositeStartData.shortTitle.length))
        val fullTitle = compositeStartData.fullTitle.substring(0, min(32000, compositeStartData.fullTitle.length))

        return AppResponse(
            code = ResponseCode.COMPOSITE,
            composite = CompositeResponse(
                xyResponse = XyResponse(
                    documentConfig = CoreSpringApp.hmXyDocumentConfig["ts_state"]!!,
                    startParamId = compositeStartData.xyStartDataId,
                    shortTitle = shortTitle,
                    fullTitle = fullTitle,
                    alServerActionButton = compositeStartData.alServerActionButton,
                ),
                graphicResponse = GraphicResponse(
                    documentTypeName = "ts_graphic_dsltt",
                    startParamId = compositeStartData.graphicStartDataId,
                    shortTitle = shortTitle,
                    fullTitle = fullTitle,
                    rangeType = compositeStartData.rangeType,
                    begTime = compositeStartData.begTime,
                    endTime = compositeStartData.endTime,
                ),
            )
        )
    }

    //--- пропускаем логи по запуску модулей из 1С, отчётов, графиков, показов картографии
    override fun checkLogSkipAliasPrefix(alias: String): Boolean =
        alias.startsWith("ts_report_") || alias.startsWith("ts_graphic_") || alias.startsWith("ts_show_")

    override fun menuInit(conn: CoreAdvancedConnection, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Учёт -------------------------------------------------------------------------------------------------------

        val alMenuGeneral = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "ts_object", true)

        if (alMenuGeneral.size > 0) {
            alMenu.add(MenuData("", "Учёт", alMenuGeneral))
        }

        //--- Контроль --------------------------------------------------------------------------------------------------------

        val alMenuControl = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuControl, "ts_composite_all", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuControl, "ts_show_state", false)

        if (alMenuControl.size > 0) {
            alMenu.add(MenuData("", "Контроль", alMenuControl))
        }

        //--- Графики --------------------------------------------------------------------------------------------------------

        val alMenuGraphic = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_dsltt", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_depth", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_speed", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_load", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_temperature_in", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_temperature_out", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_signal_level", false)

        if (alMenuGraphic.size > 0) {
            alMenu.add(MenuData("", "Графики", alMenuGraphic))
        }

        //--- Устройства ------------------------------------------------------------------------------------------------------

        val alMenuDevice = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_device", true)
//        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_device_command_history", true)

        addSeparator(alMenuDevice)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_log_journal", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_log_ds_uds", true)

        if (alMenuDevice.size > 1) {
            alMenu.add(MenuData("", "Приборы", alMenuDevice))
        }

        //--- Справочники --------------------------------------------------------------------------------------------------------

        val alMenuDir = mutableListOf<MenuData>()
        //addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "ts_client", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "ts_company", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "ts_user", true)

        if (alMenuDir.size > 0) {
            alMenu.add(MenuData("", "Справочники", alMenuDir))
        }

        //--- Система --------------------------------------------------------------------------------------------------------

        val alMenuSystem = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_property", false)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_alias", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_permission", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role_permission", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_log_user", true)

        if (alMenuSystem.size > 3) {
            alMenu.add(MenuData("", "Система", alMenuSystem))
        }

        //----------------------------------------------------------------------------------------------------------------------

        return alMenu
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Autowired
    private lateinit var objectRepository: ObjectRepository

    override fun getObjectConfig(userConfig: UserConfig, objectId: Int): ObjectConfig {
        val objectEntity = objectRepository.findByIdOrNull(objectId) ?: "ObjectConfig not exist for object_id = $objectId".let {
            AdvancedLogger.error(it)
            throw Exception(it)
        }

        //--- add the name of the object with its short login name
        val shortUserName = hmUserShortNames[objectEntity.userId]

        val objectConfig = ObjectConfig(
            objectId = objectId,
            userId = objectEntity.userId,
            name = objectEntity.name + if (shortUserName.isNullOrBlank()) "" else " ($shortUserName)",
            model = objectEntity.model,
        )

        //--- fill report title
        objectConfig.alTitleName.add("Владелец:")
        objectConfig.alTitleValue.add(hmUserFullNames[objectConfig.userId] ?: "(неизвестно)")
        objectConfig.alTitleName.add("Наименование:")
        objectConfig.alTitleValue.add(objectConfig.name)
        objectConfig.alTitleName.add("Модель:")
        objectConfig.alTitleValue.add(objectConfig.model)

        //--- sensor configuration loading
        //--- (notes: "serial no" and "start date of operation" will be ignored)
        objectEntity.sensors.forEach { sensorEntity ->

            val sensorType = sensorEntity.sensorType
            val portNum = sensorEntity.portNum

            val hmSC = objectConfig.hmSensorConfig.getOrPut(sensorType) { mutableMapOf() }
            when (sensorType) {
                SensorConfig.SENSOR_NEXT_CLEAN_DATETIME -> {
                    hmSC[portNum] = SensorConfig(
                        id = sensorEntity.id,
                        name = sensorEntity.name,
                        group = sensorEntity.group,
                        descr = sensorEntity.descr,
                        portNum = portNum,
                        sensorType = sensorType,
                    )
                }
                SensorConfig.SENSOR_STATE -> {
                    hmSC[portNum] = SensorConfigState(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                    )
                }
                SensorConfig.SENSOR_DEPTH, SensorConfig.SENSOR_SPEED, SensorConfig.SENSOR_LOAD,
                SensorConfig.SENSOR_TEMPERATURE_IN, SensorConfig.SENSOR_TEMPERATURE_OUT,
                SensorConfig.SENSOR_SIGNAL_LEVEL -> {
                    hmSC[portNum] = SensorConfigAnalogue(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        minGraphicView = sensorEntity.minGraphicView ?: 0.0,
                        maxGraphicView = sensorEntity.maxGraphicView ?: 0.0,
                        minGraphicLimit = sensorEntity.minGraphicLimit ?: 0.0,
                        maxGraphicLimit = sensorEntity.maxGraphicLimit ?: 0.0,
                        minStateView = sensorEntity.minStateView ?: 0.0,
                        maxStateView = sensorEntity.maxStateView ?: 0.0,
                    )
                }
                SensorConfig.SENSOR_SETUP -> {
                    hmSC[portNum] = SensorConfigSetup(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        showPos = sensorEntity.showPos ?: 0,
                        valueType = sensorEntity.valueType ?: SensorConfigSetup.VALUE_TYPE_NUMBER,
                        prec = sensorEntity.prec ?: 0,
                    )
                }
                else -> {
                    AdvancedLogger.error("Unknown sensorType = $sensorType for sensorId = ${sensorEntity.id}")
                }
            }

            hmSC[portNum]?.let { sensorConfig ->
                if (sensorConfig is SensorConfigBase) {
                    sensorEntity.calibration
                        //--- sorting set for interpolation
                        .sortedBy { sensorConfigCalibrationEntity -> sensorConfigCalibrationEntity.sensorValue }
                        .forEach { sensorConfigCalibrationEntity ->
                            sensorConfig.alValueSensor.add(sensorConfigCalibrationEntity.sensorValue)
                            sensorConfig.alValueData.add(sensorConfigCalibrationEntity.sensorData)
                        }
                }
            }

        }

        return objectConfig
    }
}