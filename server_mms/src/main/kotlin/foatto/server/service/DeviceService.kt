package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
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
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.ds.CoreTelematicFunction
import foatto.server.ds.MMSTelematicFunction
import foatto.server.ds.PortNumbers
import foatto.server.entity.DateEntity
import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import foatto.server.entity.SensorEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.SensorConfig
import foatto.server.model.SensorConfigCounter
import foatto.server.model.SensorConfigGeo
import foatto.server.model.SensorConfigLiquidLevel
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DeviceRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class DeviceService(
    private val entityManager: EntityManager,
    private val deviceRepository: DeviceRepository,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        //        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TYPE = "type"
        private const val FIELD_INDEX = "index"
        private const val FIELD_SERIAL_NO = "serialNo"

        //        private const val FIELD_OBJECT = "obj"
        private const val FIELD_CELL_IMEI = "cellImei"
        private const val FIELD_CELL_OWNER = "cellOwner"
        private const val FIELD_CELL_NUMBER = "cellNumber"
        private const val FIELD_CELL_ICC = "cellIcc"
        private const val FIELD_CELL_OPERATOR = "cellOperator"
        private const val FIELD_CELL_IMEI_2 = "cellImei2"
        private const val FIELD_CELL_OWNER_2 = "cellOwner2"
        private const val FIELD_CELL_NUMBER_2 = "cellNumber2"
        private const val FIELD_CELL_ICC_2 = "cellIcc2"
        private const val FIELD_CELL_OPERATOR_2 = "cellOperator2"
        private const val FIELD_FW_VERSION = "fwVersion"
        private const val FIELD_LAST_SESSION_TIME = "lastSessionTime"
        private const val FIELD_LAST_SESSION_STATUS = "lastSessionStatus"
        private const val FIELD_LAST_SESSION_ERROR = "lastSessionError"
        private const val FIELD_USING_START_DATE = "usingStartDate"

        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_OBJECT_NAME = "obj.name"    // отдельное определение для сортировки в Hibernate
        private const val FIELD_OBJECT_MODEL = "obj.model"  // отдельное определение для сортировки в Hibernate

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора

        private const val FIELD_COPY_SENSORS = "_copySensors"

        private const val SENSOR_CREATE_ENABLED = "_create_enabled"
        private const val SENSOR_GROUP_NAME = "_group_name_"
        private const val SENSOR_DESCR_PREFIX = "_descr_prefix_"
        private const val SENSOR_DESCR_POSTFIX = "_descr_postfix_"

        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private const val MAX_DEVICE_COUNT_PER_OBJECT = 65

        private const val CELL_OWNER_UNKNOWN = 0
        private const val CELL_OWNER_PLA = 1
        private const val CELL_OWNER_CLIENT = 2

        private val deviceTypes = mapOf(
            MMSTelematicFunction.DEVICE_TYPE_GALILEO to "Galileo",
            MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA to "Pulsar Data",
        )

        private val cellOwnerNames = mapOf(
            CELL_OWNER_UNKNOWN to "(неизвестно)",
            CELL_OWNER_PLA to "Петролайн",
            CELL_OWNER_CLIENT to "Клиент",
        )

        private const val MAX_PORT_PER_SENSOR = 4

        private const val SENSOR_NAME_EMIS = "emis"
        private const val SENSOR_NAME_ESD = "esd"
        private const val SENSOR_NAME_PMP = "pmp"
        private const val SENSOR_NAME_MERCURY = "mercury"

        private val sensorAutoCreates = mapOf(
            SENSOR_NAME_EMIS to "ЭМИС",
            SENSOR_NAME_ESD to "Euro Sens",
            SENSOR_NAME_PMP to "ПМП",
            SENSOR_NAME_MERCURY to "Меркурий",
        )
        private val sensorInfos = mapOf(
            SENSOR_NAME_EMIS to listOf(
                SensorInfo(sensorType = SensorConfig.SENSOR_MASS_FLOW, portNum = PortNumbers.EMIS_MASS_FLOW_270, descrBody = "Массовый расход"),
                SensorInfo(sensorType = SensorConfig.SENSOR_DENSITY, portNum = PortNumbers.EMIS_DENSITY_280, descrBody = "Плотность"),
                SensorInfo(sensorType = SensorConfig.SENSOR_TEMPERATURE, portNum = PortNumbers.EMIS_TEMPERATURE_290, descrBody = "Температура", minView = -100.0, maxView = 100.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_VOLUME_FLOW, portNum = PortNumbers.EMIS_VOLUME_FLOW_300, descrBody = "Объёмный расход"),
                SensorInfo(sensorType = SensorConfig.SENSOR_MASS_ACCUMULATED, portNum = PortNumbers.EMIS_ACCUMULATED_MASS_310, descrBody = "Накопленная масса"),
                SensorInfo(sensorType = SensorConfig.SENSOR_VOLUME_ACCUMULATED, portNum = PortNumbers.EMIS_ACCUMULATED_VOLUME_320, descrBody = "Накопленный объём"),
            ),
            SENSOR_NAME_ESD to listOf(
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE, portNum = PortNumbers.ESD_STATUS_500, descrBody = "Состояние расходомера"),
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_USING, portNum = PortNumbers.ESD_VOLUME_504, descrBody = "Расходомер"),
                SensorInfo(sensorType = SensorConfig.SENSOR_VOLUME_FLOW, portNum = PortNumbers.ESD_FLOW_508, descrBody = "Скорость потока"),
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_USING, portNum = PortNumbers.ESD_CAMERA_VOLUME_512, descrBody = "Расходомер камеры подачи"),
                SensorInfo(sensorType = SensorConfig.SENSOR_VOLUME_FLOW, portNum = PortNumbers.ESD_CAMERA_FLOW_516, descrBody = "Скорость потока камеры подачи"),
                SensorInfo(sensorType = SensorConfig.SENSOR_TEMPERATURE, portNum = PortNumbers.ESD_CAMERA_TEMPERATURE_520, descrBody = "Температура камеры подачи", minView = -100.0, maxView = 100.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_USING, portNum = PortNumbers.ESD_REVERSE_CAMERA_VOLUME_524, descrBody = "Расходомер камеры обратки"),
                SensorInfo(sensorType = SensorConfig.SENSOR_VOLUME_FLOW, portNum = PortNumbers.ESD_REVERSE_CAMERA_FLOW_528, descrBody = "Скорость потока камеры обратки"),
                SensorInfo(sensorType = SensorConfig.SENSOR_TEMPERATURE, portNum = PortNumbers.ESD_REVERSE_CAMERA_TEMPERATURE_532, descrBody = "Температура камеры обратки", minView = -100.0, maxView = 100.0),
            ),
            SENSOR_NAME_PMP to listOf(
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_LEVEL, portNum = PortNumbers.PMP_LEVEL_540, descrBody = "Уровень топлива"),
                SensorInfo(sensorType = SensorConfig.SENSOR_TEMPERATURE, portNum = PortNumbers.PMP_TEMPERATURE_560, descrBody = "Температура", minView = -100.0, maxView = 100.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_LIQUID_LEVEL, portNum = PortNumbers.PMP_VOLUME_580, descrBody = "Объём топлива"),
                SensorInfo(sensorType = SensorConfig.SENSOR_WEIGHT, portNum = PortNumbers.PMP_MASS_600, descrBody = "Масса топлива"),
                SensorInfo(sensorType = SensorConfig.SENSOR_DENSITY, portNum = PortNumbers.PMP_DENSITY_620, descrBody = "Плотность"),
            ),
            SENSOR_NAME_MERCURY to listOf(
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AD, portNum = PortNumbers.MERCURY_COUNT_ACTIVE_DIRECT_160, descrBody = "Электроэнергия активная прямая"),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_COUNT_AR, portNum = PortNumbers.MERCURY_COUNT_ACTIVE_REVERSE_164, descrBody = "Электроэнергия активная обратная"),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RD, portNum = PortNumbers.MERCURY_COUNT_REACTIVE_DIRECT_168, descrBody = "Электроэнергия реактивная прямая"),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_COUNT_RR, portNum = PortNumbers.MERCURY_COUNT_REACTIVE_REVERSE_172, descrBody = "Электроэнергия реактивная обратная"),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE, portNum = PortNumbers.MERCURY_VOLTAGE_A_180, descrBody = "Напряжение по фазе A", phase = 1, minView = 0.0, maxView = 380.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE, portNum = PortNumbers.MERCURY_VOLTAGE_B_184, descrBody = "Напряжение по фазе B", phase = 2, minView = 0.0, maxView = 380.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_VOLTAGE, portNum = PortNumbers.MERCURY_VOLTAGE_C_188, descrBody = "Напряжение по фазе C", phase = 3, minView = 0.0, maxView = 380.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_CURRENT, portNum = PortNumbers.MERCURY_CURRENT_A_200, descrBody = "Ток по фазе A", phase = 1),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_CURRENT, portNum = PortNumbers.MERCURY_CURRENT_B_204, descrBody = "Ток по фазе B", phase = 2),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_CURRENT, portNum = PortNumbers.MERCURY_CURRENT_C_208, descrBody = "Ток по фазе C", phase = 3),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF, portNum = PortNumbers.MERCURY_POWER_KOEF_A_220, descrBody = "Коэффициент мощности по фазе A", phase = 1, minView = 0.0, maxView = 1.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF, portNum = PortNumbers.MERCURY_POWER_KOEF_B_224, descrBody = "Коэффициент мощности по фазе B", phase = 2, minView = 0.0, maxView = 1.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_KOEF, portNum = PortNumbers.MERCURY_POWER_KOEF_C_228, descrBody = "Коэффициент мощности по фазе C", phase = 3, minView = 0.0, maxView = 1.0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, portNum = PortNumbers.MERCURY_POWER_ACTIVE_A_232, descrBody = "Мощность активная по фазе A", phase = 1),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, portNum = PortNumbers.MERCURY_POWER_ACTIVE_B_236, descrBody = "Мощность активная по фазе B", phase = 2),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, portNum = PortNumbers.MERCURY_POWER_ACTIVE_C_240, descrBody = "Мощность активная по фазе C", phase = 3),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, portNum = PortNumbers.MERCURY_POWER_REACTIVE_A_244, descrBody = "Мощность реактивная по фазе A", phase = 1),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, portNum = PortNumbers.MERCURY_POWER_REACTIVE_B_248, descrBody = "Мощность реактивная по фазе B", phase = 2),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, portNum = PortNumbers.MERCURY_POWER_REACTIVE_C_252, descrBody = "Мощность реактивная по фазе C", phase = 3),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL, portNum = PortNumbers.MERCURY_POWER_FULL_A_256, descrBody = "Мощность полная по фазе A", phase = 1),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL, portNum = PortNumbers.MERCURY_POWER_FULL_B_260, descrBody = "Мощность полная по фазе B", phase = 2),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL, portNum = PortNumbers.MERCURY_POWER_FULL_C_264, descrBody = "Мощность полная по фазе C", phase = 3),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_ACTIVE, portNum = PortNumbers.MERCURY_POWER_ACTIVE_ABC_330, descrBody = "Мощность активная по всем фазам", phase = 0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, portNum = PortNumbers.MERCURY_POWER_REACTIVE_ABC_340, descrBody = "Мощность реактивная по всем фазам", phase = 0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_POWER_FULL, portNum = PortNumbers.MERCURY_POWER_FULL_ABC_350, descrBody = "Мощность полная по всем фазам", phase = 0),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT, portNum = PortNumbers.MERCURY_TRANSFORM_KOEF_CURRENT_360, descrBody = "Коэффициент трансформации по току"),
                SensorInfo(sensorType = SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE, portNum = PortNumbers.MERCURY_TRANSFORM_KOEF_VOLTAGE_370, descrBody = "Коэффициент трансформации по напряжению"),
            ),
        )
        /*
        SensorInfo(
            sensorType = ,
            portNum = ,
            descrBody = ,
        //    minView = ,
        //    maxView = ,
        //    minLimit = ,
        //    maxLimit = ,
        //    indicatorDelimiterCount = ,
        //    indicatorMultiplicator = ,
        //    phase = ,
        )
        */
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // Device.userId
        alColumnInfo += FIELD_TYPE to "Тип"
        alColumnInfo += FIELD_INDEX to "Индекс на объекте"
        alColumnInfo += FIELD_SERIAL_NO to "Серийный номер"
        alColumnInfo += null to "" // Object.userId
        alColumnInfo += FIELD_OBJECT_NAME to "Наименование"
        alColumnInfo += FIELD_OBJECT_MODEL to "Модель"

        //--- две строки в каждой ячейке - по обеим симкам
        alColumnInfo += null to "IMEI"
        alColumnInfo += null to "Владелец сим-карты"
        alColumnInfo += null to "Номер телефона"
        alColumnInfo += null to "ICC"
        alColumnInfo += null to "Мобильный оператор"

        alColumnInfo += FIELD_FW_VERSION to "Версия прошивки"
        alColumnInfo += FIELD_LAST_SESSION_TIME to "Время последней сессии"
        alColumnInfo += FIELD_LAST_SESSION_STATUS to "Статус последней сессии"
        alColumnInfo += FIELD_LAST_SESSION_ERROR to "Ошибка последней сессии"
        alColumnInfo += FIELD_USING_START_DATE to "Дата/время начала эксплуатации"

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

        val zoneLocal = getTimeZone(userConfig.timeOffset)

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_SERIAL_NO))
        val findText = action.findText?.trim() ?: ""

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId
        } else {
            null
        }
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles,
        )

        val page: Page<DeviceEntity> = parentObjectEntity?.let {
            if (findText.isNotEmpty()) {
                deviceRepository.findByObjAndUserIdInAndFilter(parentObjectEntity, enabledUserIds, findText, pageRequest)
            } else {
                deviceRepository.findByObjAndUserIdIn(parentObjectEntity, enabledUserIds, pageRequest)
            }
        } ?: run {
            if (findText.isNotEmpty()) {
                deviceRepository.findByUserIdInAndFilter(enabledUserIds, findText, pageRequest)
            } else {
                deviceRepository.findByUserIdIn(enabledUserIds, pageRequest)
            }
        }
        fillTablePageButtons(action, page.totalPages, alPageButton)
        val deviceEntities = page.content

        for (deviceEntity in deviceEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[deviceEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[deviceEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[deviceEntity.userId],
                userRoles = userConfig.roles
            )

            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = deviceEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = deviceEntity.type?.let { type ->
                    deviceTypes[type] ?: "(неизвестный тип датчика)"
                } ?: "-",
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.index?.toString() ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.serialNo ?: "-")
            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = deviceEntity.obj?.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.obj?.name ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.obj?.model ?: "-")

            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "${deviceEntity.cellImei ?: "-"}\n${deviceEntity.cellImei2 ?: "-"}")
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = "${cellOwnerNames[deviceEntity.cellOwner] ?: "-"}\n${cellOwnerNames[deviceEntity.cellOwner2] ?: "-"}"
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "${deviceEntity.cellNumber ?: "-"}\n${deviceEntity.cellNumber2 ?: "-"}")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "${deviceEntity.cellIcc ?: "-"}\n${deviceEntity.cellIcc2 ?: "-"}")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "${deviceEntity.cellOperator ?: "-"}\n${deviceEntity.cellOperator2 ?: "-"}")

            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.fwVersion ?: "-")
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = deviceEntity.lastSessionTime?.let { lastSessionTime -> getDateTimeDMYHMSString(zoneLocal, lastSessionTime) } ?: "-",
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.lastSessionStatus ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = deviceEntity.lastSessionError ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = getDateEntityDMYString(deviceEntity.usingStartDate))

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = deviceEntity.id,
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

            if (deviceEntity.id == action.id) {
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

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        val deviceEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            deviceRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = deviceEntity?.let {
            deviceEntity.userId
        } ?: userConfig.id

        val parentObjectId = deviceEntity?.let {
            deviceEntity.obj?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.OBJECT) {
                action.parentId ?: 0
            } else {
                0
            }
        }

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormComboCell(
            name = FIELD_TYPE,
            caption = "Тип",
            isEditable = changeEnabled,
            value = (deviceEntity?.type ?: MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA).toString(),
            values = deviceTypes.map { (key, value) -> key.toString() to value },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_INDEX,
            caption = "Индекс на объекте",
            isEditable = changeEnabled,
            value = deviceEntity?.index?.toString() ?: "0",
        )
        formCells += FormSimpleCell(
            name = FIELD_SERIAL_NO,
            caption = "Серийный номер",
            isEditable = changeEnabled,
            value = deviceEntity?.serialNo ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_NAME,
            caption = "Наименование",
            isEditable = false,
            value = if (parentObjectId == 0) {
                "-"
            } else {
                deviceEntity?.obj?.name ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.OBJECT,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        ObjectService.FIELD_ID to FIELD_OBJECT_ID,
                        ObjectService.FIELD_NAME to FIELD_OBJECT_NAME,
                        ObjectService.FIELD_MODEL to FIELD_OBJECT_MODEL,
                    ),
                    selectorClear = mapOf(
                        FIELD_OBJECT_ID to "0",
                        FIELD_OBJECT_NAME to "",
                        FIELD_OBJECT_MODEL to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_MODEL,
            caption = "Модель",
            isEditable = false,
            value = deviceEntity?.obj?.model ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_CELL_IMEI,
            caption = "IMEI",
            isEditable = changeEnabled,
            value = deviceEntity?.cellImei ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_CELL_OWNER,
            caption = "Владелец сим-карты",
            isEditable = changeEnabled,
            value = (deviceEntity?.cellOwner ?: CELL_OWNER_UNKNOWN).toString(),
            values = cellOwnerNames.map { (key, value) -> key.toString() to value },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_NUMBER,
            caption = "Номер телефона",
            isEditable = changeEnabled,
            value = deviceEntity?.cellNumber ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_ICC,
            caption = "ICC",
            isEditable = changeEnabled,
            value = deviceEntity?.cellIcc ?: "",
        )
        //!!! String-combo с выбором из списка ранее введённых операторов
        /*
        val cellOperators = mutableSetOf("")
        val rs = conn.executeQuery(
            " SELECT cell_operator , cell_operator_2 FROM $modelTableName"
        )
        while (rs.next()) {
            cellOperators += rs.getString(1).trim()
            cellOperators += rs.getString(2).trim()
        }
        rs.close()
        val sortedCellOperators = cellOperators.toSortedSet()
         */
        formCells += FormSimpleCell(
            name = FIELD_CELL_OPERATOR,
            caption = "Мобильный оператор",
            isEditable = changeEnabled,
            value = deviceEntity?.cellOperator ?: "",
        )

        formCells += FormSimpleCell(
            name = FIELD_CELL_IMEI_2,
            caption = "IMEI 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellImei2 ?: "",
        )
        formCells += FormComboCell(
            name = FIELD_CELL_OWNER_2,
            caption = "Владелец сим-карты 2",
            isEditable = changeEnabled,
            value = (deviceEntity?.cellOwner2 ?: CELL_OWNER_UNKNOWN).toString(),
            values = cellOwnerNames.map { (key, value) -> key.toString() to value },
            asRadioButtons = true,
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_NUMBER_2,
            caption = "Номер телефона 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellNumber2 ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_ICC_2,
            caption = "ICC 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellIcc2 ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CELL_OPERATOR_2,
            caption = "Мобильный оператор 2",
            isEditable = changeEnabled,
            value = deviceEntity?.cellOperator2 ?: "",
        )

        //--- только для показа, значения не изменяются, перед сохранением берутся свежие из базы
        formCells += FormSimpleCell(
            name = FIELD_FW_VERSION,
            caption = "Версия прошивки",
            isEditable = false,
            value = deviceEntity?.fwVersion ?: "",
        )
        formCells += FormDateTimeCell(
            name = FIELD_LAST_SESSION_TIME,
            caption = "Время последней сессии",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = deviceEntity?.lastSessionTime ?: 0,
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_SESSION_STATUS,
            caption = "Статус последней сессии",
            isEditable = false,
            value = deviceEntity?.lastSessionStatus ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_SESSION_ERROR,
            caption = "Ошибка последней сессии",
            isEditable = false,
            value = deviceEntity?.lastSessionError ?: "",
        )
        formCells += FormDateTimeCell(
            name = FIELD_USING_START_DATE,
            caption = "Дата начала эксплуатации",
            isEditable = false,
            mode = FormDateTimeCellMode.DMY,
            value = deviceEntity?.usingStartDate?.let { dt ->
                LocalDateTime(dt.ye ?: 2000, dt.mo ?: 1, dt.da ?: 1, 0, 0, 0).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            },
        )

        formCells += FormBooleanCell(
            name = FIELD_COPY_SENSORS,
            caption = if (id == null) "" else "Копировать датчики при смене объекта",
            isEditable = true,
            value = false,
        )

        sensorAutoCreates.forEach { (name, descr) ->
            val fieldName = name + SENSOR_CREATE_ENABLED

            formCells += FormBooleanCell(
                name = fieldName,
                caption = "Автосоздание датчиков $descr",
                isEditable = true,
                value = false,
//--- видимость этих полей зависит от FIELD_COPY_SENSORS, но вследствие отсутствия множественности условий visibility
//--- поля типа "name + SENSOR_GROUP_NAME + si" могут оставаться видимыми при включенной, но невидимой галочке "name + SENSOR_CREATE_ENABLED" :(
//                visibility = FormCellVisibility(
//                    name = FIELD_COPY_SENSORS,
//                    state = true,
//                    values = setOf(false.toString()),
//                ),
            )
            (1..MAX_PORT_PER_SENSOR).forEach { si ->
                formCells += FormSimpleCell(
                    name = name + SENSOR_GROUP_NAME + si,
                    caption = "Наименование группы датчиков $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
                formCells += FormSimpleCell(
                    name = name + SENSOR_DESCR_PREFIX + si,
                    caption = "Префикс наименования датчика $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
                formCells += FormSimpleCell(
                    name = name + SENSOR_DESCR_POSTFIX + si,
                    caption = "Постфикс наименования датчика $si",
                    isEditable = true,
                    value = "",
                    visibility = FormCellVisibility(
                        name = fieldName,
                        state = true,
                        values = setOf(true.toString()),
                    ),
                )
            }
        }

        return formCells
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val index = formActionData[FIELD_INDEX]?.stringValue?.toIntOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_INDEX to "Не введён индекс"))
        if (index < 0 || index >= MAX_DEVICE_COUNT_PER_OBJECT) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_INDEX to "Индекс должен быть в диапазоне от 0 до ${MAX_DEVICE_COUNT_PER_OBJECT - 1}"))
        }

        val serialNo = formActionData[FIELD_SERIAL_NO]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Не введён серийный номер"))
        if (serialNo.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Не введён серийный номер"))
        }
        if (deviceRepository.findBySerialNo(serialNo).any { se -> se.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SERIAL_NO to "Такой серийный номер уже существует"))
        }

        val objectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: 0
        val objectEntity = objectRepository.findByIdOrNull(objectId)

        val copySensors = formActionData[FIELD_COPY_SENSORS]?.booleanValue ?: false

        val usingStartDate = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), Clock.System.now().epochSeconds.toInt())

        val oldDeviceEntity = id?.let {
            deviceRepository.findByIdOrNull(id)
        }

        val recordId = id ?: getNextId { nextId -> deviceRepository.existsById(nextId) }
        val deviceEntity = DeviceEntity(
            id = recordId,
            userId = recordUserId,
            index = index,
            type = formActionData[FIELD_TYPE]?.stringValue?.toIntOrNull() ?: MMSTelematicFunction.DEVICE_TYPE_PULSAR_DATA,
            serialNo = serialNo,
            obj = objectEntity,
            cellImei = formActionData[FIELD_CELL_IMEI]?.stringValue,
            cellOwner = formActionData[FIELD_CELL_OWNER]?.stringValue?.toIntOrNull() ?: CELL_OWNER_UNKNOWN,
            cellNumber = formActionData[FIELD_CELL_NUMBER]?.stringValue,
            cellIcc = formActionData[FIELD_CELL_ICC]?.stringValue,
            cellOperator = formActionData[FIELD_CELL_OPERATOR]?.stringValue,
            cellImei2 = formActionData[FIELD_CELL_IMEI_2]?.stringValue,
            cellOwner2 = formActionData[FIELD_CELL_OWNER_2]?.stringValue?.toIntOrNull() ?: CELL_OWNER_UNKNOWN,
            cellNumber2 = formActionData[FIELD_CELL_NUMBER_2]?.stringValue,
            cellIcc2 = formActionData[FIELD_CELL_ICC_2]?.stringValue,
            cellOperator2 = formActionData[FIELD_CELL_OPERATOR_2]?.stringValue,
            fwVersion = oldDeviceEntity?.fwVersion ?: "",
            lastSessionTime = oldDeviceEntity?.lastSessionTime ?: 0,
            lastSessionStatus = oldDeviceEntity?.lastSessionStatus ?: "",
            lastSessionError = oldDeviceEntity?.lastSessionError ?: "",
            usingStartDate = oldDeviceEntity?.usingStartDate ?: DateEntity(
                ye = usingStartDate[0],
                mo = usingStartDate[1],
                da = usingStartDate[2],
            ),
        )
        deviceRepository.saveAndFlush(deviceEntity)

        if (copySensors) {
            oldDeviceEntity?.let {
                oldDeviceEntity.obj?.id?.let { oldObjectId ->
                    oldDeviceEntity.index?.let { oldIndex ->
                        if (objectId != 0 && objectId != oldObjectId) {
                            copySensors(oldObjectId, oldIndex, objectEntity, index)
                        }
                    }
                }
            }
        } else if (objectId != 0) {
            sensorAutoCreates.forEach { (name, _) ->
                val fieldName = name + SENSOR_CREATE_ENABLED

                if (formActionData[fieldName]?.booleanValue == true) {
                    (1..MAX_PORT_PER_SENSOR).forEach { si ->
                        val groupName = formActionData[name + SENSOR_GROUP_NAME + si]?.stringValue?.trim() ?: ""
                        val prefix = formActionData[name + SENSOR_DESCR_PREFIX + si]?.stringValue?.trim() ?: ""
                        val postfix = formActionData[name + SENSOR_DESCR_POSTFIX + si]?.stringValue?.trim() ?: ""

                        if (prefix.isNotEmpty() || postfix.isNotEmpty()) {
                            sensorInfos[name]?.let { sensorInfoList ->
                                sensorInfoList.forEach { sensorInfo ->
                                    addSensor(
                                        obj = objectEntity,
                                        deviceIndex = index,
                                        sensorIndex = si,
                                        groupName = groupName,
                                        descrPrefix = prefix,
                                        descrPostfix = postfix,
                                        sensorType = sensorInfo.sensorType,
                                        portNum = sensorInfo.portNum,
                                        descrBody = sensorInfo.descrBody,
                                        minView = sensorInfo.minView,
                                        maxView = sensorInfo.maxView,
                                        minLimit = sensorInfo.minLimit,
                                        maxLimit = sensorInfo.maxLimit,
                                        indicatorDelimiterCount = sensorInfo.indicatorDelimiterCount,
                                        indicatorMultiplicator = sensorInfo.indicatorMultiplicator,
                                        phase = sensorInfo.phase,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            sensorRepository.flush()
        }

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)

        val deviceEntity = id?.let {
            deviceRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[deviceEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[deviceEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        deviceRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    private fun copySensors(oldObjectId: Int, oldDeviceIndex: Int, newObjectEntity: ObjectEntity?, newDeviceIndex: Int) {
        val oldObjectEntity = objectRepository.findByIdOrNull(oldObjectId) ?: return

        sensorRepository.findByObjAndPortNumBetween(
            obj = oldObjectEntity,
            startPort = oldDeviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE,
            endPort = (oldDeviceIndex + 1) * CoreTelematicFunction.MAX_PORT_PER_DEVICE - 1,
        ).forEach { oldSensorEntity ->
            val nextSensorId = getNextId { nextId -> sensorRepository.existsById(nextId) }

            oldSensorEntity.id = nextSensorId
            oldSensorEntity.obj = newObjectEntity
            oldSensorEntity.portNum = (oldSensorEntity.portNum ?: 0) + (newDeviceIndex - oldDeviceIndex) * CoreTelematicFunction.MAX_PORT_PER_DEVICE

            sensorRepository.save(oldSensorEntity)
            SensorService.checkAndCreateAggTable(entityManager, oldSensorEntity.id)

            sensorCalibrationRepository.findBySensorOrderBySensorValue(oldSensorEntity).forEach { oldSensorCalibrationEntity ->
                oldSensorCalibrationEntity.id = getNextId { nextId -> sensorCalibrationRepository.existsById(nextId) }
                oldSensorCalibrationEntity.sensor = oldSensorEntity

                sensorCalibrationRepository.save(oldSensorCalibrationEntity)
            }
        }
        sensorRepository.flush()
        sensorCalibrationRepository.flush()
    }

    private fun addSensor(
        obj: ObjectEntity?,
        deviceIndex: Int,
        sensorIndex: Int,
        groupName: String,
        descrPrefix: String,
        descrPostfix: String,
        sensorType: Int,
        portNum: Int,
        descrBody: String,
        minView: Double,
        maxView: Double,
        minLimit: Double = minView,
        maxLimit: Double = maxView,
        indicatorDelimiterCount: Int = 4,
        indicatorMultiplicator: Double = 1.0,
        phase: Int = 0,
    ) {
        val sensorEntity = SensorEntity(
            id = getNextId { nextId -> sensorRepository.existsById(nextId) },
            obj = obj,
            name = "",
            group = groupName,
            descr = "$descrPrefix $descrBody $descrPostfix",
            portNum = deviceIndex * CoreTelematicFunction.MAX_PORT_PER_DEVICE + portNum + sensorIndex,
            sensorType = sensorType,
            serialNo = null,
            usingStartDate = DateEntity(2000, 1, 1),
            minMovingTime = 1,
            minParkingTime = 300,
            minOverSpeedTime = 60,
            isAbsoluteRun = true,
            speedRoundRule = SensorConfigGeo.SPEED_ROUND_RULE_STANDART,
            runKoef = 1.0,
            isUsePos = true,
            isUseSpeed = true,
            isUseRun = true,
            boundValue = 0,
            activeValue = 1,
            begWorkValue = 0.0,
            minOnTime = 1,
            minOffTime = 1,
            smoothTime = 0,
            minIgnore = 0.0,
            maxIgnore = 0.0,
            liquidName = "",
            liquidNorm = 0.0,
            minView = minView,
            maxView = maxView,
            minLimit = minLimit,
            maxLimit = maxLimit,
            indicatorDelimiterCount = indicatorDelimiterCount,
            indicatorMultiplicator = indicatorMultiplicator,
            isAbsoluteCount = true,
            phase = phase,
            inOutType = SensorConfigCounter.CALC_TYPE_OUT,
            containerType = SensorConfigLiquidLevel.CONTAINER_TYPE_WORK,
            usingMinLen = 1,
            isUsingCalc = false,
            detectIncKoef = 1.0,
            detectIncMinDiff = 0.0,
            detectIncMinLen = 0,
            incAddTimeBefore = 0,
            incAddTimeAfter = 0,
            detectDecKoef = 1.0,
            detectDecMinDiff = 0.0,
            detectDecMinLen = 0,
            decAddTimeBefore = 0,
            decAddTimeAfter = 0,
            schemeX = 0,
            schemeY = 0,
            //!!! для совместимости со старой версией
            cmdOnId = 0,
            cmdOffId = 0,
            signalOn = "",
            signalOff = "",
            smoothMethod = 0,
        )
        sensorRepository.save(sensorEntity)
        SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)
    }

}

private class SensorInfo(
    val sensorType: Int,
    val portNum: Int,
    val descrBody: String,
    val minView: Double = 0.0,
    val maxView: Double = 1000.0,
    val minLimit: Double = minView,
    val maxLimit: Double = maxView,
    val indicatorDelimiterCount: Int = 5,
    val indicatorMultiplicator: Double = 1.0,
    val phase: Int = 0,
)
