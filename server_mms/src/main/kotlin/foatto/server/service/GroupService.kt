package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.GroupEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.GroupRepository
import foatto.server.repository.ObjectRepository
import foatto.server.util.getNextId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val objectRepository: ObjectRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        const val FIELD_ID: String = "id"
        private const val FIELD_USER_ID = "userId"
        const val FIELD_NAME: String = "name"

        private const val FIELD_OWNER_FULL_NAME = "ownerFullName"   // псевдополе для селектора
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        if (action.isSelectorMode) {
            alColumnInfo += null to "" // selector button
        }
        alColumnInfo += null to "" // userId
        alColumnInfo += FIELD_NAME to "Наименование"

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

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles,
        )

        val page: Page<GroupEntity> = if (findText.isNotEmpty()) {
            groupRepository.findByUserIdInAndFilter(enabledUserIds, findText, pageRequest)
        } else {
            groupRepository.findByUserIdIn(enabledUserIds, pageRequest)
        }
        fillTablePageButtons(action, page.totalPages, alPageButton)
        val groupEntities = page.content

        for (groupEntity in groupEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[groupEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[groupEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[groupEntity.userId],
                userRoles = userConfig.roles
            )

            val selectorAction = AppAction(
                type = ActionType.FORM_SELECTOR,
                selectorData = mapOf(
                    FIELD_ID to groupEntity.id.toString(),
                    FIELD_NAME to (groupEntity.name ?: "(неизвестно)"),
                ),
            )

            if (action.isSelectorMode) {
                alTableCell += getTableSelectorButtonCell(row = row, col = col++, selectorAction = selectorAction)
            }
            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = groupEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = groupEntity.name ?: "-")

            val formAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = groupEntity.id,
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

            alTableRowData += TableRowData(
                formAction = if (isFormEnabled) {
                    formAction
                } else {
                    null
                },
                rowAction = if (action.isSelectorMode) {
                    AppAction(
                        type = ActionType.FORM_SELECTOR,
                        selectorData = mapOf(
                            FIELD_ID to groupEntity.id.toString(),
                            FIELD_NAME to (groupEntity.name ?: "(неизвестно)"),
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

            if (groupEntity.id == action.id) {
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

        val groupEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            groupRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val userId = groupEntity?.let {
            groupEntity.userId
        } ?: userConfig.id

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormSimpleCell(
            name = FIELD_NAME,
            caption = "Наименование",
            isEditable = changeEnabled,
            value = groupEntity?.name ?: "",
        )

        return formCells
    }

    override fun getFormButtons(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean, deleteEnabled: Boolean): List<FormButton> {
        val id = action.id

        val isExistsDepencies: Boolean = if (deleteEnabled) {
            id?.let {
                objectRepository.findByGroupId(id).isNotEmpty()
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

        //TODO: общий Map со всеми ошибками
        val recordUserId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0

        val name = formActionData[FIELD_NAME]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        if (name.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Не введёно наименование"))
        }
        if (groupRepository.findByUserIdAndName(recordUserId, name).any { ge -> ge.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_NAME to "Такое наименование уже существует"))
        }

        val recordId = id ?: getNextId { nextId -> groupRepository.existsById(nextId) }
        val groupEntity = GroupEntity(
            id = recordId,
            userId = recordUserId,
            name = name,
        )
        groupRepository.saveAndFlush(groupEntity)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        groupRepository.deleteById(id)
        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)

        val groupEntity = id?.let {
            groupRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[groupEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[groupEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

}
