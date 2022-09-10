package foatto.ts.core_ts.sensor

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectSelector
import foatto.ts.core_ts.sensor.config.SensorConfig

class mSensor : mAbstract() {

    lateinit var columnSensorType: ColumnComboBox
        private set
    lateinit var columnCalibrationText: ColumnString
        private set

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val parentObjectId = hmParentData["ts_object"]

        //----------------------------------------------------------------------------------------------------------------------------------------

        modelTableName = "TS_sensor"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnSensorName = ColumnString(modelTableName, "name", "name", STRING_COLUMN_WIDTH)

        val columnSensorGroup = ColumnString(modelTableName, "group_name", "Группа датчиков", STRING_COLUMN_WIDTH).apply {
            addCombo("")
            val rs = conn.executeQuery(
                " SELECT DISTINCT group_name FROM $columnTableName WHERE object_id = $parentObjectId AND group_name IS NOT NULL AND group_name <> '' ORDER BY group_name "
            )
            while (rs.next()) {
                addCombo(rs.getString(1).trim())
            }
            rs.close()
        }

        val columnSensorDescr = ColumnString(modelTableName, "descr", "Описание", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        val columnSensorPortNum = ColumnInt(modelTableName, "port_num", "Номер входа", 10).apply {
            minValue = 0
            maxValue = 65535
        }

        columnSensorType = ColumnComboBox(modelTableName, "sensor_type", "Тип датчика").apply {
            formPinMode = FormPinMode.OFF

            //--- arrange the types of sensors depending on their "popularity" (ie frequency of use)
            val hmSensorDescr = mutableMapOf<Int, String>()
            hmSensorDescr.putAll(SensorConfig.hmSensorDescr)
            val rs = conn.executeQuery(
                """
                     SELECT sensor_type, COUNT( * ) AS aaa 
                     FROM TS_sensor 
                     GROUP BY sensor_type 
                     ORDER BY aaa DESC 
                """
            )
            while (rs.next()) {
                val sensorType = rs.getInt(1)
                //--- theoretically, incorrect / non-existent / obsolete (including zero) types of sensors are possible
                val sensorDescr = hmSensorDescr[sensorType] ?: continue

                addChoice(sensorType, sensorDescr)
                hmSensorDescr.remove(sensorType)
                //--- the most popular sensor type is set as the default type
                if (defaultValue == null) {
                    defaultValue = sensorType
                }
            }
            rs.close()

            //--- add leftovers from unpopular sensors
            hmSensorDescr.forEach { (sensorType, descr) ->
                addChoice(sensorType, descr)
            }
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        //--- for smoothable sensors (counting and analog sensors)

        val columnSmoothMethod = ColumnComboBox(modelTableName, "smooth_method", "Метод сглаживания", SensorConfig.SMOOTH_METOD_MEDIAN).apply {
            addChoice(SensorConfig.SMOOTH_METOD_MEDIAN, "Медиана")
            addChoice(SensorConfig.SMOOTH_METOD_AVERAGE, "Среднее арифметическое")
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnSmoothTime = ColumnInt(modelTableName, "smooth_time", "Период сглаживания [мин]", 10, 0).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        //--- common for all sensors, except for geo and total sensors --------------------------------------------------------------------------------

        val columnIgnoreMin = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "ignore_min_sensor",
            aCaption = "Игнорировать показания датчика менее [ед.]",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = 0.0,
        ).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnIgnoreMax = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "ignore_max_sensor",
            aCaption = "Игнорировать показания датчика более [ед.]",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = Integer.MAX_VALUE.toDouble(),
        ).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        //--- analog / measuring sensors ---------------------------------------------------------------------------------

        val columnAnalogMinView = ColumnDouble(modelTableName, "analog_min_view", "Минимальное отображаемое значение на графике", 10, 3, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnAnalogMaxView = ColumnDouble(modelTableName, "analog_max_view", "Максимальное отображаемое значение на графике", 10, 3, 100.0).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnAnalogMinLimit = ColumnDouble(modelTableName, "analog_min_limit", "Минимальное рабочее значение на графике", 10, 3, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnAnalogMaxLimit = ColumnDouble(modelTableName, "analog_max_limit", "Максимальное рабочее значение на графике", 10, 3, 100.0).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnStateMinView = ColumnDouble(modelTableName, "state_min_view", "Минимальное отображаемое значение на шкале", 10, 3, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        val columnStateMaxView = ColumnDouble(modelTableName, "state_max_view", "Максимальное отображаемое значение на шкале", 10, 3, 100.0).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
        }

        //--- any calibrable sensors ---

        columnCalibrationText = ColumnString(modelTableName, "_calibration_text", "Тарировка датчика", 20, STRING_COLUMN_WIDTH, 64000).apply {
            addFormVisible(columnSensorType, false, setOf(SensorConfig.SENSOR_STATE))
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnSensorName)

        alTableGroupColumn.add(columnSensorGroup)

        addTableColumn(columnSensorDescr)
        addTableColumn(columnSensorPortNum)
        addTableColumn(columnSensorType)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnSensorName)

        //----------------------------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = true,
            addedStaticColumnCount = -1
        )

        //----------------------------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnSensorGroup)
        alFormColumn.add(columnSensorDescr)
        alFormColumn.add(columnSensorPortNum)
        alFormColumn.add(columnSensorType)

        alFormColumn.add(columnSmoothMethod)
        alFormColumn.add(columnSmoothTime)

        alFormColumn.add(columnIgnoreMin)
        alFormColumn.add(columnIgnoreMax)

        alFormColumn.add(columnAnalogMinView)
        alFormColumn.add(columnAnalogMaxView)

        alFormColumn.add(columnAnalogMinLimit)
        alFormColumn.add(columnAnalogMaxLimit)

        alFormColumn.add(columnStateMinView)
        alFormColumn.add(columnStateMaxView)

        alFormColumn.add(columnCalibrationText)

        //----------------------------------------------------------------------------------------------------------------------------------------

        addTableSort(columnSensorGroup, true)
        addTableSort(columnSensorPortNum, true)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("ts_sensor_calibration", columnId, true))

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("TS_sensor_calibration", "sensor_id", DependData.DELETE))
    }
}
