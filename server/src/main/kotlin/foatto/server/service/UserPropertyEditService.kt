package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.server.checkRowPermission
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.UserRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserPropertyEditService(
    private val userRepository: UserRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        private const val FIELD_TIME_OFFSET = "timeOffset"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_CONTACT_INFO = "contactInfo"
        private const val FIELD_USE_THOUSANDS_DIVIDER = "useThousandsDivider"
        private const val FIELD_DECIMAL_SEPARATOR = "decimalSeparator"
        private const val FIELD_FILE = "fileId"
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction?, String>> = emptyList()

    override fun fillTableGridData(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, alTableCell: MutableList<TableBaseCell>, alTableRowData: MutableList<TableRowData>, alPageButton: MutableList<Pair<AppAction?, String>>): Int? = null

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val userEntity = id?.let {
            userRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        formCells += FormSimpleCell(
            name = FIELD_TIME_OFFSET,
            caption = "Сдвиг часового пояса [сек]",
            isEditable = changeEnabled,
            value = (userEntity?.timeOffset ?: TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds).toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            value = userEntity?.eMail ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CONTACT_INFO,
            caption = "Контактная информация",
            isEditable = changeEnabled,
            value = userEntity?.contactInfo ?: "",
            rows = 5,
        )
        formCells += FormBooleanCell(
            name = FIELD_USE_THOUSANDS_DIVIDER,
            caption = "Разделять тысячи пробелами",
            isEditable = changeEnabled,
            value = userEntity?.useThousandsDivider ?: true,
        )
        formCells += FormSimpleCell(
            name = FIELD_DECIMAL_SEPARATOR,
            caption = "Разделить дробной части",
            isEditable = changeEnabled,
            value = userEntity?.decimalSeparator ?: ".",
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файлы",
            isEditable = changeEnabled,
            fileId = userEntity?.fileId,
            files = getFormFileCellData(userEntity?.fileId)
        )

        return formCells
    }

    override fun getFormButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
        deleteEnabled: Boolean
    ): List<FormButton> = super.getFormButtons(action, userConfig, moduleConfig, addEnabled, editEnabled, deleteEnabled).filter { formButton ->
        formButton.actionType != ActionType.FORM_EXIT
    }

    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {
        val id = action.id ?: return FormActionResponse(responseCode = ResponseCode.ERROR)

        val userEntity = userRepository.findByIdOrNull(id) ?: return FormActionResponse(responseCode = ResponseCode.ERROR)

        userEntity.apply {
            timeOffset = formActionData[FIELD_TIME_OFFSET]?.stringValue?.toIntOrNull() ?: TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds
            eMail = formActionData[FIELD_EMAIL]?.stringValue
            contactInfo = formActionData[FIELD_CONTACT_INFO]?.stringValue
            useThousandsDivider = formActionData[FIELD_USE_THOUSANDS_DIVIDER]?.booleanValue ?: true
            decimalSeparator = formActionData[FIELD_DECIMAL_SEPARATOR]?.stringValue ?: "."
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fad)
            }
        }
        userRepository.saveAndFlush(userEntity)

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = AppAction(type = ActionType.NOTHING, isAutoClose = true)
        )
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(responseCode = ResponseCode.ERROR)

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = false

        val userEntity = id?.let {
            userRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[userEntity?.userId], userConfig.roles)
        val deleteEnabled = false

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }
}