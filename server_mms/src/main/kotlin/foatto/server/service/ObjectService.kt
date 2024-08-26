package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
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
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core_mms.AppModuleMMS
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.ObjectEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DepartmentRepository
import foatto.server.repository.FileStoreRepository
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.persistence.EntityManager

@Service
class ObjectService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val departmentRepository: DepartmentRepository,
    private val groupRepository: GroupRepository,
    private val fileStoreRepository: FileStoreRepository,
) : ApplicationService() {

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        if (action.isSelectorMode) {
            alColumnInfo += null to "" // selector button
        }
        alColumnInfo += null to "" // userId
        alColumnInfo += FIELD_IS_DISABLED to "Заблокирован"
        alColumnInfo += FIELD_NAME to "Наименование"
        alColumnInfo += FIELD_MODEL to "Модель"
        alColumnInfo += null to "Подразделение"
        alColumnInfo += null to "Группа"

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

        val pageRequest = getTableSortedPageRequest(action, defaultFieldNames = arrayOf(FIELD_NAME))
        val findText = action.findText?.trim() ?: ""

        val enabledUserIds = getEnabledUserIds(action, userConfig)

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
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.name ?: "-", align = TableCellAlign.LEFT)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.model ?: "-", align = TableCellAlign.LEFT)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.department?.name ?: "-", align = TableCellAlign.LEFT)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.group?.name ?: "-", align = TableCellAlign.LEFT)
            if (isAdminOnly(userConfig)) {
                alTableCell += TableButtonCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    values = getTableFileButtonCellData(fileStoreRepository, objectEntity.fileId),
                )
            }

            val formAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = objectEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val alPopupData = mutableListOf<TablePopupData>()

            if (isFormEnabled) {
                alPopupData += TablePopupData(
                    action = formAction,
                    text = "Открыть",
                    inNewTab = false,
                )
            }

            alTableRowData.add(
                TableRowData(
                    formAction = if (isFormEnabled) {
                        formAction
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
                        formAction
                    } else {
                        null
                    },
                    isRowUrlInNewTab = false,
                    gotoAction = null,
                    isGotoUrlInNewTab = true,
                    alPopupData = alPopupData,
                )
            )

            if (objectEntity.id == action.id) {
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
                action.parentId
            } else {
                null
            }
        }

        val groupId = objectEntity?.let {
            objectEntity.group?.id
        } ?: run {
            if (action.parentModule == AppModuleMMS.GROUP) {
                action.parentId
            } else {
                null
            }
        }

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            formCells = formCells,
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_DISABLED,
            caption = "Заблокирован",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = objectEntity?.isDisabled ?: false,
        )
        formCells += FormSimpleCell(
            name = FIELD_DISABLE_REASON,
            caption = "Причина блокировки",
            isEditable = changeEnabled,
            isAutoFocus = false,
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
            isAutoFocus = false,
            value = objectEntity?.name ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_MODEL,
            caption = "Модель",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = objectEntity?.model ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT,
            caption = "",
            isEditable = false,
            isAutoFocus = false,
            value = departmentId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_DEPARTMENT_NAME,
            caption = "Подразделение",
            isEditable = false,
            isAutoFocus = false,
            value = if (departmentId == null || departmentId == 0) {
                "-"
            } else {
                objectEntity?.department?.name ?: "(неизвестно)"
            },
            selectorAction = AppAction(
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
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP,
            caption = "",
            isEditable = false,
            isAutoFocus = false,
            value = groupId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_GROUP_NAME,
            caption = "Группа",
            isEditable = false,
            isAutoFocus = false,
            value = if (groupId == null || groupId == 0) {
                "-"
            } else {
                objectEntity?.group?.name ?: "(неизвестно)"
            },
            selectorAction = AppAction(
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
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_INFO,
            caption = "Информация",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = objectEntity?.info ?: "",
            rows = 5,
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = objectEntity?.eMail ?: "",
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файл схемы объекта",
            isEditable = changeEnabled,
            isAutoFocus = false,
            fileId = objectEntity?.fileId,
            files = getFormFileCellData(fileStoreRepository, objectEntity?.fileId)
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_AUTO_WORK_SHIFT_ENABLED,
            caption = "Автосоздание рабочих смен",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = objectEntity?.isAutoWorkShiftEnabled ?: false,
        )

        return formCells
    }

    override fun getFormButtons(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean, deleteEnabled: Boolean): List<FormButton> {
        val id = action.id

        val isExistsDepencies = false
        //!!!
//        val isExistsDepencies: Boolean = if (deleteEnabled) {
//            id?.let {
//                xxxRepository.findByParentId(id).isNotEmpty()
//            } ?: false
//        } else {
//            false
//        }

        return super.getFormButtons(action, userConfig, moduleConfig, addEnabled, editEnabled, deleteEnabled && !isExistsDepencies)
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): Map<String, String>? {
        val id = action.id

        //TODO: общий Map со всеми ошибками
        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val name = formActionData[FIELD_NAME]?.stringValue ?: return mapOf(FIELD_NAME to "Не введёно наименование")
        if (name.isBlank()) {
            return mapOf(FIELD_NAME to "Не введёно наименование")
        }
        if (objectRepository.findByUserIdAndName(recordUserId, name).any { oe -> oe.id != id }) {
            return mapOf(FIELD_NAME to "Такое наименование уже существует")
        }

        // !!! getNextIntId(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))
        val recordId = id ?: getNextId()
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
                formActionSaveFiles(fileStoreRepository, fad)
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
                " CREATE INDEX MMS_data_${recordId}_ontime ON MMS_data_$recordId ( ontime ) "
            )
        }

        return null
    }

    override fun formActionDelete(userId: Int, id: Int): Map<String, String>? {
        objectRepository.deleteById(id)
        executeNativeSql(entityManager, " DROP TABLE MMS_data_$id ")

/* !!!
        val sensorIds = mutableListOf<Int>()
        val rs = conn.executeQuery(" SELECT id FROM MMS_sensor WHERE object_id = $id ")
        while (rs.next()) {
            sensorIds += rs.getInt(1)
        }
        rs.close()

        for (sensorId in sensorIds) {
            if (MMSFunction.checkAggregationTableIsExists(conn, sensorId)) {
                MMSFunction.deleteAggTable(conn, sensorId)
            }
        }

        conn.executeUpdate(" DELETE FROM MMS_work_shift_data WHERE shift_id IN ( SELECT id FROM MMS_work_shift WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_work_shift WHERE object_id = $id ")

        conn.executeUpdate(" DELETE FROM MMS_sensor_calibration WHERE sensor_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_equip_service_shedule WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_equip_service_history WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_sensor WHERE object_id = $id ")
 */
        return null
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

    override fun checkExistingId(nextId: Int): Boolean = objectRepository.existsById(nextId)

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_IS_DISABLED = "isDisabled"
        private const val FIELD_DISABLE_REASON = "disableReason"
        private const val FIELD_NAME = "name"
        private const val FIELD_MODEL = "model"
        private const val FIELD_DEPARTMENT = "department"
        private const val FIELD_GROUP = "group"
        private const val FIELD_INFO = "info"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_FILE = "fileId"

        //        private const val FIELD_LAST_ALERT_TIME = "lastAlertTime"
        private const val FIELD_IS_AUTO_WORK_SHIFT_ENABLED = "isAutoWorkShiftEnabled"

        private const val FIELD_OWNER_FULL_NAME = "ownerFullName"   // псевдополе для селектора
        private const val FIELD_DEPARTMENT_NAME = "departmentName"   // псевдополе для селектора
        private const val FIELD_GROUP_NAME = "groupName"             // псевдополе для селектора
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