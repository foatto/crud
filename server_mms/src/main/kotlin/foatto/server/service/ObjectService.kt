package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableBooleanCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core_mms.AppModuleMMS
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.ObjectEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.DeviceRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.getNextId
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ObjectService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val deviceRepository: DeviceRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        const val FIELD_ID: String = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_IS_DISABLED = "isDisabled"
        private const val FIELD_DISABLE_REASON = "disableReason"
        const val FIELD_NAME: String = "name"
        const val FIELD_MODEL: String = "model"
        private const val FIELD_DEPARTMENT = "department"
        private const val FIELD_GROUP = "group"
        private const val FIELD_INFO = "info"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_FILE = "fileId"

        //        private const val FIELD_LAST_ALERT_TIME = "lastAlertTime"
        private const val FIELD_IS_AUTO_WORK_SHIFT_ENABLED = "isAutoWorkShiftEnabled"

        private const val FIELD_OWNER_FULL_NAME = "_ownerFullName"   // псевдополе для селектора
        private const val FIELD_DEPARTMENT_NAME = "_departmentName"   // псевдополе для селектора
        private const val FIELD_GROUP_NAME = "_groupName"             // псевдополе для селектора
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        if (action.isSelectorMode) {
            alColumnInfo += null to "" // selector button
        }
        alColumnInfo += null to "" // userId
        alColumnInfo += FIELD_IS_DISABLED to "Заблокирован"
        alColumnInfo += FIELD_NAME to "Наименование"
        alColumnInfo += FIELD_MODEL to "Модель"
        alColumnInfo += null to "Подразделение"     //!!! сортировка как в gnssapp (и проверить поиск!)
        alColumnInfo += null to "Группа"            //!!! сортировка как в gnssapp (и проверить поиск!)

        if (isAdminOnly(userConfig)) {
            alColumnInfo += null to "Файл схемы объекта"
        }

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

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_NAME))
        val findText = action.findText?.trim() ?: ""

        val enabledUserIds = getEnabledUserIds(action.module, action.type, userConfig.relatedUserIds, userConfig.roles)

        val page: Page<ObjectEntity> = if (findText.isNotEmpty()) {
            objectRepository.findByUserIdInAndFilter(enabledUserIds, findText, pageRequest)
        } else {
            objectRepository.findByUserIdIn(enabledUserIds, pageRequest)
        }
        fillTablePageButtons(action, page.totalPages, alPageButton)
        val objectEntities = page.content

        for (objectEntity in objectEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[objectEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[objectEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[objectEntity.userId],
                userRoles = userConfig.roles
            )

            val selectorAction = AppAction(
                type = ActionType.FORM_SELECTOR,
                selectorData = mapOf(
                    FIELD_ID to objectEntity.id.toString(),
                    FIELD_NAME to (objectEntity.name ?: "(неизвестно)"),
                ),
            )

            if (action.isSelectorMode) {
                alTableCell += getTableSelectorButtonCell(row = row, col = col++, selectorAction = selectorAction)
            }
            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = objectEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            alTableCell += TableBooleanCell(row = row, col = col++, dataRow = row, value = objectEntity.isDisabled ?: false)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.name ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.model ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.department?.name ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.group?.name ?: "-")
            if (isAdminOnly(userConfig)) {
                alTableCell += TableButtonCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    values = getTableFileButtonCellData(objectEntity.fileId),
                )
            }

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = objectEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val alPopupData = getPopupDatas(
                userConfig = userConfig,
                id = objectEntity.id,
                isFormEnabled = isFormEnabled,
                formOpenAction = formOpenAction,
            )

            alTableRowData += TableRowData(
                formAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                rowAction = if (action.isSelectorMode) {
                    AppAction(
                        type = ActionType.FORM_SELECTOR,
                        selectorData = mapOf(
                            FIELD_ID to objectEntity.id.toString(),
                            FIELD_NAME to (objectEntity.name ?: "(неизвестно)"),
                        ),
                    )
                } else if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                gotoAction = null,
                isGotoUrlInNewTab = true,
                alPopupData = alPopupData,
            )

            if (objectEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    private fun getPopupDatas(
        userConfig: ServerUserConfig,
        id: Int,
        isFormEnabled: Boolean,
        formOpenAction: AppAction,
    ): List<TablePopupData> {
        val alPopupData = mutableListOf<TablePopupData>()

        if (isFormEnabled) {
            alPopupData += TablePopupData(
                action = formOpenAction,
                text = "Открыть",
                inNewTab = false,
            )
        }

        val begTime = getCurrentTimeInt() / 86_400 * 86_400 - userConfig.timeOffset
        val endTime = begTime + 86_400

        getTableTablePopupData(userConfig, AppModuleMMS.DAY_WORK, id, alPopupData)

        getTableTablePopupData(userConfig, AppModuleMMS.SENSOR, id, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.OBJECT_DATA, id, alPopupData)
        getTableTablePopupData(userConfig, AppModuleMMS.DEVICE, id, alPopupData)

        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, id, alPopupData)
        getTableChartPopupData(userConfig, AppModuleMMS.CHART_ANALOGUE_BY_TYPE, id, alPopupData)

        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_SUMMARY, id, begTime, endTime, alPopupData)

        if (checkAccessPermission(AppModuleMMS.MAP_TRACE, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Карты",
                action = AppAction(
                    type = ActionType.MODULE_MAP,
                    module = AppModuleMMS.MAP_TRACE,
                    id = id,
                    timeRangeType = 30 * 24 * 60 * 60,   //!!! траектория за последние 24 часа (а не 30 дней, как сейчас)
                ),
                text = appModuleConfigs[AppModuleMMS.MAP_TRACE]?.caption ?: "(неизвестный тип карты)",
                inNewTab = true,
            )
        }
//        if (checkAccessPermission(AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE, userConfig.roles)) {
//            alPopupData += TablePopupData(
//                group = "Контрольные схемы",
//                action = AppAction(
//                    type = ActionType.MODULE_SCHEME,
//                    module = AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE,
//                    id = id,
//                ),
//                text = appModuleConfigs[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE]?.caption ?: "(неизвестный тип схемы)",
//                inNewTab = true,
//            )
//        }
        if (checkAccessPermission(AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Контрольные панели",
                action = AppAction(
                    type = ActionType.MODULE_COMPOSITE,
                    module = AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD,
                    id = id,
                ),
                text = appModuleConfigs[AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD]?.caption ?: "(неизвестный тип контрольной панели)",
                inNewTab = true,
            )
        }

        return alPopupData
    }

    private fun getTableTablePopupData(userConfig: ServerUserConfig, module: String, id: Int, alPopupData: MutableList<TablePopupData>) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopupData(
                action = AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = module,
                    parentModule = AppModuleMMS.OBJECT,
                    parentId = id,
                ),
                text = appModuleConfigs[module]?.caption ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    private fun getTableChartPopupData(userConfig: ServerUserConfig, module: String, id: Int, alPopupData: MutableList<TablePopupData>) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Графики",
                action = AppAction(
                    type = ActionType.MODULE_CHART,
                    module = module,
                    id = id,
                    timeRangeType = 24 * 60 * 60,   // графики за последние 24 часа
                    //!!! где-то здесь надо передавать конкретный тип аналогового датчика (пока будем выводить графики по всем аналоговым датчикам сразу)
                ),
                text = appModuleConfigs[module]?.caption ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    private fun getTableReportPopupData(
        userConfig: ServerUserConfig,
        module: String,
        id: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopupData>
    ) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Отчёты",
                action = AppAction(
                    type = ActionType.MODULE_FORM,
                    module = module,
                    id = null,
                    parentModule = AppModuleMMS.OBJECT,
                    parentId = id,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[module]?.caption ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
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

        val objectEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            objectRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = objectEntity?.let {
            objectEntity.userId
        } ?: userConfig.id

        val departmentId = objectEntity?.let {
            objectEntity.department?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.DEPARTMENT) {
                action.parentId ?: 0
            } else {
                0
            }
        }

        val groupId = objectEntity?.let {
            objectEntity.group?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.GROUP) {
                action.parentId ?: 0
            } else {
                0
            }
        }

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_DISABLED,
            caption = "Заблокирован",
            isEditable = changeEnabled,
            value = objectEntity?.isDisabled ?: false,
        )
        formCells += FormSimpleCell(
            name = FIELD_DISABLE_REASON,
            caption = "Причина блокировки",
            isEditable = changeEnabled,
            value = objectEntity?.disableReason ?: "",
            visibility = FormCellVisibility(
                name = FIELD_IS_DISABLED,
                state = true,
                values = setOf(true.toString()),
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_NAME,
            caption = "Наименование",
            isEditable = changeEnabled,
            value = objectEntity?.name ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_MODEL,
            caption = "Модель",
            isEditable = changeEnabled,
            value = objectEntity?.model ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT,
            caption = "",
            isEditable = false,
            value = departmentId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT_NAME,
            caption = "Подразделение",
            isEditable = false,
            value = if (departmentId == null || departmentId == 0) {
                "-"
            } else {
                objectEntity?.department?.name ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.DEPARTMENT,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        DepartmentService.FIELD_ID to FIELD_DEPARTMENT,
                        DepartmentService.FIELD_NAME to FIELD_DEPARTMENT_NAME,
                    ),
                    selectorClear = mapOf(
                        FIELD_DEPARTMENT to "0",
                        FIELD_DEPARTMENT_NAME to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP,
            caption = "",
            isEditable = false,
            value = groupId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP_NAME,
            caption = "Группа",
            isEditable = false,
            value = if (groupId == null || groupId == 0) {
                "-"
            } else {
                objectEntity?.group?.name ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModuleMMS.GROUP,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        GroupService.FIELD_ID to FIELD_GROUP,
                        GroupService.FIELD_NAME to FIELD_GROUP_NAME,
                    ),
                    selectorClear = mapOf(
                        FIELD_GROUP to "0",
                        FIELD_GROUP_NAME to "",
                    ),
                )
            } else {
                null
            },
        )
        formCells += FormSimpleCell(
            name = FIELD_INFO,
            caption = "Информация",
            isEditable = changeEnabled,
            value = objectEntity?.info ?: "",
            rows = 5,
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            value = objectEntity?.eMail ?: "",
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файл схемы объекта",
            isEditable = changeEnabled,
            fileId = objectEntity?.fileId,
            files = getFormFileCellData(objectEntity?.fileId)
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_AUTO_WORK_SHIFT_ENABLED,
            caption = "Автосоздание рабочих смен",
            isEditable = changeEnabled,
            value = objectEntity?.isAutoWorkShiftEnabled ?: false,
        )

        return formCells
    }

    override fun getFormButtons(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean, deleteEnabled: Boolean): List<FormButton> {
        val id = action.id

        val isExistsDepencies: Boolean = if (deleteEnabled) {
            id?.let {
                deviceRepository.findByObjectId(id).isNotEmpty()
            } ?: false
        } else {
            false
        }

        return super.getFormButtons(action, userConfig, moduleConfig, addEnabled, editEnabled, deleteEnabled && !isExistsDepencies)
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val name = formActionData[FIELD_NAME]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        if (name.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        }
        if (objectRepository.findByUserIdAndName(recordUserId, name).any { oe -> oe.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Такое наименование уже существует"))
        }

        // !!! getNextIntId(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))
        val recordId = id ?: getNextId { nextId -> objectRepository.existsById(nextId) }
        val objectEntity = ObjectEntity(
            id = recordId,
            userId = recordUserId,
            isDisabled = formActionData[FIELD_IS_DISABLED]?.booleanValue == true,
            disableReason = formActionData[FIELD_DISABLE_REASON]?.stringValue,
            name = formActionData[FIELD_NAME]?.stringValue,
            model = formActionData[FIELD_MODEL]?.stringValue,
            department = departmentRepository.findByIdOrNull(formActionData[FIELD_DEPARTMENT]?.stringValue?.toIntOrNull() ?: 0),
            group = groupRepository.findByIdOrNull(formActionData[FIELD_GROUP]?.stringValue?.toIntOrNull() ?: 0),
            info = formActionData[FIELD_INFO]?.stringValue,
            eMail = formActionData[FIELD_EMAIL]?.stringValue,
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fad)
            },
            isAutoWorkShiftEnabled = formActionData[FIELD_IS_AUTO_WORK_SHIFT_ENABLED]?.booleanValue == true,
        )
        objectRepository.saveAndFlush(objectEntity)

        id?.let {
            //!!! executeNativeSql(" UPDATE MMS_day_work SET user_id = $recordUserId WHERE object_id = $recordId ")
        } ?: run {
            executeNativeSql(
                entityManager,
                " CREATE TABLE MMS_data_$recordId ( ontime INT NOT NULL, sensor_data BYTEA ) ",
                " CREATE INDEX MMS_data_${recordId}_ontime ON MMS_data_$recordId ( ontime ) ",
            )
        }

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        //--- remove all depended sensors & datas
        objectRepository.findByIdOrNull(id)?.let { objectEntity ->
            fileStoreService.deleteFile(objectEntity.fileId, null)
            sensorRepository.findByObj(objectEntity).forEach { sensorEntity ->
                SensorService.deleteSensor(
                    entityManager = entityManager,
                    sensorRepository = sensorRepository,
                    sensorCalibrationRepository = sensorCalibrationRepository,
                    sensorId = sensorEntity.id,
                )
            }
        }

        objectRepository.deleteById(id)
        executeNativeSql(entityManager, " DROP TABLE MMS_data_$id ")

        /*
            conn.executeUpdate(" DELETE FROM MMS_work_shift_data WHERE shift_id IN ( SELECT id FROM MMS_work_shift WHERE object_id = $id ) ")
            conn.executeUpdate(" DELETE FROM MMS_work_shift WHERE object_id = $id ")

            conn.executeUpdate(" DELETE FROM MMS_equip_service_shedule WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
            conn.executeUpdate(" DELETE FROM MMS_equip_service_history WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
         */
        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)

        val objectEntity = id?.let {
            objectRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[objectEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[objectEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

}
/*
    companion object {
        const val PERM_REMOTE_CONTROL: String = "remote_control"
        const val PERM_SCHEME_SENSOR_MOVE: String = "sensor_move"
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа на (дистанционное) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
        //--- права доступа на перемещение датчиков по схеме объекта
        alPermission.add(Pair(PERM_SCHEME_SENSOR_MOVE, "21 Scheme Sensor Move"))
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val md = model as mObject
        if (column == md.columnObjectName) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            if ((hmColumnData[md.columnDisabled] as DataBoolean).value) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            } else {
                val id = (hmColumnData[model.columnId] as DataInt).intValue

                val rs = conn.executeQuery(" SELECT MAX(ontime) FROM MMS_data_${id} ")
                val lastDataTime = if (rs.next()) {
                    rs.getInt(1)
                } else {
                    0
                }
                rs.close()

                //--- нет данных больше суток - критично
                if (getCurrentTimeInt() - lastDataTime > 1 * 24 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
                //--- нет данных больше часа - предупреждение + начинается оповещение по e-mail
                else if (getCurrentTimeInt() - lastDataTime > 1 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
                }
                //--- всё нормально
                else {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
                }
            }
        }
    }
 */