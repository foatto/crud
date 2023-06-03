package foatto.mms.core_mms.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.ds.CoreTelematicFunction
import foatto.mms.core_mms.ds.GalileoFunction
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.iMMSApplication
import java.io.File

class cDevice : cStandart() {

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val md = model as mDevice
        if (column == md.columnSerialNo) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            val lastSessionTime = (hmColumnData[md.columnDeviceLastSessionTime] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()
            //--- раскраска номера контроллера в зависимости от времени последнего входа в систему
            val curTime = getCurrentTimeInt()

            tci.foreColor = if (lastSessionTime == 0) {
                TABLE_CELL_FORE_COLOR_DISABLED
            } else if (curTime - lastSessionTime > 7 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_CRITICAL
            } else if (curTime - lastSessionTime > 1 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_WARNING
            } else {
                TABLE_CELL_FORE_COLOR_NORMAL
            }
        }
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        createSensors(hmColumnData)
//        clearOldCameraInfo(hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        createSensors(hmColumnData)

        //--- (re)create DataServer restart flag file
        val dataServerIniFileName = (application as iMMSApplication).dataServerIniFileName
        File(dataServerIniFileName).copyTo(File(dataServerIniFileName + "_"), true)

//        clearOldCameraInfo(hmColumnData)

        return postURL
    }

//    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
//        super.postDelete(id, hmColumnData)
//
//        clearOldCameraInfo(hmColumnData)
//    }

    //----------------------------------------------------------------------------------------------------------------------------------------

//    private fun clearOldCameraInfo(hmColumnData: Map<iColumn, iData>) {
//        val md = model as mDevice
//
//        val deviceID = (hmColumnData[md.columnDevice] as DataInt).intValue
//        val objectId = (hmColumnData[md.columnObject] as DataInt).intValue
//
//        conn.executeUpdate(" DELETE FROM VC_camera WHERE name = '$deviceID' AND object_id <> $objectId ")
//    }

    private fun createSensors(hmColumnData: Map<iColumn, iData>) {
        val m = model as mDevice

        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue
        if (objectId != 0) {
            val deviceIndex = (hmColumnData[m.columnDeviceIndex] as DataInt).intValue

            val isESDCreate = (hmColumnData[m.columnESDCreatingEnabled] as DataBoolean).value
            if (isESDCreate) {
                val alGroupName = m.alColumnESDGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPrefix = m.alColumnESDDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPostfix = m.alColumnESDDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createESDSensors(objectId, deviceIndex, alGroupName, alDescrPrefix, alDescrPostfix)
            }

            val isEmisCreate = (hmColumnData[m.columnEmisCreatingEnabled] as DataBoolean).value
            if (isEmisCreate) {
                val alGroupName = m.alColumnEmisGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPrefix = m.alColumnEmisDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPostfix = m.alColumnEmisDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createEmisSensors(objectId, deviceIndex, alGroupName, alDescrPrefix, alDescrPostfix)
            }

            val isUSSCreate = (hmColumnData[m.columnUSSCreatingEnabled] as DataBoolean).value
            if (isUSSCreate) {
                val alGroupName = m.alColumnUSSGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPrefix = m.alColumnUSSDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPostfix = m.alColumnUSSDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createUSSSensors(objectId, deviceIndex, alGroupName, alDescrPrefix, alDescrPostfix)
            }

            val isMercuryCreate = (hmColumnData[m.columnMercuryCreatingEnabled] as DataBoolean).value
            if (isMercuryCreate) {
                val alGroupName = m.alColumnMercuryGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPrefix = m.alColumnMercuryDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alDescrPostfix = m.alColumnMercuryDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createMercurySensors(objectId, deviceIndex, alGroupName, alDescrPrefix, alDescrPostfix)
            }
        }
    }

    private fun createESDSensors(
        objectId: Int,
        deviceIndex: Int,
        alGroupName: List<String>,
        alDescrPrefix: List<String>,
        alDescrPostfix: List<String>,
    ) {
        for (si in 0 until mDevice.MAX_PORT_PER_SENSOR) {
            val groupName = alGroupName[si].trim()
            val descrPrefix = alDescrPrefix[si].trim()
            val descrPostfix = alDescrPostfix[si].trim()

            if (descrPrefix.isNotEmpty() || descrPostfix.isNotEmpty()) {
                addStateSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE,
                    portNum = GalileoFunction.PORT_NUM_ESD_STATUS,
                    descrBody = "Состояние расходомера",
                )
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = GalileoFunction.PORT_NUM_ESD_VOLUME,
                    descrBody = "Расходомер",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = GalileoFunction.PORT_NUM_ESD_FLOW,
                    descrBody = "Скорость потока",
                )
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = GalileoFunction.PORT_NUM_ESD_CAMERA_VOLUME,
                    descrBody = "Расходомер камеры подачи",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = GalileoFunction.PORT_NUM_ESD_CAMERA_FLOW,
                    descrBody = "Скорость потока камеры подачи",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = GalileoFunction.PORT_NUM_ESD_CAMERA_TEMPERATURE,
                    descrBody = "Температура камеры подачи",
                )
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_LIQUID_USING,
                    portNum = GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_VOLUME,
                    descrBody = "Расходомер камеры обратки",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_FLOW,
                    descrBody = "Скорость потока камеры обратки",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = GalileoFunction.PORT_NUM_ESD_REVERSE_CAMERA_TEMPERATURE,
                    descrBody = "Температура камеры обратки",
                )
            }
        }
    }

    private fun createEmisSensors(
        objectId: Int,
        deviceIndex: Int,
        alGroupName: List<String>,
        alDescrPrefix: List<String>,
        alDescrPostfix: List<String>,
    ) {
        for (si in 0 until mDevice.MAX_PORT_PER_SENSOR) {
            val groupName = alGroupName[si].trim()
            val descrPrefix = alDescrPrefix[si].trim()
            val descrPostfix = alDescrPostfix[si].trim()

            if (descrPrefix.isNotEmpty() || descrPostfix.isNotEmpty()) {
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_MASS_FLOW,
                    portNum = GalileoFunction.PORT_NUM_EMIS_MASS_FLOW,
                    descrBody = "Массовый расход",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_DENSITY,
                    portNum = GalileoFunction.PORT_NUM_EMIS_DENSITY,
                    descrBody = "Плотность",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_TEMPERATURE,
                    portNum = GalileoFunction.PORT_NUM_EMIS_TEMPERATURE,
                    descrBody = "Температура",
                )
                addAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_FLOW,
                    portNum = GalileoFunction.PORT_NUM_EMIS_VOLUME_FLOW,
                    descrBody = "Объёмный расход",
                )
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_MASS_ACCUMULATED,
                    portNum = GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_MASS,
                    descrBody = "Накопленная масса",
                )
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    portNum = GalileoFunction.PORT_NUM_EMIS_ACCUMULATED_VOLUME,
                    descrBody = "Накопленный объём",
                )
            }
        }
    }

    private fun createUSSSensors(
        objectId: Int,
        deviceIndex: Int,
        alGroupName: List<String>,
        alDescrPrefix: List<String>,
        alDescrPostfix: List<String>,
    ) {
        for (si in 0 until mDevice.MAX_PORT_PER_SENSOR) {
            val groupName = alGroupName[si].trim()
            val descrPrefix = alDescrPrefix[si].trim()
            val descrPostfix = alDescrPostfix[si].trim()

            if (descrPrefix.isNotEmpty() || descrPostfix.isNotEmpty()) {
                addCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_VOLUME_ACCUMULATED,
                    portNum = GalileoFunction.PORT_NUM_USS_ACCUMULATED_VOLUME,
                    descrBody = "Накопленный объём",
                )
            }
        }
    }

    private fun createMercurySensors(
        objectId: Int,
        deviceIndex: Int,
        alGroupName: List<String>,
        alDescrPrefix: List<String>,
        alDescrPostfix: List<String>,
    ) {
        for (si in 0 until mDevice.MAX_PORT_PER_SENSOR) {
            val groupName = alGroupName[si].trim()
            val descrPrefix = alDescrPrefix[si].trim()
            val descrPostfix = alDescrPostfix[si].trim()

            if (descrPrefix.isNotEmpty() || descrPostfix.isNotEmpty()) {

                addEnergoCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AD,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_DIRECT,
                    descrBody = "Электроэнергия активная прямая",
                )
                addEnergoCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AR,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_COUNT_ACTIVE_REVERSE,
                    descrBody = "Электроэнергия активная обратная",
                )
                addEnergoCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RD,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_DIRECT,
                    descrBody = "Электроэнергия реактивная прямая",
                )
                addEnergoCounterSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RR,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_COUNT_REACTIVE_REVERSE,
                    descrBody = "Электроэнергия реактивная обратная",
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_A,
                    descrBody = "Напряжение по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_B,
                    descrBody = "Напряжение по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_VOLTAGE_C,
                    descrBody = "Напряжение по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_A,
                    descrBody = "Ток по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_B,
                    descrBody = "Ток по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_C,
                    descrBody = "Ток по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_A,
                    descrBody = "Ток по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_B,
                    descrBody = "Ток по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_CURRENT,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_CURRENT_C,
                    descrBody = "Ток по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_A,
                    descrBody = "Коэффициент мощности по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_B,
                    descrBody = "Коэффициент мощности по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_KOEF_C,
                    descrBody = "Коэффициент мощности по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_A,
                    descrBody = "Мощность активная по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_B,
                    descrBody = "Мощность активная по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_C,
                    descrBody = "Мощность активная по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_A,
                    descrBody = "Мощность реактивная по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_B,
                    descrBody = "Мощность реактивная по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_C,
                    descrBody = "Мощность реактивная по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_A,
                    descrBody = "Мощность полная по фазе A",
                    phase = 1,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_B,
                    descrBody = "Мощность полная по фазе B",
                    phase = 2,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_C,
                    descrBody = "Мощность полная по фазе C",
                    phase = 3,
                )

                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_ACTIVE_ABC,
                    descrBody = "Мощность активная по всем фазам",
                    phase = 0,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_REACTIVE_ABC,
                    descrBody = "Мощность реактивная по всем фазам",
                    phase = 0,
                )
                addEnergoAnalogueSensor(
                    objectId = objectId,
                    deviceIndex = deviceIndex,
                    sensorIndex = si,
                    groupName = groupName,
                    descrPrefix = descrPrefix,
                    descrPostfix = descrPostfix,
                    sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL,
                    portNum = GalileoFunction.PORT_NUM_MERCURY_POWER_FULL_ABC,
                    descrBody = "Мощность полная по всем фазам",
                    phase = 0,
                )
            }
        }
    }

    private fun addStateSensor(
        objectId: Int,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
    ) {
        conn.executeUpdate(
            """
                INSERT INTO MMS_sensor( 
                    id , object_id , name , group_name , descr , 
                    port_num , 
                    sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da 
                ) VALUES ( 
                    ${conn.getNextIntId("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix $descrBody $descrPostfix' , 
                    ${deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex} , 
                    $sensorType , 0 , 0 , 2000 , 1 , 1 
                )
            """
        )
    }

    private fun addCounterSensor(
        objectId: Int,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
    ) {
        conn.executeUpdate(
            """
                INSERT INTO MMS_sensor( 
                    id , object_id , name , group_name , descr , 
                    port_num , 
                    sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                    min_on_time, min_off_time, ignore_min_sensor , ignore_max_sensor , is_absolute_count , in_out_type , liquid_name            
                ) VALUES ( 
                    ${conn.getNextIntId("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix $descrBody $descrPostfix' , 
                    ${deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex} , 
                    $sensorType , 0, 0 , 2000 , 1 , 1 ,
                    1 , 1 , 0 , 0 , 1 , ${SensorConfigCounter.CALC_TYPE_OUT} , ''                            
                )
            """
        )
    }

    private fun addEnergoCounterSensor(
        objectId: Int,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
    ) {
        conn.executeUpdate(
            """
                INSERT INTO MMS_sensor( 
                    id , object_id , name , group_name , descr , 
                    port_num , 
                    sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                    ignore_min_sensor , ignore_max_sensor , is_absolute_count             
                ) VALUES ( 
                    ${conn.getNextIntId("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix $descrBody $descrPostfix' , 
                    ${deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex} , 
                    $sensorType , 0, 0 , 2000 , 1 , 1 ,
                    0 , 0 , 1                            
                )
            """
        )
    }

    private fun addAnalogueSensor(
        objectId: Int,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
    ) {
        conn.executeUpdate(
            """
                INSERT INTO MMS_sensor( 
                    id , object_id , name , group_name , descr , 
                    port_num , 
                    sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                    smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                    analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                ) VALUES ( 
                    ${conn.getNextIntId("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix $descrBody $descrPostfix' , 
                    ${deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex} , 
                    $sensorType , 0, 0 , 2000 , 1 , 1 ,
                    0 , 0 , 0 , 0 ,                             
                    0 , 0 , 0 , 0                              
                )
            """
        )
    }

    private fun addEnergoAnalogueSensor(
        objectId: Int,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
        phase: Int,
    ) {
        conn.executeUpdate(
            """
                INSERT INTO MMS_sensor( 
                    id , object_id , name , group_name , descr , 
                    port_num , 
                    sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                    smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                    analog_min_view , analog_max_view , analog_min_limit , analog_max_limit , energo_phase                 
                ) VALUES ( 
                    ${conn.getNextIntId("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix $descrBody $descrPostfix' , 
                    ${deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex} , 
                    $sensorType , 0, 0 , 2000 , 1 , 1 ,
                    0 , 0 , 0 , 0 ,                             
                    0 , 0 , 0 , 0 , $phase                              
                )
            """
        )
    }
}
/*
-- дискретные датчики (времени работы оборудования и сигналов)
    bound_value         INT,    -- граничное значение (<= bound_value - логический 0, выше - логический 1
    active_value        INT,    -- активное/рабочее значение
    min_on_time         INT,    -- минимальное (учитываемое) время работы
    min_off_time        INT,    -- минимальное (учитываемое) время простоя
    beg_work_value      FLOAT8, -- наработка на момент установки датчика
    cmd_on_id           INT,    -- команда на включение
    cmd_off_id          INT,    -- команда на отключение
    signal_on           VARCHAR( 250 ), -- сигналы, разрешающие включение
    signal_off          VARCHAR( 250 ), -- сигналы, разрешающие отключение
    -- применяется только для показаний электросчётчиков
    energo_phase        INT NOT NULL DEFAULT(0),
    -- применяется только для уровнемеров
    container_type              INT NOT NULL DEFAULT(1),    -- тип ёмкости
    analog_using_min_len        INT,    -- минимальная продолжительность расхода
    analog_is_using_calc        INT,    -- использовать ли расчётный расход топлива за период заправок/сливов
    analog_detect_inc           FLOAT8,  -- скорость увеличения уровня (топлива) для детектора заправки
    analog_detect_inc_min_diff  FLOAT8,  -- минимально учитываемый объём заправки
    analog_detect_inc_min_len   INT,    -- минимально учитываемая продолжительность заправки
    analog_inc_add_time_before  INT,    -- добавить время до заправки
    analog_inc_add_time_after   INT,    -- добавить время после заправки
    analog_detect_dec           FLOAT8,  -- скорость уменьшения уровня (топлива) для детектора слива
    analog_detect_dec_min_diff  FLOAT8,  -- минимально учитываемый объём слива
    analog_detect_dec_min_len   INT,    -- минимально учитываемая продолжительность слива
    analog_dec_add_time_before  INT,    -- добавить время до заправки
    analog_dec_add_time_after   INT,    -- добавить время после заправки
-- общее для гео и дискретных датчиков
    liquid_norm         FLOAT8,  -- норматив расхода рабочей жидкости/топлива

 */

