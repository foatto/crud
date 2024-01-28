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
import foatto.core_server.app.composite.server.CompositeStartData
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.spring.CoreSpringApp
import foatto.spring.controllers.CoreAppController
import foatto.spring.services.UserService
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.iTSApplication
import foatto.ts.spring.services.ObjectService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import kotlin.math.min

@RestController
class TSAppController(
    private val objectService: ObjectService,
    userService: UserService,
) : CoreAppController(userService), iTSApplication {

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

    override fun getObjectConfig(userConfig: UserConfig, objectId: Int): ObjectConfig =
        objectService.getObjectConfig(
            hmUserFullNames = hmUserFullNames,
            hmUserShortNames = hmUserShortNames,
            userConfig = userConfig,
            objectId = objectId,
        )
}