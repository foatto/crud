package foatto.mms.core_mms.sensor

import foatto.app.CoreSpringController
import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.FormColumnCaptionData
import foatto.core_server.app.server.FormColumnVisibleData
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
import foatto.sql.CoreAdvancedStatement

class mSensor : mAbstract() {

    lateinit var columnSensorType: ColumnRadioButton
        private set
    lateinit var columnCalibrationText: ColumnString
        private set

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //        //--- этот метод может быть запущен из "Модули системы", безо всяких parent data
        //        Integer objectID = hmParentData.get(  "mms_object"  );
        //        AutoConfig ac = autoID == null ? null : AutoConfig.getAutoConfig(  conn, userConfig, autoID  );

        val parentObjectID = hmParentData["mms_object"]

        //--- это "оборудование" ( для пользователей ) или полноценные "датчики" для монтажников?
        val isEquip = aliasConfig.alias == "mms_equip"

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_sensor"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnSensorName = ColumnString(tableName, "name", "name", STRING_COLUMN_WIDTH)

        val columnSensorSumGroup = ColumnString(tableName, "sum_group_name", "Группа для суммирования", STRING_COLUMN_WIDTH)
        columnSensorSumGroup.addCombo("")
        var rs = stm.executeQuery(
            " SELECT DISTINCT sum_group_name FROM $tableName WHERE object_id = $parentObjectID AND sum_group_name IS NOT NULL AND sum_group_name <> '' ORDER BY sum_group_name "
        )
        while(rs.next()) columnSensorSumGroup.addCombo(rs.getString(1).trim())
        rs.close()

        val columnSensorGroup = ColumnString(tableName, "group_name", "Группа датчиков", STRING_COLUMN_WIDTH)
        columnSensorGroup.addCombo("")
        rs = stm.executeQuery(
            " SELECT DISTINCT group_name FROM $tableName WHERE object_id = $parentObjectID AND group_name IS NOT NULL AND group_name <> '' ORDER BY group_name "
        )
        while(rs.next()) columnSensorGroup.addCombo(rs.getString(1).trim())
        rs.close()

        val columnSensorDescr = ColumnString(tableName, "descr", "Описание", STRING_COLUMN_WIDTH)
        columnSensorDescr.isRequired = true
        columnSensorDescr.isEditable = !isEquip

        val columnSensorPortNum = ColumnInt(tableName, "port_num", "Номер входа", 10)
        columnSensorPortNum.minValue = 0
        columnSensorPortNum.maxValue = 65535

        columnSensorType = ColumnRadioButton(tableName, "sensor_type", "Тип датчика")
        columnSensorType.formPinMode = FormPinMode.OFF
        columnSensorType.isEditable = !isEquip
        //--- расставляем типы датчиков в завимости от их "популярности" ( т.е. частоты использования )
        val hmSensorDescr = mutableMapOf<Int, String>()
        hmSensorDescr.putAll(SensorConfig.hmSensorDescr)
        rs = stm.executeQuery(" SELECT sensor_type, COUNT( * ) AS aaa FROM MMS_sensor GROUP BY sensor_type ORDER BY aaa DESC ")
        while(rs.next()) {
            val sensorType = rs.getInt(1)
            //--- теоретически возможны неправильные/несуществующие/устаревшие ( в т.ч. нулевые ) типы датчиков
            val sensorDescr = hmSensorDescr[sensorType] ?: continue

            columnSensorType.addChoice(sensorType, sensorDescr)
            hmSensorDescr.remove(sensorType)
            //--- самый популярный тип датчика установим в качестве типа по-умолчанию
            if(columnSensorType.defaultValue == null) columnSensorType.defaultValue = sensorType
        }
        rs.close()
        //--- добавить остатки непопулярных датчиков
        for((sensorType, descr) in hmSensorDescr) columnSensorType.addChoice(sensorType, descr)

        val columnSensorSerialNo = ColumnString(tableName, "serial_no", "Серийный номер", STRING_COLUMN_WIDTH)
        columnSensorSerialNo.formPinMode = FormPinMode.OFF
        columnSensorSerialNo.isEditable = !isEquip
        val columnSensorWorkBegin = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации")
        columnSensorWorkBegin.isEditable = !isEquip

        //----------------------------------------------------------------------------------------------------------------------------------------

        val arrGeoSensor = intArrayOf(SensorConfig.SENSOR_GEO)
        val arrWorkSensor = intArrayOf(SensorConfig.SENSOR_WORK)
        val arrWorkSignalSensor = intArrayOf(SensorConfig.SENSOR_WORK, SensorConfig.SENSOR_SIGNAL)
        val arrCounterSensor = intArrayOf( /*SensorConfig.SENSOR_LIQUID_USING,*/ SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW)
        val arrPhasedEnergoSensorOnly = intArrayOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL
        )
        val arrAnalogSensorOnly = intArrayOf(
            SensorConfig.SENSOR_LIQUID_FLOW_CALC,
            SensorConfig.SENSOR_LIQUID_LEVEL,
            SensorConfig.SENSOR_WEIGHT,
            SensorConfig.SENSOR_TURN,
            SensorConfig.SENSOR_PRESSURE,
            SensorConfig.SENSOR_TEMPERATURE,
            SensorConfig.SENSOR_VOLTAGE,
            SensorConfig.SENSOR_POWER,
            SensorConfig.SENSOR_DENSITY,
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL
        )
        //--- параметры analog_max_view ( Максимальная скорость ) и analog_max_limit ( Ограничение скорости [км/ч] ) используются также в настройках геодатчиков
        val arrAnalogAndGeoSensor = intArrayOf(
            SensorConfig.SENSOR_GEO,
            SensorConfig.SENSOR_LIQUID_FLOW_CALC,
            SensorConfig.SENSOR_LIQUID_LEVEL,
            SensorConfig.SENSOR_WEIGHT,
            SensorConfig.SENSOR_TURN,
            SensorConfig.SENSOR_PRESSURE,
            SensorConfig.SENSOR_TEMPERATURE,
            SensorConfig.SENSOR_VOLTAGE,
            SensorConfig.SENSOR_POWER,
            SensorConfig.SENSOR_DENSITY,
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL
        )
        val arrLLSensor = intArrayOf(SensorConfig.SENSOR_LIQUID_LEVEL)
        val arrGeoAndSummarySensor = intArrayOf(
            SensorConfig.SENSOR_GEO,
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR
        )

        //--- гео-датчики ( координат, скорости и пробега ) ----------------------------------------------------------------------------------------

        val columnMinMovingTime = ColumnInt(tableName, "min_moving_time", "Минимальное время движения [сек]", 10, 1)
        columnMinMovingTime.formPinMode = FormPinMode.OFF
        columnMinMovingTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnMinParkingTime = ColumnInt(tableName, "min_parking_time", "Минимальное время стоянки [сек]", 10, 300)
        columnMinParkingTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnMinOverSpeedTime = ColumnInt(tableName, "min_over_speed_time", "Минимальное время превышения скорости [сек]", 10, 60)
        columnMinOverSpeedTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnIsAbsoluteRun = ColumnBoolean(tableName, "is_absolute_run", "Абсолютный пробег", true)
        columnIsAbsoluteRun.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnSpeedRoundRule = ColumnComboBox(tableName, "speed_round_rule", "Правило округления скорости", SensorConfigGeo.SPEED_ROUND_RULE_STANDART)
        columnSpeedRoundRule.addChoice(SensorConfigGeo.SPEED_ROUND_RULE_LESS, "В меньшую сторону")
        columnSpeedRoundRule.addChoice(SensorConfigGeo.SPEED_ROUND_RULE_STANDART, "Стандартно")
        columnSpeedRoundRule.addChoice(SensorConfigGeo.SPEED_ROUND_RULE_GREATER, "В большую сторону")
        columnSpeedRoundRule.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnRunKoef = ColumnDouble(tableName, "run_koef", "Коэффициент учёта погрешности", 10, 3, 1.0)
        columnRunKoef.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnIsUsePos = ColumnBoolean(tableName, "is_use_pos", "Использовать местоположение", true)
        columnIsUsePos.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnIsUseSpeed = ColumnBoolean(tableName, "is_use_speed", "Использовать скорость", true)
        columnIsUseSpeed.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        val columnIsUseRun = ColumnBoolean(tableName, "is_use_run", "Использовать пробег", true)
        columnIsUseRun.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrGeoSensor))

        //--- дискретные датчики: время работы оборудования; сигналы -----------------------------------------------------------------------------

        val columnBoundValue = ColumnInt(tableName, "bound_value", "Граничное значение", 10, 0)
        columnBoundValue.formPinMode = FormPinMode.OFF
        columnBoundValue.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSignalSensor))

        val columnActiveValue = ColumnRadioButton(tableName, "active_value", "Рабочее состояние", 1)
        columnActiveValue.addChoice(0, "<= граничному значению")
        columnActiveValue.addChoice(1, ">  граничного значения")
        columnActiveValue.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSignalSensor))

        val columnMinOnTime = ColumnInt(tableName, "min_on_time", "Минимальное время работы [сек]", 10, 1)
        columnMinOnTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val columnMinOffTime = ColumnInt(tableName, "min_off_time", "Минимальное время простоя [сек]", 10, 1)
        columnMinOffTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val columnCalcInMoving = ColumnBoolean(tableName, "calc_in_moving", "Учитывать работу в движении", true)
        columnCalcInMoving.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val columnCalcInParking = ColumnBoolean(tableName, "calc_in_parking", "Учитывать работу на стоянках", true)
        columnCalcInParking.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val columnBegWorkValue = ColumnDouble(tableName, "beg_work_value", "Наработка на момент установки датчика [мото-час]", 10, 1, 0.0)
        columnBegWorkValue.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val selfLinkOnTableName = "MMS_device_command_1"
        val columnCommandIDOn = ColumnInt(selfLinkOnTableName, "id")
        columnCommandIDOn.selfLinkTableName = "MMS_device_command"
        val columnCommandOn = ColumnInt(tableName, "cmd_on_id", columnCommandIDOn)
        val columnCommandDescrOn = ColumnString(selfLinkOnTableName, "descr", "Команда включения", STRING_COLUMN_WIDTH)
        columnCommandDescrOn.selfLinkTableName = "MMS_device_command"
        columnCommandDescrOn.formPinMode = FormPinMode.OFF
        columnCommandDescrOn.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        columnCommandDescrOn.selectorAlias = "mms_device_command"
        columnCommandDescrOn.addSelectorColumn(columnCommandOn, columnCommandIDOn)
        columnCommandDescrOn.addSelectorColumn(columnCommandDescrOn)

        val selfLinkOffTableName = "MMS_device_command_2"
        val columnCommandIDOff = ColumnInt(selfLinkOffTableName, "id")
        columnCommandIDOff.selfLinkTableName = "MMS_device_command"
        val columnCommandOff = ColumnInt(tableName, "cmd_off_id", columnCommandIDOff)
        val columnCommandDescrOff = ColumnString(selfLinkOffTableName, "descr", "Команда отключения", STRING_COLUMN_WIDTH)
        columnCommandDescrOff.selfLinkTableName = "MMS_device_command"
        columnCommandDescrOff.formPinMode = FormPinMode.OFF
        columnCommandDescrOff.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        columnCommandDescrOff.selectorAlias = "mms_device_command"
        columnCommandDescrOff.addSelectorColumn(columnCommandOff, columnCommandIDOff)
        columnCommandDescrOff.addSelectorColumn(columnCommandDescrOff)

        val columnSignalOn = ColumnString(tableName, "signal_on", "Сигналы, разрешающие включение", STRING_COLUMN_WIDTH)
        columnSignalOn.formPinMode = FormPinMode.OFF
        columnSignalOn.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        val columnSignalOff = ColumnString(tableName, "signal_off", "Сигналы, разрешающие отключение", STRING_COLUMN_WIDTH)
        columnSignalOff.formPinMode = FormPinMode.OFF
        columnSignalOff.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrWorkSensor))

        //--- счётные датчики --------------------------------------------------------------------------------------------------

        val columnCountValueSensor = ColumnInt(tableName, "count_value_sensor", "Кол-во счетных импульсов", 10, 1)
        columnCountValueSensor.formPinMode = FormPinMode.OFF
        columnCountValueSensor.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrCounterSensor))

        val columnCountValueData = ColumnDouble(tableName, "count_value_data", "Кол-во измеряемой величины [л, кг, кВт*ч]", 10, 3, 0.0)
        columnCountValueData.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrCounterSensor))

        //        ColumnDouble columnFuelUsingMax = new ColumnDouble(  tableName, "liquid_using_max", "Максимально возможный расход [л/час]", 10, 0, 100.0  );
        //            columnFuelUsingMax.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingNormal = new ColumnDouble(  tableName, "liquid_using_normal", "Граница рабочего хода [л/час]", 10, 0, 10.0  );
        //            columnFuelUsingNormal.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingBorder = new ColumnDouble(  tableName, "liquid_using_border", "Граница холостого хода [л/час]", 10, 0, 1.0  );
        //            columnFuelUsingBorder.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );

        //--- аналоговые/измерительные датчики ---------------------------------------------------------------------------------

        val columnAnalogDim = ColumnString(tableName, "analog_dim", "Единица измерения", STRING_COLUMN_WIDTH)
        columnAnalogDim.formPinMode = FormPinMode.OFF
        columnAnalogDim.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))

        val columnAnalogMinView = ColumnDouble(tableName, "analog_min_view", "Минимальное отображаемое значение", 10, 3, 0.0)
        columnAnalogMinView.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))

        val columnAnalogMaxView = ColumnDouble(tableName, "analog_max_view", "-", 10, 3, 100.0)
        columnAnalogMaxView.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))
        columnAnalogMaxView.addFormCaption(FormColumnCaptionData(columnSensorType, "Максимальное отображаемое значение", arrAnalogSensorOnly))

        val columnAnalogMinLimit = ColumnDouble(tableName, "analog_min_limit", "Минимальное рабочее значение", 10, 3, 0.0)
        columnAnalogMinLimit.formPinMode = FormPinMode.OFF
        columnAnalogMinLimit.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))

        val columnAnalogMaxLimit = ColumnDouble(tableName, "analog_max_limit", "-", 10, 3, 100.0)
        columnAnalogMaxLimit.formPinMode = FormPinMode.OFF
        columnAnalogMaxLimit.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogAndGeoSensor))
        columnAnalogMaxLimit.addFormCaption(FormColumnCaptionData(columnSensorType, "Ограничение скорости [км/ч]", arrGeoSensor))
        columnAnalogMaxLimit.addFormCaption(FormColumnCaptionData(columnSensorType, "Максимальное рабочее значение", arrAnalogSensorOnly))

        //--- применяется только для показаний электросчётчиков

        val columnEnergoPhase = ColumnRadioButton(tableName, "energo_phase", "Фаза", 1)
        columnEnergoPhase.addChoice(1, "A")
        columnEnergoPhase.addChoice(2, "B")
        columnEnergoPhase.addChoice(3, "C")
        columnEnergoPhase.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrPhasedEnergoSensorOnly))

        //--- пока применяются только для датчиков уровня жидкости ( топлива )

        val columnAnalogUsingMinLen = ColumnInt(tableName, "analog_using_min_len", "-", 10, 1)
        columnAnalogUsingMinLen.formPinMode = FormPinMode.OFF
        columnAnalogUsingMinLen.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogUsingMinLen.addFormCaption(FormColumnCaptionData(columnSensorType, "Минимальная продолжительность расхода [сек]", arrLLSensor))
        val columnAnalogIsUsingCalc = ColumnBoolean(tableName, "analog_is_using_calc", "-", false)
        columnAnalogIsUsingCalc.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogIsUsingCalc.addFormCaption(FormColumnCaptionData(columnSensorType, "Использовать расчётный расход за время заправки/слива", arrLLSensor))

        val columnAnalogDetectInc = ColumnDouble(tableName, "analog_detect_inc", "-", 10, 0, 1.0)
        columnAnalogDetectInc.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectInc.addFormCaption(FormColumnCaptionData(columnSensorType, "Детектор заправки [л/час]", arrLLSensor))
        val columnAnalogDetectIncMinDiff = ColumnDouble(tableName, "analog_detect_inc_min_diff", "-", 10, 0, 0.0)
        columnAnalogDetectIncMinDiff.formPinMode = FormPinMode.ON
        columnAnalogDetectIncMinDiff.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectIncMinDiff.addFormCaption(FormColumnCaptionData(columnSensorType, "Минимальный объём заправки [л]", arrLLSensor))
        val columnAnalogDetectIncMinLen = ColumnInt(tableName, "analog_detect_inc_min_len", "-", 10, 1)
        columnAnalogDetectIncMinLen.formPinMode = FormPinMode.ON
        columnAnalogDetectIncMinLen.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectIncMinLen.addFormCaption(FormColumnCaptionData(columnSensorType, "Минимальная продолжительность заправки [сек]", arrLLSensor))
        val columnAnalogIncAddTimeBefore = ColumnInt(tableName, "analog_inc_add_time_before", "-", 10, 0)
        columnAnalogIncAddTimeBefore.formPinMode = FormPinMode.ON
        columnAnalogIncAddTimeBefore.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogIncAddTimeBefore.addFormCaption(FormColumnCaptionData(columnSensorType, "Добавить время к началу заправки [сек]", arrLLSensor))
        val columnAnalogIncAddTimeAfter = ColumnInt(tableName, "analog_inc_add_time_after", "-", 10, 0)
        columnAnalogIncAddTimeAfter.formPinMode = FormPinMode.ON
        columnAnalogIncAddTimeAfter.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogIncAddTimeAfter.addFormCaption(FormColumnCaptionData(columnSensorType, "Добавить время к концу заправки [сек]", arrLLSensor))

        val columnAnalogDetectDec = ColumnDouble(tableName, "analog_detect_dec", "-", 10, 0, 1.0)
        columnAnalogDetectDec.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectDec.addFormCaption(FormColumnCaptionData(columnSensorType, "Детектор слива [л/час]", arrLLSensor))
        val columnAnalogDetectDecMinDiff = ColumnDouble(tableName, "analog_detect_dec_min_diff", "-", 10, 0, 0.0)
        columnAnalogDetectDecMinDiff.formPinMode = FormPinMode.ON
        columnAnalogDetectDecMinDiff.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectDecMinDiff.addFormCaption(FormColumnCaptionData(columnSensorType, "Минимальный объём слива [л]", arrLLSensor))
        val columnAnalogDetectDecMinLen = ColumnInt(tableName, "analog_detect_dec_min_len", "-", 10, 1)
        columnAnalogDetectDecMinLen.formPinMode = FormPinMode.ON
        columnAnalogDetectDecMinLen.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDetectDecMinLen.addFormCaption(FormColumnCaptionData(columnSensorType, "Минимальная продолжительность слива [сек]", arrLLSensor))
        val columnAnalogDecAddTimeBefore = ColumnInt(tableName, "analog_dec_add_time_before", "-", 10, 0)
        columnAnalogDecAddTimeBefore.formPinMode = FormPinMode.ON
        columnAnalogDecAddTimeBefore.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDecAddTimeBefore.addFormCaption(FormColumnCaptionData(columnSensorType, "Добавить время к началу слива [сек]", arrLLSensor))
        val columnAnalogDecAddTimeAfter = ColumnInt(tableName, "analog_dec_add_time_after", "-", 10, 0)
        columnAnalogDecAddTimeAfter.formPinMode = FormPinMode.ON
        columnAnalogDecAddTimeAfter.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrLLSensor))
        columnAnalogDecAddTimeAfter.addFormCaption(FormColumnCaptionData(columnSensorType, "Добавить время к концу слива [сек]", arrLLSensor))

        val columnSmoothMethod = ColumnComboBox(tableName, "smooth_method", "Метод сглаживания", SensorConfig.SMOOTH_METOD_MEDIAN)
        columnSmoothMethod.addChoice(SensorConfig.SMOOTH_METOD_MEDIAN, "Медиана")
        columnSmoothMethod.addChoice(SensorConfig.SMOOTH_METOD_AVERAGE, "Среднее арифметическое")
        columnSmoothMethod.addChoice(SensorConfig.SMOOTH_METOD_AVERAGE_SQUARE, "Среднее квадратическое")
        columnSmoothMethod.addChoice(SensorConfig.SMOOTH_METOD_AVERAGE_GEOMETRIC, "Среднее геометрическое")
        columnSmoothMethod.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))

        val columnSmoothTime = ColumnInt(tableName, "smooth_time", "Период сглаживания [мин]", 10, 0)
        columnSmoothTime.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))

        columnCalibrationText = ColumnString(tableName, "_calibration_text", "Тарировка датчика", 20, STRING_COLUMN_WIDTH, 64000)
        columnCalibrationText.addFormVisible(FormColumnVisibleData(columnSensorType, true, arrAnalogSensorOnly))
        columnCalibrationText.isVirtual = true

        //--- общее для всех датчиков, кроме гео и суммарных датчиков --------------------------------------------------------------------------------

        val columnAnalogIgnoreMin = ColumnInt(tableName, "ignore_min_sensor", "Игнорировать показания датчика менее [ед.]", 10, 0)
        columnAnalogIgnoreMin.formPinMode = FormPinMode.OFF
        columnAnalogIgnoreMin.addFormVisible(FormColumnVisibleData(columnSensorType, false, arrGeoAndSummarySensor))

        val columnAnalogIgnoreMax = ColumnInt(tableName, "ignore_max_sensor", "Игнорировать показания датчика более [ед.]", 10, Integer.MAX_VALUE)
        columnAnalogIgnoreMax.addFormVisible(FormColumnVisibleData(columnSensorType, false, arrGeoAndSummarySensor))

        //--- общее для гео, дискретных, счётных и уровня жидкости датчиков ----------------------------------------------------------------------

        val columnLiquidName = ColumnString(tableName, "liquid_name", "Наименование топлива", STRING_COLUMN_WIDTH)
        columnLiquidName.formPinMode = FormPinMode.OFF
        columnLiquidName.addFormVisible(
            FormColumnVisibleData(
                columnSensorType, true, intArrayOf(
                    SensorConfig.SENSOR_GEO,
                    SensorConfig.SENSOR_WORK,
                    //SensorConfig.SENSOR_LIQUID_USING, - вместо него буду два новых датчика - SENSOR_MASS_FLOW и SENSOR_VOLUME_FLOW
                    SensorConfig.SENSOR_LIQUID_LEVEL,
                    SensorConfig.SENSOR_DENSITY,
                    SensorConfig.SENSOR_MASS_FLOW,
                    SensorConfig.SENSOR_VOLUME_FLOW,
                    SensorConfig.SENSOR_MASS_ACCUMULATED,
                    SensorConfig.SENSOR_VOLUME_ACCUMULATED
                )
            )
        )

        //--- общее для гео и дискретных датчиков ------------------------------------------------------------------------------------------------

        val columnLiquidNorm = ColumnDouble(tableName, "liquid_norm", "-", 10, 1, 0.0)
        columnLiquidNorm.formPinMode = FormPinMode.OFF
        columnLiquidNorm.addFormVisible(FormColumnVisibleData(columnSensorType, true, intArrayOf(SensorConfig.SENSOR_GEO, SensorConfig.SENSOR_WORK)))
        columnLiquidNorm.addFormCaption(FormColumnCaptionData(columnSensorType, "Норматив расхода топлива [л/100км]", intArrayOf(SensorConfig.SENSOR_GEO)))
        columnLiquidNorm.addFormCaption(FormColumnCaptionData(columnSensorType, "Норматив расхода топлива [л/час]", intArrayOf(SensorConfig.SENSOR_WORK)))

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnSensorName)
        alTableHiddenColumn.add(columnCommandOn)
        alTableHiddenColumn.add(columnCommandOff)

        if(isEquip) {
            alTableHiddenColumn.add(columnSensorSumGroup)
            alTableHiddenColumn.add(columnSensorGroup)
            addTableColumn(columnSensorDescr)
            alTableHiddenColumn.add(columnSensorPortNum)
            alTableHiddenColumn.add(columnSensorType)
            addTableColumn(columnSensorSerialNo)
            addTableColumn(columnSensorWorkBegin)
            addTableColumn(columnBegWorkValue)
        } else {
            alTableGroupColumn.add(columnSensorSumGroup)
            alTableGroupColumn.add(columnSensorGroup)
            //            addTableColumn(  columnSensorSumGroup  );
            //            addTableColumn(  columnSensorGroup  );

            addTableColumn(columnSensorDescr)
            addTableColumn(columnSensorPortNum)
            addTableColumn(columnSensorType)
            addTableColumn(columnSensorSerialNo)
            addTableColumn(columnSensorWorkBegin)
        }

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnSensorName)
        alFormHiddenColumn.add(columnCommandOn)
        alFormHiddenColumn.add(columnCommandOff)

        //----------------------------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(this, true, !isEquip, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, true, -1)

        //----------------------------------------------------------------------------------------------------------------------------------------

        if(isEquip) {
            alFormHiddenColumn.add(columnSensorSumGroup)
            alFormHiddenColumn.add(columnSensorGroup)
            alFormColumn.add(columnSensorDescr)
            alFormHiddenColumn.add(columnSensorPortNum)
            alFormColumn.add(columnSensorType)
            alFormColumn.add(columnSensorSerialNo)
            alFormColumn.add(columnSensorWorkBegin)
        } else {
            alFormColumn.add(columnSensorSumGroup)
            alFormColumn.add(columnSensorGroup)
            alFormColumn.add(columnSensorDescr)
            alFormColumn.add(columnSensorPortNum)
            alFormColumn.add(columnSensorType)
            alFormColumn.add(columnSensorSerialNo)
            alFormColumn.add(columnSensorWorkBegin)
        }

        alFormColumn.add(columnMinMovingTime)
        alFormColumn.add(columnMinParkingTime)
        alFormColumn.add(columnMinOverSpeedTime)
        alFormColumn.add(columnIsAbsoluteRun)
        alFormColumn.add(columnSpeedRoundRule)
        alFormColumn.add(columnRunKoef)
        alFormColumn.add(columnIsUsePos)
        alFormColumn.add(columnIsUseSpeed)
        alFormColumn.add(columnIsUseRun)

        if(isEquip) {
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

        alFormColumn.add(columnCountValueSensor)
        alFormColumn.add(columnCountValueData)

        alFormColumn.add(columnAnalogDim)
        alFormColumn.add(columnAnalogMinView)
        alFormColumn.add(columnAnalogMaxView)
        alFormColumn.add(columnAnalogMinLimit)
        alFormColumn.add(columnAnalogMaxLimit)

        alFormColumn.add(columnEnergoPhase)

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

        alFormColumn.add(columnSmoothMethod)
        alFormColumn.add(columnSmoothTime)

        if(isEquip) {
            alFormHiddenColumn.add(columnAnalogIgnoreMin)
            alFormHiddenColumn.add(columnAnalogIgnoreMax)
            alFormHiddenColumn.add(columnLiquidName)
            alFormHiddenColumn.add(columnLiquidNorm)
            alFormHiddenColumn.add(columnCalibrationText)
        } else {
            alFormColumn.add(columnAnalogIgnoreMin)
            alFormColumn.add(columnAnalogIgnoreMax)
            alFormColumn.add(columnLiquidName)
            alFormColumn.add(columnLiquidNorm)
            alFormColumn.add(columnCalibrationText)
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnSensorSumGroup)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnSensorGroup)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnSensorPortNum)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------------------------

        if(isEquip) {
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
