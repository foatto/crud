package foatto.server.service

import foatto.core.ActionType
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.OrgType
import foatto.server.entity.UserEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.ObjectDataResponse
import foatto.server.model.ObjectDataResponseSensorInfo
import foatto.server.model.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.repository.UserRepository
import jakarta.persistence.EntityManager
import kotlinx.datetime.Instant
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class APIService(
    private val entityManager: EntityManager,
    private val logonService: LogonService,
    private val userRepository: UserRepository,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) {

    fun getObjectData(
        token: String,
        objectName: String,
        start: String?,
        duration: Int?,
    ): ObjectDataResponse {
        // 2020-08-30T18:43:00Z
        // 2023-01-02T23:40:57Z
        val begTime = start?.let { Instant.parse(start).epochSeconds.toInt() }
        val endTime = begTime?.let { begTime + min(duration ?: 86_400, 86_400) }

        val userEntity = getUser(token) ?: return ObjectDataResponse(errorMessage = "Пользователь с таким логином и паролём не найден")
        if (userEntity.isDisabled == true) {
            return ObjectDataResponse(errorMessage = "Пользователь заблокирован")
        }

        val enabledUserIds = getEnabledUserIds(
            module = AppModuleMMS.OBJECT,
            actionType = ActionType.MODULE_TABLE,
            relatedUserIds = logonService.loadRelatedUserIds(
                userId = userEntity.id,
                parentId = userEntity.parentId ?: 0,
                orgType = userEntity.orgType ?: OrgType.ORG_TYPE_WORKER,
            ),
            roles = userEntity.roles,
        )

        val objectEntities = objectRepository.findByUserIdInAndName(enabledUserIds, objectName)
        if (objectEntities.size > 1) {
            return ObjectDataResponse(errorMessage = "Найдено ${objectEntities.size} объектов с таким наименованием")
        }
        val objectEntity = objectEntities.firstOrNull() ?: return ObjectDataResponse(errorMessage = "Объект с таким наименованием не найден")

        val sensors = mutableListOf<ObjectDataResponseSensorInfo>()
        sensorRepository.findByObj(objectEntity).forEach { sensorEntity ->
            val data = mutableListOf<Map<String, String>>()

            SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

            ApplicationService.queryNativeSql(
                entityManager,
                """
                    SELECT ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3
                    FROM MMS_agg_${sensorEntity.id}
                """ + (
                    begTime?.let {
                        """
                            WHERE ontime_0 BETWEEN $begTime AND $endTime
                            ORDER BY ontime_0 ASC
                        """
                    } ?: """
                            WHERE ontime_0 = ( SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} )
                         """
                    )
            ) { rs ->
                while (rs.next()) {
                    var pos = 1

                    val ontime0 = rs.getInt(pos++)
                    val ontime1 = rs.getInt(pos++)
                    val type0 = rs.getInt(pos++)
                    val value0 = rs.getDouble(pos++)
                    val value1 = rs.getDouble(pos++)
                    val value2 = rs.getDouble(pos++)
                    val value3 = rs.getDouble(pos++)

                    when (sensorEntity.sensorType) {
                        SensorConfig.SENSOR_GEO -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "speed" to type0.toString(),
                                "wgsX" to value0.toString(),
                                "wgsY" to value1.toString(),
                                "absoluteRun" to value2.toString(),
                                "relativeRun" to value3.toString(),
                            )
                        }

                        SensorConfig.SENSOR_WORK -> {
                            data += mapOf(
                                "begTime" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "endTime" to Instant.fromEpochSeconds(ontime1.toLong()).toString(),
                                "state" to type0.toString(),
                            )
                        }

                        in SensorConfig.analogueSensorTypes -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "value" to value1.toString(),
                                //"value" to value0.toString(),
                                //"smoothValue" to value1.toString(),
                            )
                        }

                        in SensorConfig.counterSensorTypes -> {
                            data += mapOf(
                                "time" to Instant.fromEpochSeconds(ontime0.toLong()).toString(),
                                "value" to value0.toString(),
                                "delta" to value1.toString(),
                                "avgMinute" to value2.toString(),
                                "avgHour" to value3.toString(),
                            )
                        }
                    }
                }
            }
            sensors += ObjectDataResponseSensorInfo(
                name = sensorEntity.descr,
                type = sensorEntity.sensorType,
                data = data,
            )
        }

        return ObjectDataResponse(sensors = sensors)
    }

    private fun getUser(token: String): UserEntity? {
        val tokens = token.split(" ")
        if (tokens.size != 2) {
            return null
        }
        tokens.firstOrNull()?.let { login ->
            tokens.lastOrNull()?.let { password ->
                return userRepository.findByLoginAndPassword(login, password).firstOrNull()
            }
        }
        return null
    }
}

/*
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
        } ?: run {
            devices[0] = DevicesResponse(
                companyId = 0,
                companyName = "",
                uds110 = mutableListOf(),
                uds101 = mutableListOf(),
                skk311 = mutableListOf(),
            )

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
        } ?: run {
            devices[0] = DevicesDetailResponse(
                companyId = 0,
                companyName = "",
                uds110 = mutableMapOf(),
                uds101 = mutableMapOf(),
                skk311 = mutableMapOf(),
            )
        }

        conn.commit()
        conn.close()

        return devices.values.toList()
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
                    AND TS_device.id <> 0
                    AND TS_object.id <> 0
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
                    AND TS_device.id <> 0
                    AND TS_object.id <> 0
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

    private fun getDateTimeFromYMD(date: String): Int {
        val tokens = date.split("-").filter { it.isNotEmpty() }
        if (tokens.size != 3) {
            return 0
        }
        val ye = tokens[0].toIntOrNull() ?: return 0
        val mo = tokens[1].toIntOrNull() ?: return 0
        val da = tokens[2].toIntOrNull() ?: return 0

        return ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId0).toEpochSecond().toInt()
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
 */