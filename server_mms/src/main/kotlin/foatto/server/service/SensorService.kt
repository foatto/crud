package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormCellCaption
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getRandomInt
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.UserRelationEnum
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.SensorConfig
import foatto.server.model.SensorConfigCounter
import foatto.server.model.SensorConfigGeo
import foatto.server.model.SensorConfigLiquidLevel
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
class SensorService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
    private val objectRepository: ObjectRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        private const val FIELD_OBJECT_ID = "obj_id"

        // private const val FIELD_NAME = "name"
        private const val FIELD_GROUP = "group"
        private const val FIELD_DESCR = "descr"
        private const val FIELD_PORT_NUM = "portNum"
        private const val FIELD_SENSOR_TYPE = "sensorType"
        private const val FIELD_SERIAL_NO = "serialNo"
        private const val FIELD_USING_START_DATE = "usingStartDate"

        private const val FIELD_MIN_MOVING_TIME = "minMovingTime"
        private const val FIELD_MIN_PARKING_TIME = "minParkingTime"
        private const val FIELD_MIN_OVER_SPEED_TIME = "minOverSpeedTime"
        private const val FIELD_IS_ABSOLUTE_RUN = "isAbsoluteRun"
        private const val FIELD_SPEED_ROUND_RULE = "speedRoundRule"
        private const val FIELD_RUN_KOEF = "runKoef"
        private const val FIELD_IS_USE_POS = "isUsePos"
        private const val FIELD_IS_USE_SPEED = "isUseSpeed"
        private const val FIELD_IS_USE_RUN = "isUseRun"

        private const val FIELD_BOUND_VALUE = "boundValue"
        private const val FIELD_ACTIVE_VALUE = "activeValue"
        private const val FIELD_BEG_WORK_VALUE = "begWorkValue"

        //        private const val FIELD_CMD_ON_ID = "cmdOnId"
//        private const val FIELD_CMD_OFF_ID = "cmdOffId"
//        private const val FIELD_SIGNAL_ON = "signalOn"
//        private const val FIELD_SIGNAL_OFF = "signalOff"
        private const val FIELD_MIN_ON_TIME = "minOnTime"
        private const val FIELD_MIN_OFF_TIME = "minOffTime"

        private const val FIELD_SMOOTH_TIME = "smoothTime"
        private const val FIELD_MIN_IGNORE = "minIgnore"
        private const val FIELD_MAX_IGNORE = "maxIgnore"
        private const val FIELD_LIQUID_NAME = "liquidName"
        private const val FIELD_LIQUID_NORM = "liquidNorm"

        private const val FIELD_MIN_VIEW = "minView"
        private const val FIELD_MAX_VIEW = "maxView"
        private const val FIELD_MIN_LIMIT = "minLimit"
        private const val FIELD_MAX_LIMIT = "maxLimit"

        private const val FIELD_INDICATOR_DELIMITER_COUNT = "indicatorDelimiterCount"
        private const val FIELD_INDICATOR_MILTIPLICATOR = "indicatorMultiplicator"

        private const val FIELD_IS_ABSOLUTE_COUNT = "isAbsoluteCount"
        private const val FIELD_PHASE = "phase"
        private const val FIELD_IN_OUT_TYPE = "inOutType"

        private const val FIELD_CONTAINER_TYPE = "containerType"
        private const val FIELD_USING_MIN_LEN = "usingMinLen"
        private const val FIELD_IS_USING_CALC = "isUsingCalc"
        private const val FIELD_DETECT_INC_KOEF = "detectIncKoef"
        private const val FIELD_DETECT_INC_MIN_DIFF = "detectIncMinDiff"
        private const val FIELD_DETECT_INC_MIN_LEN = "detectIncMinLen"
        private const val FIELD_INC_ADD_TIME_BEFORE = "incAddTimeBefore"
        private const val FIELD_INC_ADD_TIME_AFTER = "incAddTimeAfter"
        private const val FIELD_DETECT_DEC_KOEF = "detectDecKoef"
        private const val FIELD_DETECT_DEC_MIN_DIFF = "detectDecMinDiff"
        private const val FIELD_DETECT_DEC_MIN_LEN = "detectDecMinLen"
        private const val FIELD_DEC_ADD_TIME_BEFORE = "decAddTimeBefore"
        private const val FIELD_DEC_ADD_TIME_AFTER = "decAddTimeAfter"
        private const val FIELD_SCHEME_X = "schemeX"
        private const val FIELD_SCHEME_Y = "schemeY"

        private const val FIELD_CALIBRATION = "_calibration" // псевдополе для калибровки/тарировки

        fun checkAndCreateAggTable(conn: CoreAdvancedConnection, sensorId: Int) {
            if (!checkAggTableIsExists(conn, sensorId)) {
                createAggTable(conn, sensorId)
            }
        }

        fun checkAndCreateAggTable(entityManager: EntityManager, sensorId: Int) {
            if (!checkAggTableIsExists(entityManager, sensorId)) {
                createAggTable(entityManager, sensorId)
            }
        }

        private fun checkAggTableIsExists(conn: CoreAdvancedConnection, sensorId: Int): Boolean {
            val rs = conn.executeQuery(
                """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public' 
                    AND tablename = 'mms_agg_$sensorId'
                """
            )
            val result = rs.next()
            rs.close()

            return result
        }

        private fun checkAggTableIsExists(entityManager: EntityManager, sensorId: Int): Boolean {
            var result = false

            queryNativeSql(
                entityManager,
                """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public' 
                    AND tablename = 'mms_agg_$sensorId'
                """
            ) { rs: ResultSet ->
                result = rs.next()
            }

            return result
        }

        private fun createAggTable(conn: CoreAdvancedConnection, sensorId: Int) {
            conn.executeUpdate(
                """
                    CREATE TABLE MMS_agg_$sensorId (
                        ontime_0        INT NOT NULL,
                        ontime_1        INT,         
                        type_0          INT,
                        value_0         FLOAT8,
                        value_1         FLOAT8,
                        value_2         FLOAT8,
                        value_3         FLOAT8
                    )
                """
            )
            conn.executeUpdate(" CREATE INDEX MMS_agg_${sensorId}_ontime_0 ON MMS_agg_$sensorId ( ontime_0 ) ")
            conn.executeUpdate(" CREATE INDEX MMS_agg_${sensorId}_ontime_1 ON MMS_agg_$sensorId ( ontime_1 ) ")
        }

        private fun createAggTable(entityManager: EntityManager, sensorId: Int) {
            executeNativeSql(
                entityManager,
                """
                    CREATE TABLE MMS_agg_$sensorId (
                        ontime_0        INT NOT NULL,
                        ontime_1        INT,         
                        type_0          INT,
                        value_0         FLOAT8,
                        value_1         FLOAT8,
                        value_2         FLOAT8,
                        value_3         FLOAT8
                    )
                """,
                " CREATE INDEX MMS_agg_${sensorId}_ontime_0 ON MMS_agg_$sensorId ( ontime_0 ) ",
                " CREATE INDEX MMS_agg_${sensorId}_ontime_1 ON MMS_agg_$sensorId ( ontime_1 ) ",
            )
        }

        fun deleteSensor(
            entityManager: EntityManager,
            sensorRepository: SensorRepository,
            sensorCalibrationRepository: SensorCalibrationRepository,
            sensorId: Int
        ) {
            sensorCalibrationRepository.deleteBySensorId(sensorId)
            sensorRepository.deleteById(sensorId)
            executeNativeSql(entityManager, " DROP TABLE MMS_agg_$sensorId ")
        }
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // sensorGroup

        alColumnInfo += FIELD_DESCR to "Описание"
        alColumnInfo += FIELD_PORT_NUM to "Номер входа"
        alColumnInfo += FIELD_SENSOR_TYPE to "Тип датчика"
        alColumnInfo += FIELD_SERIAL_NO to "Серийный номер"
        alColumnInfo += FIELD_USING_START_DATE to "Дата начала эксплуатации"

        return getTableColumnCaptionActions(
            action = action,
            alColumnInfo = alColumnInfo,
        )
    }

    override fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        alTableCell: MutableList<TableBaseCell>,
        alTableRowData: MutableList<TableRowData>,
        alPageButton: MutableList<Pair<AppAction?, String>>,
    ): Int? {

        var currentRowNo: Int? = null
        var row = 0

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_GROUP), Sort.Order(Sort.Direction.ASC, FIELD_PORT_NUM))
        val findText = action.findText?.trim() ?: ""

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId ?: return null
        } else {
            return null
        }
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId) ?: return null

        val page: Page<SensorEntity> = if (findText.isNotEmpty()) {
            sensorRepository.findByObjAndFilter(parentObjectEntity, findText, pageRequest)
        } else {
            sensorRepository.findByObj(parentObjectEntity, pageRequest)
        }

        fillTablePageButtons(action, page.totalPages, alPageButton)
        val sensorEntities = page.content

        var prevGroupName: String? = null
        for (sensorEntity in sensorEntities) {
            var col = 0

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = UserRelationEnum.NOBODY,
                userRoles = userConfig.roles
            )

            val groupName = sensorEntity.group ?: "-"
            if (prevGroupName != groupName) {
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = 6,
                    dataRow = row,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                alTableRowData += TableRowData()
                row++
            }
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "", backColorType = TableCellBackColorType.GROUP_0)

            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.descr ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.portNum?.toString() ?: "-")
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = sensorEntity.sensorType?.let { sensorType ->
                    SensorConfig.hmSensorDescr[sensorType] ?: "(неизвестный тип датчика)"
                } ?: "-",
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorEntity.serialNo ?: "-")
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = getDateEntityDMYString(sensorEntity.usingStartDate),
            )

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = sensorEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val alPopupData = mutableListOf<TablePopupData>()

            if (isFormEnabled) {
                alPopupData += TablePopupData(
                    action = formOpenAction,
                    text = "Открыть",
                    inNewTab = false,
                )
            }

//        if (isEquip) {
//            alChildData.add(ChildData("mms_equip_service_shedule", columnId, true))
//            alChildData.add(ChildData("mms_equip_service_history", columnId))
//        } else {
//            alChildData.add(ChildData("mms_sensor_calibration", columnId, true))
//        }
            if (checkAccessPermission(AppModuleMMS.SENSOR_CALIBRATION, userConfig.roles)) {
                alPopupData += TablePopupData(
                    action = AppAction(
                        type = ActionType.MODULE_TABLE,
                        module = AppModuleMMS.SENSOR_CALIBRATION,
                        parentModule = AppModuleMMS.SENSOR,
                        parentId = sensorEntity.id,
                    ),
                    text = appModuleConfigs[AppModuleMMS.SENSOR_CALIBRATION]?.caption ?: "(неизвестный тип модуля: ${AppModuleMMS.SENSOR_CALIBRATION})",
                    inNewTab = true,
                )
            }
            if (checkAccessPermission(AppModuleMMS.SENSOR_DATA, userConfig.roles)) {
                alPopupData += TablePopupData(
                    action = AppAction(
                        type = ActionType.MODULE_TABLE,
                        module = AppModuleMMS.SENSOR_DATA,
                        parentModule = AppModuleMMS.SENSOR,
                        parentId = sensorEntity.id,
                    ),
                    text = appModuleConfigs[AppModuleMMS.SENSOR_DATA]?.caption ?: "(неизвестный тип модуля: ${AppModuleMMS.SENSOR_DATA})",
                    inNewTab = true,
                )
            }

            alTableRowData += TableRowData(
                formAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                gotoAction = null,
                isGotoUrlInNewTab = true,
                alPopupData = alPopupData,
            )

            if (sensorEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val changeEnabled = id?.let { editEnabled } ?: addEnabled

//        //--- this is "equipment" (for users) or full "sensors" (for installers)?
//        val isEquip = aliasConfig.name == "mms_equip"

        val sensorEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            sensorRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId ?: 0
        } else {
            0
        }

        val sensorCalibrationInfo = id?.let {
            sensorCalibrationRepository.findBySensorIdOrderBySensorValue(id).joinToString("\n") { sensorCalibrationEntity ->
                "${sensorCalibrationEntity.sensorValue} = ${sensorCalibrationEntity.dataValue}"
            }
        } ?: ""

        val signalSensorType = setOf(SensorConfig.SENSOR_SIGNAL).map { it.toString() }.toSet()
        val geoSensorType = setOf(SensorConfig.SENSOR_GEO).map { it.toString() }.toSet()
        val workSensorType = setOf(SensorConfig.SENSOR_WORK).map { it.toString() }.toSet()

        val counterAndSummarySensorTypes = setOf(
            SensorConfig.SENSOR_LIQUID_USING,
            SensorConfig.SENSOR_MASS_ACCUMULATED,
            SensorConfig.SENSOR_VOLUME_ACCUMULATED,
        ).map { it.toString() }.toSet()

        val counterStateSensorType = setOf(SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE).map { it.toString() }.toSet()

        val energoSummarySensorTypes = setOf(
            SensorConfig.SENSOR_ENERGO_COUNT_AD,
            SensorConfig.SENSOR_ENERGO_COUNT_AR,
            SensorConfig.SENSOR_ENERGO_COUNT_RD,
            SensorConfig.SENSOR_ENERGO_COUNT_RR,
        ).map { it.toString() }.toSet()

        val liquidLevelSensorType = setOf(SensorConfig.SENSOR_LIQUID_LEVEL).map { it.toString() }.toSet()
        val phasedEnergoSensorTypes = setOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL,
        ).map { it.toString() }.toSet()
        val analogSensorTypes = (setOf(
            SensorConfig.SENSOR_WEIGHT,
            SensorConfig.SENSOR_TURN,
            SensorConfig.SENSOR_PRESSURE,
            SensorConfig.SENSOR_TEMPERATURE,
            SensorConfig.SENSOR_VOLTAGE,
            SensorConfig.SENSOR_POWER,
            SensorConfig.SENSOR_DENSITY,
            SensorConfig.SENSOR_MASS_FLOW,
            SensorConfig.SENSOR_VOLUME_FLOW,
            //!!! без учёта фазы?
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT,
            SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE,
        ) + liquidLevelSensorType + phasedEnergoSensorTypes).map { it.toString() }.toSet()

        //--- common data

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectId.toString(),
        )

        //!!! combo с возможностью ручного ввода
//" SELECT DISTINCT group_name FROM $columnTableName WHERE object_id = $parentObjectId AND group_name IS NOT NULL AND group_name <> '' ORDER BY group_name "
        formCells += FormSimpleCell(
            name = FIELD_GROUP,
            caption = "Группа",
            isEditable = changeEnabled,
            value = sensorEntity?.group ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_DESCR,
            caption = "Описание",
            isEditable = changeEnabled,
//            isEditable = !isEquip
            value = sensorEntity?.descr ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_PORT_NUM,
            caption = "Номер входа",
            isEditable = changeEnabled,
            value = sensorEntity?.portNum?.toString() ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_SENSOR_TYPE,
            caption = "Тип датчика",
            //--- нельзя менять тип уже созданному датчику, т.к. в его таблицу уже записаны данные,
            //--- могущими быть некорректными для нового типа датчика
            isEditable = changeEnabled && id == null,
//            isEditable = !isEquip
            value = sensorEntity?.sensorType?.toString() ?: "",
            //!!! заполнять с сортировкой по популярности
            values = SensorConfig.hmSensorDescr.map { (key, value) -> key.toString() to value },
        )
        /*
                    //--- arrange the types of sensors depending on their "popularity" (ie frequency of use)
                    val hmSensorDescr = mutableMapOf<Int, String>()
                    hmSensorDescr.putAll(SensorConfig.hmSensorDescr)
                    val rs = conn.executeQuery(" SELECT sensor_type, COUNT( sensor_type ) AS aaa FROM MMS_sensor GROUP BY sensor_type ORDER BY aaa DESC ")
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
                    hmSensorDescr.forEach { (sensorType, descr) ->
                        addChoice(sensorType, descr)
                    }
        */
        formCells += FormSimpleCell(
            name = FIELD_SERIAL_NO,
            caption = "Серийный номер",
            isEditable = changeEnabled,
//            formPinMode = FormPinMode.OFF
//            isEditable = !isEquip
            value = sensorEntity?.serialNo ?: "",
        )
        formCells += FormDateTimeCell(
            name = FIELD_USING_START_DATE,
            caption = "Дата начала эксплуатации",
            isEditable = changeEnabled,
//            isEditable = !isEquip
            mode = FormDateTimeCellMode.DMY,
            value = sensorEntity?.usingStartDate?.let { dt ->
                LocalDateTime(dt.ye ?: 2000, dt.mo ?: 1, dt.da ?: 1, 0, 0, 0).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            },
        )

        //--- geo-sensors (coordinates, speed and mileage) ----------------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_MOVING_TIME,
            caption = "Минимальное время движения [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.minMovingTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_MIN_PARKING_TIME,
            caption = "Минимальное время стоянки [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.minParkingTime?.toString() ?: "300",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_OVER_SPEED_TIME,
            caption = "Минимальное время превышения скорости [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.minOverSpeedTime?.toString() ?: "60",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_ABSOLUTE_RUN,
            caption = "Абсолютный пробег",
            isEditable = changeEnabled,
            value = sensorEntity?.isAbsoluteRun ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormComboCell(
            name = FIELD_SPEED_ROUND_RULE,
            caption = "Правило округления скорости",
            isEditable = changeEnabled,
            value = sensorEntity?.speedRoundRule?.toString() ?: SensorConfigGeo.SPEED_ROUND_RULE_STANDART.toString(),
            values = listOf(
                SensorConfigGeo.SPEED_ROUND_RULE_LESS.toString() to "В меньшую сторону",
                SensorConfigGeo.SPEED_ROUND_RULE_STANDART.toString() to "Стандартно",
                SensorConfigGeo.SPEED_ROUND_RULE_GREATER.toString() to "В большую сторону",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_RUN_KOEF,
            caption = "Коэффициент учёта погрешности",
            isEditable = changeEnabled,
            value = sensorEntity?.runKoef?.toString() ?: "1.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_USE_POS,
            caption = "Использовать местоположение",
            isEditable = changeEnabled,
            value = sensorEntity?.isUsePos ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_USE_SPEED,
            caption = "Использовать скорость",
            isEditable = changeEnabled,
            value = sensorEntity?.isUseSpeed ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_USE_RUN,
            caption = "Использовать пробег",
            isEditable = changeEnabled,
            value = sensorEntity?.isUseRun ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType,
            ),
        )

        //--- discrete sensors: equipment operating time; -----------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_BOUND_VALUE,
            caption = "Граничное значение",
            isEditable = changeEnabled,
            value = sensorEntity?.boundValue?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType + signalSensorType,
            ),
        )
        formCells += FormComboCell(
            name = FIELD_ACTIVE_VALUE,
            caption = "Рабочее состояние",
            isEditable = changeEnabled,
            value = sensorEntity?.activeValue?.toString() ?: "1",
            values = listOf(
                "0" to "если < граничного значения",
                "1" to "если > граничного значения",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType + signalSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_BEG_WORK_VALUE,
            caption = "",   // isEquip = "Наработка на момент установки датчика [мото-час]",
            isEditable = changeEnabled,
            value = sensorEntity?.begWorkValue?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType + signalSensorType,
            ),
        )

        //--- command and signals -----------------------------------------------------------------------------

//    val cmdOnId: Int?,
//    val cmdOffId: Int?,
//    val signalOn: String?,
//    val signalOff: String?,

        //--- discrete and counter sensors: equipment operating time; -----------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_ON_TIME,
            caption = "Минимальное время работы [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.minOnTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType + counterAndSummarySensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_OFF_TIME,
            caption = "Минимальное время простоя [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.minOffTime?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = workSensorType + counterAndSummarySensorTypes,
            ),
        )

        //--- for smoothable sensors (analog sensors only)

        formCells += FormSimpleCell(
            name = FIELD_SMOOTH_TIME,
            caption = "Период сглаживания [мин]",
            isEditable = changeEnabled,
            value = sensorEntity?.smoothTime?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        //--- common for all sensors, except for geo and total sensors --------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_IGNORE,
            caption = "Игнорировать показания датчика менее",
            isEditable = changeEnabled,
            value = sensorEntity?.minIgnore?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType + counterStateSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MAX_IGNORE,
            caption = "Игнорировать показания датчика более",
            isEditable = changeEnabled,
            value = sensorEntity?.maxIgnore?.toString() ?: "1000000.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = geoSensorType + counterStateSensorType,
            ),
        )

        //--- common for geo sensors, discrete, counting, liquid level, density, total mass and total volume

        formCells += FormSimpleCell(
            name = FIELD_LIQUID_NAME,
            caption = "Наименование топлива",
            isEditable = changeEnabled,
            value = sensorEntity?.liquidName ?: "",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType + workSensorType + counterAndSummarySensorTypes + liquidLevelSensorType,
            ),
        )

        //--- common for geo and discrete sensors ------------------------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_LIQUID_NORM,
            caption = "-",
            isEditable = changeEnabled,
            value = sensorEntity?.liquidNorm?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType + workSensorType,
            ),
            captions = FormCellCaption(
                name = FIELD_SENSOR_TYPE,
                captions = mapOf(
                    "Норматив расхода топлива [л/100км]" to geoSensorType,
                    "Норматив расхода топлива [л/час]" to workSensorType,
                ),
            ),
        )

        //--- applies only to (fuel) counter sensors

        formCells += FormBooleanCell(
            name = FIELD_IS_ABSOLUTE_COUNT,
            caption = "Накопительный счётчик",
            isEditable = changeEnabled,
            value = sensorEntity?.isAbsoluteCount ?: true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = counterAndSummarySensorTypes + energoSummarySensorTypes,
            ),
        )

        //--- applies for counter and summary volume/mass sensors

        formCells += FormComboCell(
            name = FIELD_IN_OUT_TYPE,
            caption = "Тип учёта",
            isEditable = changeEnabled,
            value = sensorEntity?.inOutType?.toString() ?: SensorConfigCounter.CALC_TYPE_OUT.toString(),
            values = listOf(
                SensorConfigCounter.CALC_TYPE_IN.toString() to "Входящий/заправочный счётчик",
                SensorConfigCounter.CALC_TYPE_OUT.toString() to "Исходящий/расходный счётчик",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = counterAndSummarySensorTypes,
            ),
        )

        //        ColumnDouble columnFuelUsingMax = new ColumnDouble(  tableName, "liquid_using_max", "Максимально возможный расход [л/час]", 10, 0, 100.0  );
        //            columnFuelUsingMax.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingNormal = new ColumnDouble(  tableName, "liquid_using_normal", "Граница рабочего хода [л/час]", 10, 0, 10.0  );
        //            columnFuelUsingNormal.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );
        //
        //        ColumnDouble columnFuelUsingBorder = new ColumnDouble(  tableName, "liquid_using_border", "Граница холостого хода [л/час]", 10, 0, 1.0  );
        //            columnFuelUsingBorder.addFormVisible(  new FormColumnVisibleData(  columnSensorType, true, new int[] { SensorConfig.SENSOR_LIQUID_USING }  )  );

        //--- applies only to readings of electricity meters

        formCells += FormComboCell(
            name = FIELD_PHASE,
            caption = "Фаза",
            isEditable = changeEnabled,
            value = sensorEntity?.phase?.toString() ?: "0",
            values = listOf(
                0.toString() to "По сумме фаз",
                1.toString() to "A",
                2.toString() to "B",
                3.toString() to "C",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = phasedEnergoSensorTypes,
            ),
        )

        //--- analog / measuring sensors ---------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MIN_VIEW,
            caption = "Минимальное отображаемое значение",
            isEditable = changeEnabled,
            value = sensorEntity?.minView?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MAX_VIEW,
            caption = "Максимальное отображаемое значение",
            isEditable = changeEnabled,
            value = sensorEntity?.maxView?.toString() ?: "100.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_MIN_LIMIT,
            caption = "Минимальное рабочее значение",
            isEditable = changeEnabled,
            value = sensorEntity?.minLimit?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        //--- geo + analog / measuring sensors ---------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_MAX_LIMIT,
            caption = "-",
            isEditable = changeEnabled,
            value = sensorEntity?.maxLimit?.toString() ?: "100.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = geoSensorType + analogSensorTypes,
            ),
            captions = FormCellCaption(
                name = FIELD_SENSOR_TYPE,
                captions = mapOf(
                    "Ограничение скорости [км/ч]" to geoSensorType,
                    "Максимальное рабочее значение" to analogSensorTypes,
                ),
            ),
        )

        //--- analog measuring sensors ---------------------------------------------------------------------------------

        formCells += FormSimpleCell(
            name = FIELD_INDICATOR_DELIMITER_COUNT,
            caption = "Шкала: количество делений",
            isEditable = changeEnabled,
            value = sensorEntity?.indicatorDelimiterCount?.toString() ?: "4",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_INDICATOR_MILTIPLICATOR,
            caption = "Шкала: множитель",
            isEditable = changeEnabled,
            value = sensorEntity?.indicatorMultiplicator?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = analogSensorTypes,
            ),
        )

        //--- while they are only used for liquid (fuel) level sensors

        formCells += FormComboCell(
            name = FIELD_CONTAINER_TYPE,
            caption = "Тип ёмкости",
            isEditable = changeEnabled,
            value = sensorEntity?.containerType?.toString() ?: SensorConfigLiquidLevel.CONTAINER_TYPE_WORK.toString(),
            values = listOf(
                SensorConfigLiquidLevel.CONTAINER_TYPE_MAIN.toString() to "Основная ёмкость",
                SensorConfigLiquidLevel.CONTAINER_TYPE_WORK.toString() to "Рабочая/расходная ёмкость",
            ),
            asRadioButtons = true,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_USING_MIN_LEN,
            caption = "Минимальная продолжительность расхода [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.usingMinLen?.toString() ?: "1",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_USING_CALC,
            caption = "Использовать расчётный расход за время заправки/слива",
            isEditable = changeEnabled,
            value = sensorEntity?.isUsingCalc ?: false,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_DETECT_INC_KOEF,
            caption = "Детектор заправки [л/час]",
            isEditable = changeEnabled,
            value = sensorEntity?.detectIncKoef?.toString() ?: "1.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DETECT_INC_MIN_DIFF,
            caption = "Минимальный объём заправки",
            isEditable = changeEnabled,
            value = sensorEntity?.detectIncMinDiff?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DETECT_INC_MIN_LEN,
            caption = "Минимальная продолжительность заправки [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.detectIncMinLen?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_INC_ADD_TIME_BEFORE,
            caption = "Добавить время к началу заправки [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.incAddTimeBefore?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_INC_ADD_TIME_AFTER,
            caption = "Добавить время к концу заправки [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.incAddTimeAfter?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_DETECT_DEC_KOEF,
            caption = "Детектор слива [л/час]",
            isEditable = changeEnabled,
            value = sensorEntity?.detectDecKoef?.toString() ?: "1.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DETECT_DEC_MIN_DIFF,
            caption = "Минимальный объём слива",
            isEditable = changeEnabled,
            value = sensorEntity?.detectDecMinDiff?.toString() ?: "0.0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DETECT_DEC_MIN_LEN,
            caption = "Минимальная продолжительность слива [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.detectDecMinLen?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DEC_ADD_TIME_BEFORE,
            caption = "Добавить время к началу слива [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.decAddTimeBefore?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_DEC_ADD_TIME_AFTER,
            caption = "Добавить время к концу слива [сек]",
            isEditable = changeEnabled,
            value = sensorEntity?.decAddTimeAfter?.toString() ?: "0",
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = true,
                values = liquidLevelSensorType,
            ),
        )

        formCells += FormSimpleCell(
            name = FIELD_SCHEME_X,
            caption = "",
            isEditable = false,
            value = sensorEntity?.schemeX?.toString() ?: "0",
        )
        formCells += FormSimpleCell(
            name = FIELD_SCHEME_Y,
            caption = "",
            isEditable = false,
            value = sensorEntity?.schemeY?.toString() ?: "0",
        )

        formCells += FormSimpleCell(
            name = FIELD_CALIBRATION,
            caption = "Тарировка датчика",
            isEditable = changeEnabled,
            value = sensorCalibrationInfo,
            rows = 20,
            visibility = FormCellVisibility(
                name = FIELD_SENSOR_TYPE,
                state = false,
                values = signalSensorType + geoSensorType + workSensorType + counterStateSensorType,
            ),
        )

        return formCells
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: 0
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)!!

        val descr = formActionData[FIELD_DESCR]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to "Не введёно описание"))
        if (descr.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to "Не введёно описание"))
        }
        if (sensorRepository.findByObjAndDescr(parentObjectEntity, descr).any { oe -> oe.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_DESCR to "Такое описание уже существует"))
        }

        val portNum = formActionData[FIELD_PORT_NUM]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PORT_NUM to "Не введён номер входа"))
        if (portNum < 0 || portNum > 65535) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PORT_NUM to "Номер входа должен быть в диапазоне от 0 до 65535"))
        }

        val usingStartDate = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), formActionData[FIELD_USING_START_DATE]?.dateTimeValue ?: 0)

        val recordId = id ?: getNextId { nextId -> sensorRepository.existsById(nextId) }
        val sensorEntity = SensorEntity(
            id = recordId,
            obj = parentObjectEntity,
            name = "",
            group = formActionData[FIELD_GROUP]?.stringValue ?: "",
            descr = descr,
            portNum = portNum,
            sensorType = formActionData[FIELD_SENSOR_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfig.SENSOR_GEO,
            serialNo = formActionData[FIELD_SERIAL_NO]?.stringValue ?: "",
            usingStartDate = DateEntity(
                ye = usingStartDate[0],
                mo = usingStartDate[1],
                da = usingStartDate[2],
            ),
            minMovingTime = formActionData[FIELD_MIN_MOVING_TIME]?.stringValue?.toIntOrNull() ?: 1,
            minParkingTime = formActionData[FIELD_MIN_PARKING_TIME]?.stringValue?.toIntOrNull() ?: 300,
            minOverSpeedTime = formActionData[FIELD_MIN_OVER_SPEED_TIME]?.stringValue?.toIntOrNull() ?: 60,
            isAbsoluteRun = formActionData[FIELD_IS_ABSOLUTE_RUN]?.booleanValue ?: true,
            speedRoundRule = formActionData[FIELD_SPEED_ROUND_RULE]?.stringValue?.toIntOrNull() ?: SensorConfigGeo.SPEED_ROUND_RULE_STANDART,
            runKoef = formActionData[FIELD_RUN_KOEF]?.stringValue?.toDoubleOrNull() ?: 1.0,
            isUsePos = formActionData[FIELD_IS_USE_POS]?.booleanValue ?: true,
            isUseSpeed = formActionData[FIELD_IS_USE_SPEED]?.booleanValue ?: true,
            isUseRun = formActionData[FIELD_IS_USE_RUN]?.booleanValue ?: true,
            boundValue = formActionData[FIELD_BOUND_VALUE]?.stringValue?.toIntOrNull() ?: 0,
            activeValue = formActionData[FIELD_ACTIVE_VALUE]?.stringValue?.toIntOrNull() ?: 1,
            begWorkValue = formActionData[FIELD_BEG_WORK_VALUE]?.stringValue?.toDoubleOrNull() ?: 0.0,
            minOnTime = formActionData[FIELD_MIN_ON_TIME]?.stringValue?.toIntOrNull() ?: 1,
            minOffTime = formActionData[FIELD_MIN_OFF_TIME]?.stringValue?.toIntOrNull() ?: 1,
            smoothTime = formActionData[FIELD_SMOOTH_TIME]?.stringValue?.toIntOrNull() ?: 0,
            minIgnore = formActionData[FIELD_MIN_IGNORE]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxIgnore = formActionData[FIELD_MAX_IGNORE]?.stringValue?.toDoubleOrNull() ?: 0.0,
            liquidName = formActionData[FIELD_LIQUID_NAME]?.stringValue ?: "",
            liquidNorm = formActionData[FIELD_LIQUID_NORM]?.stringValue?.toDoubleOrNull() ?: 0.0,
            minView = formActionData[FIELD_MIN_VIEW]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxView = formActionData[FIELD_MAX_VIEW]?.stringValue?.toDoubleOrNull() ?: 100.0,
            minLimit = formActionData[FIELD_MIN_LIMIT]?.stringValue?.toDoubleOrNull() ?: 0.0,
            maxLimit = formActionData[FIELD_MAX_LIMIT]?.stringValue?.toDoubleOrNull() ?: 100.0,
            indicatorDelimiterCount = formActionData[FIELD_INDICATOR_DELIMITER_COUNT]?.stringValue?.toIntOrNull() ?: 4,
            indicatorMultiplicator = formActionData[FIELD_INDICATOR_MILTIPLICATOR]?.stringValue?.toDoubleOrNull() ?: 1.0,
            isAbsoluteCount = formActionData[FIELD_IS_ABSOLUTE_COUNT]?.booleanValue ?: true,
            phase = formActionData[FIELD_PHASE]?.stringValue?.toIntOrNull() ?: 0,
            inOutType = formActionData[FIELD_IN_OUT_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfigCounter.CALC_TYPE_OUT,
            containerType = formActionData[FIELD_CONTAINER_TYPE]?.stringValue?.toIntOrNull() ?: SensorConfigLiquidLevel.CONTAINER_TYPE_WORK,
            usingMinLen = formActionData[FIELD_USING_MIN_LEN]?.stringValue?.toIntOrNull() ?: 1,
            isUsingCalc = formActionData[FIELD_IS_USING_CALC]?.booleanValue ?: false,
            detectIncKoef = formActionData[FIELD_DETECT_INC_KOEF]?.stringValue?.toDoubleOrNull() ?: 1.0,
            detectIncMinDiff = formActionData[FIELD_DETECT_INC_MIN_DIFF]?.stringValue?.toDoubleOrNull() ?: 0.0,
            detectIncMinLen = formActionData[FIELD_DETECT_INC_MIN_LEN]?.stringValue?.toIntOrNull() ?: 0,
            incAddTimeBefore = formActionData[FIELD_INC_ADD_TIME_BEFORE]?.stringValue?.toIntOrNull() ?: 0,
            incAddTimeAfter = formActionData[FIELD_INC_ADD_TIME_AFTER]?.stringValue?.toIntOrNull() ?: 0,
            detectDecKoef = formActionData[FIELD_DETECT_DEC_KOEF]?.stringValue?.toDoubleOrNull() ?: 1.0,
            detectDecMinDiff = formActionData[FIELD_DETECT_DEC_MIN_DIFF]?.stringValue?.toDoubleOrNull() ?: 0.0,
            detectDecMinLen = formActionData[FIELD_DETECT_DEC_MIN_LEN]?.stringValue?.toIntOrNull() ?: 0,
            decAddTimeBefore = formActionData[FIELD_DEC_ADD_TIME_BEFORE]?.stringValue?.toIntOrNull() ?: 0,
            decAddTimeAfter = formActionData[FIELD_DEC_ADD_TIME_AFTER]?.stringValue?.toIntOrNull() ?: 0,
            schemeX = formActionData[FIELD_SCHEME_X]?.stringValue?.toIntOrNull() ?: 0,
            schemeY = formActionData[FIELD_SCHEME_Y]?.stringValue?.toIntOrNull() ?: 0,
            //!!! для совместимости со старой версией
            cmdOnId = 0,
            cmdOffId = 0,
            signalOn = "",
            signalOff = "",
            smoothMethod = 0,
        )
        sensorRepository.saveAndFlush(sensorEntity)

        sensorCalibrationRepository.deleteBySensorId(recordId)
        val calibrationText = formActionData[FIELD_CALIBRATION]?.stringValue ?: ""
        calibrationText.split('\n').forEach { calibrationPair ->
            val alSensorData = calibrationPair.split('=')
            if (alSensorData.size == 2) {
                val sensorValue = alSensorData[0].trim().toDoubleOrNull()
                val dataValue = alSensorData[1].trim().toDoubleOrNull()

                if (sensorValue != null && dataValue != null) {
                    val sensorCalibrationEntity = SensorCalibrationEntity(
                        id = getNextSensorCalibrationId(),
                        sensor = sensorEntity,
                        sensorValue = sensorValue,
                        dataValue = dataValue,
                    )
                    sensorCalibrationRepository.save(sensorCalibrationEntity)
                }
            }
        }
        sensorCalibrationRepository.flush()

        checkAndCreateAggTable(entityManager, recordId)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    private fun getNextSensorCalibrationId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (sensorCalibrationRepository.existsById(nextId)) {
                continue
            }
            return nextId
        }
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)
        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, UserRelationEnum.NOBODY, userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, UserRelationEnum.NOBODY, userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        deleteSensor(
            entityManager = entityManager,
            sensorRepository = sensorRepository,
            sensorCalibrationRepository = sensorCalibrationRepository,
            sensorId = id,
        )
//        alDependData.add(DependData("MMS_equip_service_shedule", "equip_id", DependData.DELETE))
//        alDependData.add(DependData("MMS_equip_service_history", "equip_id", DependData.DELETE))

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

}

/*
    //--- "оборудование" нельзя добавить, оно является другим отображением "датчика работы оборудования"
    override fun isAddEnabled(): Boolean = aliasConfig.name != "mms_equip"

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        var s = super.addSQLWhere(hsTableRenameList)
        if (aliasConfig.name == "mms_equip") {
            s += " AND ${renameTableName(hsTableRenameList, model.modelTableName)}." +
                "${(model as mSensor).columnSensorType.getFieldName()} = ${SensorConfig.SENSOR_WORK} "
        }
        return s
    }
 */