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
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.ds.TSHandler
import foatto.ts.iTSApplication
import foatto.ts.spring.models.*
import foatto.ts.spring.services.ObjectService
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.nio.ByteOrder
import java.time.Instant

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
        private const val URL_DEVICES_DETAIL = "devices_detail"
        private const val URL_DEVICE_DATA = "device_data"

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

        getUserId(conn, request.token)?.let { userId ->
            val userConfig = getUserConfig(conn, userId)
            val objectPermissions = userConfig.userPermission["ts_object"] ?: emptySet()

            val deviceInfos = getDeviceInfos(conn, request.companyId, request.type)

            deviceInfos.forEach { deviceInfo ->
                if (checkPerm(userConfig, objectPermissions, deviceInfo.userId)) {

                    val devicesResponse = devices.getOrPut(deviceInfo.userId) {
                        DevicesResponse(
                            companyId = deviceInfo.userId,
                            companyName = deviceInfo.userName,
                            uds110 = mutableListOf(),
                            uds101 = mutableListOf(),
                            skk311 = mutableListOf(),
                        )
                    }
                    when (deviceInfo.deviceType) {
                        TSHandler.DEVICE_TYPE_UDS -> devicesResponse.uds110 += deviceInfo.serialNo
                        TSHandler.DEVICE_TYPE_UDS_OLD -> devicesResponse.uds101 += deviceInfo.serialNo
                        TSHandler.DEVICE_TYPE_SKK -> devicesResponse.skk311 += deviceInfo.serialNo
                    }
                }
            }
        }

        conn.commit()
        conn.close()

        return devices.values.toList()
    }

    @PostMapping(value = ["/$URL_API_BASE/$URL_DEVICES_DETAIL/$URL_API_VERSION"])
    @Transactional
    fun devicesDetail(
        @RequestBody
        request: DevicesDetailRequest,
    ): List<DevicesDetailResponse> {
        val devices = mutableMapOf<Int, DevicesDetailResponse>()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

        getUserId(conn, request.token)?.let { userId ->
            val userConfig = getUserConfig(conn, userId)
            val objectPermissions = userConfig.userPermission["ts_object"] ?: emptySet()

            val deviceInfos = getDeviceInfos(conn, request.companyId, request.type)

            deviceInfos.forEach { deviceInfo ->
                if (checkPerm(userConfig, objectPermissions, deviceInfo.userId)) {
                    var sensorData: DeviceSensorShortData? = null

                    val rs = conn.executeQuery(
                        """
                            SELECT ontime , sensor_data 
                            FROM TS_data_${deviceInfo.objectId} 
                            WHERE ontime = ( SELECT MAX(ontime) FROM TS_data_${deviceInfo.objectId} ) 
                        """
                    )
                    while (rs.next()) {
                        val curTime = rs.getInt(1)
                        val bbIn = rs.getByteBuffer(2, ByteOrder.BIG_ENDIAN)

                        sensorData = DeviceSensorShortData(
                            onTime = Instant.ofEpochSecond(curTime.toLong()).toString(),
                            depth = AbstractObjectStateCalc.getSensorData(1, bbIn)?.toDouble(),
                            speed = AbstractObjectStateCalc.getSensorData(2, bbIn)?.toDouble(),
                            force = AbstractObjectStateCalc.getSensorData(3, bbIn)?.toDouble(),
                        )
                    }
                    rs.close()

                    val devicesDetailResponse = devices.getOrPut(deviceInfo.userId) {
                        DevicesDetailResponse(
                            companyId = deviceInfo.userId,
                            companyName = deviceInfo.userName,
                            uds110 = mutableMapOf(),
                            uds101 = mutableMapOf(),
                            skk311 = mutableMapOf(),
                        )
                    }
                    when (deviceInfo.deviceType) {
                        TSHandler.DEVICE_TYPE_UDS -> devicesDetailResponse.uds110[deviceInfo.serialNo] = sensorData
                        TSHandler.DEVICE_TYPE_UDS_OLD -> devicesDetailResponse.uds101[deviceInfo.serialNo] = sensorData
                        TSHandler.DEVICE_TYPE_SKK -> devicesDetailResponse.skk311[deviceInfo.serialNo] = sensorData
                    }
                }
            }
        }

        conn.commit()
        conn.close()

        return devices.values.toList()
    }

    @PostMapping(value = ["/$URL_API_BASE/$URL_DEVICE_DATA/$URL_API_VERSION"])
    @Transactional
    fun deviceData(
        @RequestBody
        request: DeviceDataRequest,
    ): DeviceDataResponse {
        val result = DeviceDataResponse(
            companyId = 0,
            companyName = "",
            type = "",
            serialNo = "",
            vers = "",
            imei = "",
            rows = mutableListOf(),
        )

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

        getUserId(conn, request.token)?.let { userId ->
            val userConfig = getUserConfig(conn, userId)
            val objectPermissions = userConfig.userPermission["ts_object"] ?: emptySet()

            getDeviceInfo(conn, request.serialNo, request.type)?.let { deviceInfo ->
                if (checkPerm(userConfig, objectPermissions, deviceInfo.userId)) {
                    result.companyId = deviceInfo.userId
                    result.companyName = deviceInfo.userName
                    result.type = request.type
                    result.serialNo = request.serialNo
                    result.vers = deviceInfo.fwVersion
                    result.imei = deviceInfo.imei

                    val rs = conn.executeQuery(
                        """
                            SELECT ontime, sensor_data 
                            FROM TS_data_${deviceInfo.objectId} 
                            WHERE  
                        """ +
                            if (request.isArchive) {
                                (request.start?.let { start ->
                                    """
                                        ontime >= ${Instant.parse(start).epochSecond}
                                    """
                                } ?: "") +
                                    (request.end?.let { end ->
                                        """
                                            ${request.start?.let { " AND " } ?: ""}
                                            ontime <= ${Instant.parse(end).epochSecond}
                                        """
                                    } ?: "") +
                                    """
                                        ORDER BY ontime
                                    """
                            } else {
                                """
                                ontime = ( SELECT MAX(ontime) FROM TS_data_${deviceInfo.objectId} )
                            """
                            }
                    )
                    while (rs.next()) {
                        val curTime = rs.getInt(1)
                        val bbIn = rs.getByteBuffer(2, ByteOrder.BIG_ENDIAN)

                        result.rows += if (deviceInfo.deviceType == TSHandler.DEVICE_TYPE_SKK) {
                            DeviceSensorFullData(
                                onTime = Instant.ofEpochSecond(curTime.toLong()).toString(),
                                depth = AbstractObjectStateCalc.getSensorData(1, bbIn)?.toDouble(),
                                speed = AbstractObjectStateCalc.getSensorData(2, bbIn)?.toDouble(),
                                force = AbstractObjectStateCalc.getSensorData(3, bbIn)?.toDouble(),
                                mdmRC = AbstractObjectStateCalc.getSensorData(10, bbIn)?.toInt(),
                                mdmCSQ = AbstractObjectStateCalc.getSensorData(11, bbIn)?.toInt(),
                                mdmMany = AbstractObjectStateCalc.getSensorData(12, bbIn)?.toInt(),
                                depthMin = AbstractObjectStateCalc.getSensorData(20, bbIn)?.toInt(),
                                depthMax = AbstractObjectStateCalc.getSensorData(21, bbIn)?.toInt(),
                                depthFlags = AbstractObjectStateCalc.getSensorData(22, bbIn)?.toInt(),
                                speedMin = AbstractObjectStateCalc.getSensorData(30, bbIn)?.toInt(),
                                speedMax = AbstractObjectStateCalc.getSensorData(31, bbIn)?.toInt(),
                                speedFlags = AbstractObjectStateCalc.getSensorData(32, bbIn)?.toInt(),
                                forceMin = AbstractObjectStateCalc.getSensorData(40, bbIn)?.toInt(),
                                forceMax = AbstractObjectStateCalc.getSensorData(41, bbIn)?.toInt(),
                                forceFlags = AbstractObjectStateCalc.getSensorData(42, bbIn)?.toInt(),
                                wire = AbstractObjectStateCalc.getSensorData(50, bbIn)?.toInt(),
                                encoder = AbstractObjectStateCalc.getSensorData(51, bbIn)?.toInt(),
                                roller = AbstractObjectStateCalc.getSensorData(52, bbIn)?.toInt(),
                                area = AbstractObjectStateCalc.getSensorData(60, bbIn)?.toString(),
                                bush = AbstractObjectStateCalc.getSensorData(61, bbIn)?.toString(),
                                well = AbstractObjectStateCalc.getSensorData(62, bbIn)?.toString(),
                                shop = AbstractObjectStateCalc.getSensorData(63, bbIn)?.toString(),
                                crew = AbstractObjectStateCalc.getSensorData(64, bbIn)?.toString(),
                                auto = AbstractObjectStateCalc.getSensorData(65, bbIn)?.toString(),
                                people = AbstractObjectStateCalc.getSensorData(66, bbIn)?.toString(),
                            )
                        } else {
                            DeviceSensorFullData(
                                onTime = Instant.ofEpochSecond(curTime.toLong()).toString(),
                                depth = AbstractObjectStateCalc.getSensorData(1, bbIn)?.toDouble(),
                                speed = AbstractObjectStateCalc.getSensorData(2, bbIn)?.toDouble(),
                                force = AbstractObjectStateCalc.getSensorData(3, bbIn)?.toDouble(),
                            )
                        }
                    }
                    rs.close()
                }
            }
        }

        conn.commit()
        conn.close()

        return result
    }

    private fun getUserId(conn: CoreAdvancedConnection, token: String): Int? {
        val tokens = token.split(" ")
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
            null
        }
        rsUsers.close()

        return userId
    }

    private fun getDeviceInfos(conn: CoreAdvancedConnection, companyId: Int?, deviceType: String?): List<DeviceInfo> {
        val deviceInfos = mutableListOf<DeviceInfo>()

        val rs = conn.executeQuery(
            """
                SELECT SYSTEM_users.id, SYSTEM_users.full_name, 
                       TS_device.type, TS_device.serial_no, TS_device.fw_version, TS_device.imei,
                       TS_object.id   
                FROM TS_device, TS_object, SYSTEM_users
                WHERE TS_device.object_id = TS_object.id
                AND TS_object.user_id = SYSTEM_users.id  
            """ +
                (companyId?.let { " AND SYSTEM_users.id = $companyId " } ?: "") +
                (deviceType?.let { " AND TS_device.type = ${requestTypeToDeviceType[deviceType] ?: 0} " } ?: "")
        )
        while (rs.next()) {
            var pos = 1

            deviceInfos += DeviceInfo(
                userId = rs.getInt(pos++),
                userName = rs.getString(pos++),
                deviceType = rs.getInt(pos++),
                serialNo = rs.getString(pos++),
                fwVersion = rs.getString(pos++),
                imei = rs.getString(pos++),
                objectId = rs.getInt(pos++),
            )
        }
        rs.close()

        return deviceInfos
    }

    private fun getDeviceInfo(conn: CoreAdvancedConnection, serialNo: String, deviceType: String): DeviceInfo? {
        var deviceInfo: DeviceInfo? = null

        val rs = conn.executeQuery(
            """
                SELECT SYSTEM_users.id, SYSTEM_users.full_name, 
                       TS_device.type, TS_device.serial_no, TS_device.fw_version, TS_device.imei,
                       TS_object.id   
                FROM TS_device, TS_object, SYSTEM_users
                WHERE TS_device.object_id = TS_object.id
                    AND TS_object.user_id = SYSTEM_users.id  
                    AND TS_device.serial_no = '$serialNo' 
                    AND TS_device.type = ${requestTypeToDeviceType[deviceType] ?: 0} 
            """
        )
        while (rs.next()) {
            var pos = 1

            deviceInfo = DeviceInfo(
                userId = rs.getInt(pos++),
                userName = rs.getString(pos++),
                deviceType = rs.getInt(pos++),
                serialNo = rs.getString(pos++),
                fwVersion = rs.getString(pos++),
                imei = rs.getString(pos++),
                objectId = rs.getInt(pos++),
            )
        }
        rs.close()

        return deviceInfo
    }

    private fun checkPerm(userConfig: UserConfig, hsPermission: Set<String>, userId: Int): Boolean {
        for ((relName, _) in UserRelation.arrNameDescr) {
            if (userConfig.getUserIDList(relName).contains(userId)) {
                return hsPermission.contains("table_$relName")
            }
        }
        return hsPermission.contains("table_${UserRelation.OTHER}")
    }

}

private class DeviceInfo(
    val userId: Int,
    val userName: String,
    val deviceType: Int,
    val serialNo: String,
    val fwVersion: String,
    val imei: String,
    val objectId: Int,
)
