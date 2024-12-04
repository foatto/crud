package foatto.ts.spring.controllers

import foatto.core.app.chart.ChartActionRequest
import foatto.core.app.chart.ChartActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.MenuData
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.UserRelation
import foatto.spring.CoreSpringApp
import foatto.spring.controllers.CoreAppController
import foatto.spring.services.UserService
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.ds.TSHandler
import foatto.ts.iTSApplication
import foatto.ts.spring.models.DevicesRequest
import foatto.ts.spring.models.DevicesResponse
import foatto.ts.spring.services.ObjectService
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
        chartActionRequest: ChartActionRequest
    ): ChartActionResponse {
        return super.graphic(chartActionRequest)
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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
//        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_dsl", false)
//        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "ts_graphic_tt", false)
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

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        private const val URL_API_BASE = "api"
        private const val URL_API_VERSION = "v1"

        private const val URL_DEVICES = "devices"

        private val requestTypeToDeviceType = mapOf(
            "uds110" to TSHandler.DEVICE_TYPE_UDS,
            "uds101" to TSHandler.DEVICE_TYPE_UDS_OLD,
            "skk311" to TSHandler.DEVICE_TYPE_SKK,
        )
    }

    @PostMapping(value = ["/$URL_API_BASE/$URL_DEVICES/$URL_API_VERSION"])
    @Transactional
    fun devices(
        @RequestBody
        request: DevicesRequest,
    ): List<DevicesResponse> {

        val devices = mutableMapOf<Int, DevicesResponse>()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

        val tokens = request.token.split(" ")
        val (login, password) = tokens.firstOrNull() to tokens.lastOrNull()
        val rsUsers = conn.executeQuery(
            """
                SELECT id
                FROM SYSTEM_users
                WHERE login = '$login'
                AND pwd = '$password'
            """
        )
        val userId = if (rsUsers.next()) {
            rsUsers.getInt(1)
        } else {
            0
        }
        rsUsers.close()

        if (userId != 0) {
            val userConfig = getUserConfig(conn, userId)
            val objectPermissions = userConfig.userPermission["ts_object"] ?: emptySet()

            val deviceIds = mutableListOf<Int>()
            val deviceTypes = mutableListOf<Int>()
            val userIds = mutableListOf<Int>()
            val userNames = mutableListOf<String>()

            val rsDevices = conn.executeQuery(
                """
                    SELECT TS_device.id, TS_device.type, TS_object.user_id, SYSTEM_users.full_name  
                    FROM TS_device, TS_object, SYSTEM_users
                    WHERE TS_device.object_id = TS_object.id
                    AND TS_object.user_id = SYSTEM_users.id  
                """ +
                    (
                        request.companyId?.let { companyId ->
                            """
                                AND SYSTEM_users.id = $companyId  
                            """
                        } ?: ""
                        ) +
                    (
                        request.type?.let { type ->
                            """
                                AND TS_device.type = ${requestTypeToDeviceType[type] ?: 0}  
                            """
                        } ?: ""
                        )
            )
            while (rsDevices.next()) {
                var pos = 1

                deviceIds += rsDevices.getInt(pos++)
                deviceTypes += rsDevices.getInt(pos++)
                userIds += rsDevices.getInt(pos++)
                userNames += rsDevices.getString(pos++)
            }
            rsDevices.close()
            /*
    device_index        INT NOT NULL,
    serial_no           VARCHAR(250),
    cell_num            VARCHAR(250),
    fw_version          VARCHAR(250),
    imei                VARCHAR(250),
             */

            userIds.forEachIndexed { index, rowUserId ->
                if (checkPerm(userConfig, objectPermissions, rowUserId)) {
                    val deviceId = deviceIds[index]
                    val deviceType = deviceTypes[index]
                    val userName = userNames[index]

                    val devicesResponse = devices.getOrPut(rowUserId) {
                        DevicesResponse(
                            companyId = rowUserId,
                            companyName = userName,
                            uds110 = mutableListOf(),
                            uds101 = mutableListOf(),
                            skk311 = mutableListOf(),
                        )
                    }
                    when (deviceType) {
                        TSHandler.DEVICE_TYPE_UDS -> devicesResponse.uds110 += deviceIds[index]
                        TSHandler.DEVICE_TYPE_UDS_OLD -> devicesResponse.uds101 += deviceIds[index]
                        TSHandler.DEVICE_TYPE_SKK -> devicesResponse.skk311 += deviceIds[index]
                    }
                }
            }
        }

        conn.commit()
        conn.close()

        return devices.values.toList()
    }

    private fun checkPerm(userConfig: UserConfig, hsPermission: Set<String>, userId: Int): Boolean {
        for ((relName, _) in UserRelation.arrNameDescr) {
            if (userConfig.getUserIDList(relName).contains(userId)) {
                return hsPermission.contains("table_$relName")
            }
        }
        return hsPermission.contains("table_${UserRelation.OTHER}")
    }

    /*
checkPerm(PERM_TABLE, recordUserID)


 */

}