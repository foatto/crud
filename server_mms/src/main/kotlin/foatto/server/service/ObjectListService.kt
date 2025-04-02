package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core_mms.AppModuleMMS
import foatto.server.entity.ObjectEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class ObjectListService(
    private val objectRepository: ObjectRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        private const val FIELD_IS_DISABLED = "isDisabled"
        const val FIELD_NAME: String = "name"
        const val FIELD_MODEL: String = "model"
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

//        alColumnInfo += null to "" // userId
//        alColumnInfo += FIELD_IS_DISABLED to "Заблокирован"
        alColumnInfo += FIELD_NAME to "Наименование"
//        alColumnInfo += FIELD_MODEL to "Модель"
//        alColumnInfo += null to "Подразделение"     //!!! сортировка как в gnssapp (и проверить поиск!)
//        alColumnInfo += null to "Группа"            //!!! сортировка как в gnssapp (и проверить поиск!)

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

//            alTableCell += getTableUserNameCell(
//                row = row,
//                col = col++,
//                userId = userConfig.id,
//                rowUserId = objectEntity.userId,
//                rowOwnerShortName = rowOwnerShortName,
//                rowOwnerFullName = rowOwnerFullName
//            )
//            alTableCell += TableBooleanCell(row = row, col = col++, dataRow = row, value = objectEntity.isDisabled ?: false)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.name ?: "-")
//            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.model ?: "-")
//            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.department?.name ?: "-")
//            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = objectEntity.group?.name ?: "-")

            alTableRowData += TableRowData(
                formAction = null,
                rowAction = AppAction(
                    type = ActionType.MODULE_COMPOSITE,
                    module = AppModuleMMS.COMPOSITE_OBJECT_LIST_DASHBOARD,
                    id = objectEntity.id,
                ),
                isRowUrlInNewTab = false,
                gotoAction = null,
                isGotoUrlInNewTab = true,
                alPopupData = emptyList(),
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
    ): List<FormBaseCell> = emptyList()

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse = FormActionResponse(responseCode = ResponseCode.OK)

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(responseCode = ResponseCode.OK)

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)

}
