package foatto.mms.core_mms.sensor

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.ObjectSelector
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.sql.CoreAdvancedStatement

class mSensor : mAbstract() {

    lateinit var columnSensorType: ColumnComboBox   //ColumnRadioButton
        private set
    lateinit var columnCalibrationText: ColumnString
        private set

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val parentObjectID = hmParentData["mms_object"]

        //--- this is "equipment" (for users) or full "sensors" (for installers)?
        val isEquip = aliasConfig.alias == "mms_equip"

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_sensor"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnSensorName = ColumnString(tableName, "name", "name", STRING_COLUMN_WIDTH)

        val columnSensorGroup = ColumnString(tableName, "group_name", "Группа датчиков", STRING_COLUMN_WIDTH).apply {
            addCombo("")
            val rs = stm.executeQuery(
                " SELECT DISTINCT group_name FROM $tableName WHERE object_id = $parentObjectID AND group_name IS NOT NULL AND group_name <> '' ORDER BY group_name "
            )
            while (rs.next()) addCombo(rs.getString(1).trim())
            rs.close()
        }

        val columnSensorDescr = ColumnString(tableName, "descr", "Описание", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            isEditable = !isEquip
        }

        val columnSensorPortNum = ColumnInt(tableName, "port_num", "Номер входа", 10).apply {
            minValue = 0
            maxValue = 65535
        }

        columnSensorType = ColumnComboBox(tableName, "sensor_type", "Тип датчика").apply {
            formPinMode = FormPinMode.OFF
            isEditable = !isEquip

            //--- arrange the types of sensors depending on their "popularity" (ie frequency of use)
            val hmSensorDescr = mutableMapOf<Int, String>()
            hmSensorDescr.putAll(SensorConfig.hmSensorDescr)
            val rs = stm.executeQuery(" SELECT sensor_type, COUNT( * ) AS aaa FROM MMS_sensor GROUP BY sensor_type ORDER BY aaa DESC ")
            while (rs.next()) {
                val sensorType = rs.getInt(1)
                //--- theoretically, incorrect / non-existent / obsolete (including zero) types of sensors are possible
                val sensorDescr = hmSensorDescr[sensorType] ?: continue

                addChoice(sensorType, sensorDescr)
                hmSensorDescr.remove(sensorType)
                //--- the most popular sensor type is set as the default type
                if (defaultValue == null) defaultValue = sensorType
            }
            rs.close()
            //--- add leftovers from unpopular sensors
            for ((sensorType, descr) in hmSensorDescr) addChoice(sensorType, descr)
        }

        val columnSensorSerialNo = ColumnString(tableName, "serial_no", "Серийный номер", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
            isEditable = !isEquip
        }

        val columnSensorWorkBegin = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации").apply {
            isEditable = !isEquip
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        val signalSensorType = setOf(SensorConfig.SENSOR_SIGNAL)
        val geoSensorType = setOf(SensorConfig.SENSOR_GEO)
        val workSensorType = setOf(SensorConfig.SENSOR_WORK)

        val counterSensorTypes = setOf(SensorConfig.SENSOR_LIQUID_USING)

        val liquidSummarySensorTypes = setOf(
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
        )
        val energoSummarySensorTypes = setOf(
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR,
        )

        val liquidLevelSensorType = setOf(SensorConfig.SENSOR_LIQUID_LEVEL)
        val phasedEnergoSensorTypes = setOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL,
        )
        val analogSensorTypes = setOf(
            SensorConfig.SENSOR_LIQUID_FLOW_CALC,
            SensorConfig.SENSOR_WEIGHT,
            SensorConfig.SENSOR_TURN,
            SensorConfig.SENSOR_PRESSURE,
            SensorConfig.SENSOR_TEMPERATURE,
            SensorConfig.SENSOR_VOLTAGE,
            SensorConfig.SENSOR_POWER,
            SensorConfig.SENSOR_DENSITY,
            SensorConfig.SENSOR_MASS_FLOW,
            SensorConfig.SENSOR_VOLUME_FLOW,
        ) + liquidLevelSensorType + phasedEnergoSensorTypes

        //--- geo-sensors (coordinates, speed and mileage) ----------------------------------------------------------------------------------------

        val columnMinMovingTime = ColumnInt(tableName, "min_moving_time", "Минимальное время движения [сек]", 10, 1).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnMinParkingTime = ColumnInt(tableName, "min_parking_time", "Минимальное время стоянки [сек]", 10, 300).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnMinOverSpeedTime = ColumnInt(tableName, "min_over_speed_time", "Минимальное время превышения скорости [сек]", 10, 60).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnIsAbsoluteRun = ColumnBoolean(tableName, "is_absolute_run", "Абсолютный пробег", true).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnSpeedRoundRule = ColumnComboBox(tableName, "speed_round_rule", "Правило округления скорости", SensorConfigGeo.SPEED_ROUND_RULE_STANDART).apply {
            addChoice(SensorConfigGeo.SPEED_ROUND_RULE_LESS, "В меньшую сторону")
            addChoice(SensorConfigGeo.SPEED_ROUND_RULE_STANDART, "Стандартно")
            addChoice(SensorConfigGeo.SPEED_ROUND_RULE_GREATER, "В большую сторону")
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnRunKoef = ColumnDouble(tableName, "run_koef", "Коэффициент учёта погрешности", 10, 3, 1.0).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnIsUsePos = ColumnBoolean(tableName, "is_use_pos", "Использовать местоположение", true).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnIsUseSpeed = ColumnBoolean(tableName, "is_use_speed", "Использовать скорость", true).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        val columnIsUseRun = ColumnBoolean(tableName, "is_use_run", "Использовать пробег", true).apply {
            addFormVisible(columnSensorType, true, geoSensorType)
        }

        //--- discrete sensors: equipment operating time; -----------------------------------------------------------------------------

        val columnBoundValue = ColumnInt(tableName, "bound_value", "Граничное значение", 10, 0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, workSensorType + signalSensorType)
        }

        val columnActiveValue = ColumnRadioButton(tableName, "active_value", "Рабочее состояние", 1).apply {
            addChoice(0, "<= граничному значению")
            addChoice(1, ">  граничного значения")
            addFormVisible(columnSensorType, true, workSensorType + signalSensorType)
        }

        val columnMinOnTime = ColumnInt(tableName, "min_on_time", "Минимальное время работы [сек]", 10, 1).apply {
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val columnMinOffTime = ColumnInt(tableName, "min_off_time", "Минимальное время простоя [сек]", 10, 1).apply {
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val columnCalcInMoving = ColumnBoolean(tableName, "calc_in_moving", "Учитывать работу в движении", true).apply {
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val columnCalcInParking = ColumnBoolean(tableName, "calc_in_parking", "Учитывать работу на стоянках", true).apply {
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val columnBegWorkValue = ColumnDouble(tableName, "beg_work_value", "Наработка на момент установки датчика [мото-час]", 10, 1, 0.0).apply {
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val selfLinkOnTableName = "MMS_device_command_1"

        val columnCommandIDOn = ColumnInt(selfLinkOnTableName, "id").apply {
            selfLinkTableName = "MMS_device_command"
        }
        val columnCommandOn = ColumnInt(tableName, "cmd_on_id", columnCommandIDOn)
        val columnCommandDescrOn = ColumnString(selfLinkOnTableName, "descr", "Команда включения", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = "MMS_device_command"
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, workSensorType)

            selectorAlias = "mms_device_command"
            addSelectorColumn(columnCommandOn, columnCommandIDOn)
            addSelectorColumn(this)
        }

        val selfLinkOffTableName = "MMS_device_command_2"

        val columnCommandIDOff = ColumnInt(selfLinkOffTableName, "id").apply {
            selfLinkTableName = "MMS_device_command"
        }
        val columnCommandOff = ColumnInt(tableName, "cmd_off_id", columnCommandIDOff)
        val columnCommandDescrOff = ColumnString(selfLinkOffTableName, "descr", "Команда отключения", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = "MMS_device_command"
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, workSensorType)

            selectorAlias = "mms_device_command"
            addSelectorColumn(columnCommandOff, columnCommandIDOff)
            addSelectorColumn(this)
        }

        val columnSignalOn = ColumnString(tableName, "signal_on", "Сигналы, разрешающие включение", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, workSensorType)
        }

        val columnSignalOff = ColumnString(tableName, "signal_off", "Сигналы, разрешающие отключение", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, workSensorType)
        }

        //--- for smoothable sensors (counting and analog sensors)

        val columnSmoothMethod = ColumnComboBox(tableName, "smooth_method", "Метод сглаживания", SensorConfig.SMOOTH_METOD_MEDIAN).apply {
            addChoice(SensorConfig.SMOOTH_METOD_MEDIAN, "Медиана")
            addChoice(SensorConfig.SMOOTH_METOD_AVERAGE, "Среднее арифметическое")
            addChoice(SensorConfig.SMOOTH_METOD_AVERAGE_SQUARE, "Среднее квадратическое")
            addChoice(SensorConfig.SMOOTH_METOD_AVERAGE_GEOMETRIC, "Среднее геометрическое")
            addFormVisible(columnSensorType, false, signalSensorType + geoSensorType + workSensorType)
        }

        val columnSmoothTime = ColumnInt(tableName, "smooth_time", "Период сглаживания [мин]", 10, 0).apply {
            addFormVisible(columnSensorType, false, signalSensorType + geoSensorType + workSensorType)
        }

        //--- common for all sensors, except for geo and total sensors --------------------------------------------------------------------------------

        val columnIgnoreMin = ColumnDouble(
            aTableName = tableName,
            aFieldName = "ignore_min_sensor",
            aCaption = "Игнорировать показания датчика менее [ед.]",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = 0.0,
        ).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, false, geoSensorType)
        }

        val columnIgnoreMax = ColumnDouble(
            aTableName = tableName,
            aFieldName = "ignore_max_sensor",
            aCaption = "Игнорировать показания датчика более [ед.]",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = Integer.MAX_VALUE.toDouble(),
        ).apply {
            addFormVisible(columnSensorType, false, geoSensorType)
        }

        //--- common for geo sensors, discrete, counting, liquid level, density, total mass and total volume

        val columnLiquidName = ColumnString(tableName, "liquid_name", "Наименование топлива", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(
                columnSensorType,
                true,
                geoSensorType + workSensorType + counterSensorTypes + liquidSummarySensorTypes + liquidLevelSensorType
            )
        }

        //--- common for geo and discrete sensors ------------------------------------------------------------------------------------------------

        val columnLiquidNorm = ColumnDouble(tableName, "liquid_norm", "-", 10, 1, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, geoSensorType + workSensorType)
            addFormCaption(columnSensorType, "Норматив расхода топлива [л/100км]", geoSensorType)
            addFormCaption(columnSensorType, "Норматив расхода топлива [л/час]", workSensorType)
        }

        //--- counting sensors --------------------------------------------------------------------------------------------------

        //        ColumnDouble columnFuelUsingMax = new ColumnDouble(  tableName, "liquid_using_max", "Максимально возможный расход [л/час]", 10, 0, 100.0  );
        //            columnFuelUsingMax.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingNormal = new ColumnDouble(  tableName, "liquid_using_normal", "Граница рабочего хода [л/час]", 10, 0, 10.0  );
        //            columnFuelUsingNormal.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingBorder = new ColumnDouble(  tableName, "liquid_using_border", "Граница холостого хода [л/час]", 10, 0, 1.0  );
        //            columnFuelUsingBorder.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );

        //--- applies only to readings of electricity meters

        val columnEnergoPhase = ColumnRadioButton(tableName, "energo_phase", "Фаза", 0).apply {
            addChoice(0, "По сумме фаз")
            addChoice(1, "A")
            addChoice(2, "B")
            addChoice(3, "C")
            addFormVisible(columnSensorType, true, phasedEnergoSensorTypes)
        }

        //--- analog / measuring sensors ---------------------------------------------------------------------------------

        val columnAnalogMinView = ColumnDouble(tableName, "analog_min_view", "Минимальное отображаемое значение", 10, 3, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, analogSensorTypes)
        }

        val columnAnalogMaxView = ColumnDouble(tableName, "analog_max_view", "-", 10, 3, 100.0).apply {
            addFormVisible(columnSensorType, true, analogSensorTypes)
            addFormCaption(columnSensorType, "Максимальное отображаемое значение", analogSensorTypes)
        }

        val columnAnalogMinLimit = ColumnDouble(tableName, "analog_min_limit", "Минимальное рабочее значение", 10, 3, 0.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, analogSensorTypes)
        }

        //--- geo + analog / measuring sensors ---------------------------------------------------------------------------------

        val columnAnalogMaxLimit = ColumnDouble(tableName, "analog_max_limit", "-", 10, 3, 100.0).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, analogSensorTypes + geoSensorType)
            addFormCaption(columnSensorType, "Ограничение скорости [км/ч]", geoSensorType)
            addFormCaption(columnSensorType, "Максимальное рабочее значение", analogSensorTypes)
        }

        //--- while they are only used for liquid (fuel) level sensors

        val columnAnalogUsingMinLen = ColumnInt(tableName, "analog_using_min_len", "-", 10, 1).apply {
            formPinMode = FormPinMode.OFF
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Минимальная продолжительность расхода [сек]", liquidLevelSensorType)
        }
        val columnAnalogIsUsingCalc = ColumnBoolean(tableName, "analog_is_using_calc", "-", false).apply {
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Использовать расчётный расход за время заправки/слива", liquidLevelSensorType)
        }

        val columnAnalogDetectInc = ColumnDouble(tableName, "analog_detect_inc", "-", 10, 0, 1.0).apply {
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Детектор заправки [л/час]", liquidLevelSensorType)
        }
        val columnAnalogDetectIncMinDiff = ColumnDouble(tableName, "analog_detect_inc_min_diff", "-", 10, 0, 0.0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Минимальный объём заправки", liquidLevelSensorType)
        }
        val columnAnalogDetectIncMinLen = ColumnInt(tableName, "analog_detect_inc_min_len", "-", 10, 1).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Минимальная продолжительность заправки [сек]", liquidLevelSensorType)
        }
        val columnAnalogIncAddTimeBefore = ColumnInt(tableName, "analog_inc_add_time_before", "-", 10, 0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Добавить время к началу заправки [сек]", liquidLevelSensorType)
        }
        val columnAnalogIncAddTimeAfter = ColumnInt(tableName, "analog_inc_add_time_after", "-", 10, 0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Добавить время к концу заправки [сек]", liquidLevelSensorType)
        }

        val columnAnalogDetectDec = ColumnDouble(tableName, "analog_detect_dec", "-", 10, 0, 1.0).apply {
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Детектор слива [л/час]", liquidLevelSensorType)
        }
        val columnAnalogDetectDecMinDiff = ColumnDouble(tableName, "analog_detect_dec_min_diff", "-", 10, 0, 0.0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Минимальный объём слива", liquidLevelSensorType)
        }
        val columnAnalogDetectDecMinLen = ColumnInt(tableName, "analog_detect_dec_min_len", "-", 10, 1).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Минимальная продолжительность слива [сек]", liquidLevelSensorType)
        }
        val columnAnalogDecAddTimeBefore = ColumnInt(tableName, "analog_dec_add_time_before", "-", 10, 0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Добавить время к началу слива [сек]", liquidLevelSensorType)
        }
        val columnAnalogDecAddTimeAfter = ColumnInt(tableName, "analog_dec_add_time_after", "-", 10, 0).apply {
            formPinMode = FormPinMode.ON
            addFormVisible(columnSensorType, true, liquidLevelSensorType)
            addFormCaption(columnSensorType, "Добавить время к концу слива [сек]", liquidLevelSensorType)
        }

        //--- any calibrable sensors ---

        columnCalibrationText = ColumnString(tableName, "_calibration_text", "Тарировка датчика", 20, STRING_COLUMN_WIDTH, 64000).apply {
            addFormVisible(columnSensorType, false, signalSensorType + geoSensorType + workSensorType)
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnSensorName)
        alTableHiddenColumn.add(columnCommandOn)
        alTableHiddenColumn.add(columnCommandOff)

        alTableGroupColumn.add(columnSensorGroup)

        addTableColumn(columnSensorDescr)
        if (isEquip) {
            alTableHiddenColumn.add(columnSensorPortNum)
            alTableHiddenColumn.add(columnSensorType)
        } else {
            addTableColumn(columnSensorPortNum)
            addTableColumn(columnSensorType)
        }
        addTableColumn(columnSensorSerialNo)
        addTableColumn(columnSensorWorkBegin)
        if (isEquip) {
            addTableColumn(columnBegWorkValue)
        }

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnSensorName)
        alFormHiddenColumn.add(columnCommandOn)
        alFormHiddenColumn.add(columnCommandOff)

        //----------------------------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(this, true, !isEquip, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, true, -1)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnSensorGroup)
        alFormColumn.add(columnSensorDescr)
        if (isEquip) {
            alFormHiddenColumn.add(columnSensorPortNum)
        } else {
            alFormColumn.add(columnSensorPortNum)
        }
        alFormColumn.add(columnSensorType)
        alFormColumn.add(columnSensorSerialNo)
        alFormColumn.add(columnSensorWorkBegin)

        alFormColumn.add(columnMinMovingTime)
        alFormColumn.add(columnMinParkingTime)
        alFormColumn.add(columnMinOverSpeedTime)
        alFormColumn.add(columnIsAbsoluteRun)
        alFormColumn.add(columnSpeedRoundRule)
        alFormColumn.add(columnRunKoef)
        alFormColumn.add(columnIsUsePos)
        alFormColumn.add(columnIsUseSpeed)
        alFormColumn.add(columnIsUseRun)

        if (isEquip) {
            alFormHiddenColumn.add(columnBoundValue)
            alFormHiddenColumn.add(columnActiveValue)
            alFormHiddenColumn.add(columnMinOnTime)
            alFormHiddenColumn.add(columnMinOffTime)
            alFormHiddenColumn.add(columnCalcInMoving)
            alFormHiddenColumn.add(columnCalcInParking)
            alFormColumn.add(columnBegWorkValue)
            alFormHiddenColumn.add(columnCommandDescrOn)
            alFormHiddenColumn.add(columnCommandDescrOff)
            alFormHiddenColumn.add(columnSignalOn)
            alFormHiddenColumn.add(columnSignalOff)
        } else {
            alFormColumn.add(columnBoundValue)
            alFormColumn.add(columnActiveValue)
            alFormColumn.add(columnMinOnTime)
            alFormColumn.add(columnMinOffTime)
            alFormColumn.add(columnCalcInMoving)
            alFormColumn.add(columnCalcInParking)
            alFormColumn.add(columnBegWorkValue)
            alFormColumn.add(columnCommandDescrOn)
            alFormColumn.add(columnCommandDescrOff)
            alFormColumn.add(columnSignalOn)
            alFormColumn.add(columnSignalOff)
        }

        alFormColumn.add(columnSmoothMethod)
        alFormColumn.add(columnSmoothTime)

        if (isEquip) {
            alFormHiddenColumn.add(columnIgnoreMin)
            alFormHiddenColumn.add(columnIgnoreMax)
        } else {
            alFormColumn.add(columnIgnoreMin)
            alFormColumn.add(columnIgnoreMax)
        }

        if (isEquip) {
            alFormHiddenColumn.add(columnLiquidName)
            alFormHiddenColumn.add(columnLiquidNorm)
        } else {
            alFormColumn.add(columnLiquidName)
            alFormColumn.add(columnLiquidNorm)
        }

        alFormColumn.add(columnEnergoPhase)
        alFormColumn.add(columnAnalogMinView)
        alFormColumn.add(columnAnalogMaxView)
        alFormColumn.add(columnAnalogMinLimit)
        alFormColumn.add(columnAnalogMaxLimit)

        alFormColumn.add(columnAnalogUsingMinLen)
        alFormColumn.add(columnAnalogIsUsingCalc)
        alFormColumn.add(columnAnalogDetectInc)
        alFormColumn.add(columnAnalogDetectIncMinDiff)
        alFormColumn.add(columnAnalogDetectIncMinLen)
        alFormColumn.add(columnAnalogIncAddTimeBefore)
        alFormColumn.add(columnAnalogIncAddTimeAfter)
        alFormColumn.add(columnAnalogDetectDec)
        alFormColumn.add(columnAnalogDetectDecMinDiff)
        alFormColumn.add(columnAnalogDetectDecMinLen)
        alFormColumn.add(columnAnalogDecAddTimeBefore)
        alFormColumn.add(columnAnalogDecAddTimeAfter)

        if (isEquip) {
            alFormHiddenColumn.add(columnCalibrationText)
        } else {
            alFormColumn.add(columnCalibrationText)
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnSensorGroup)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnSensorPortNum)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------------------------

        if (isEquip) {
            alChildData.add(ChildData("mms_equip_service_shedule", columnID!!, true))
            alChildData.add(ChildData("mms_equip_service_history", columnID!!))
        } else {
            alChildData.add(ChildData("mms_sensor_calibration", columnID!!, true))
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_sensor_calibration", "sensor_id", DependData.DELETE))
        alDependData.add(DependData("MMS_equip_service_shedule", "equip_id", DependData.DELETE))
        alDependData.add(DependData("MMS_equip_service_history", "equip_id", DependData.DELETE))
    }
}
