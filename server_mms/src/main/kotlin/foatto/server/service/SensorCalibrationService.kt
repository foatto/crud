package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core_mms.AppModuleMMS
import foatto.server.UserRelationEnum
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.SensorCalibrationEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.getNextId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SensorCalibrationService(
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        //        private const val FIELD_ID = "id"
        private const val FIELD_SENSOR = "sensor"
        private const val FIELD_SENSOR_VALUE = "sensorValue"
        private const val FIELD_SENSOR_DATA = "sensorData"
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += FIELD_SENSOR_VALUE to "Значение датчика"
        alColumnInfo += FIELD_SENSOR_DATA to "Значение измеряемой величины"

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

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_SENSOR_VALUE))

        val parentSensorId = if (action.parentModule == AppModuleMMS.SENSOR) {
            action.parentId ?: return null
        } else {
            return null
        }
        val parentSensorEntity = sensorRepository.findByIdOrNull(parentSensorId) ?: return null

        val page: Page<SensorCalibrationEntity> = sensorCalibrationRepository.findBySensor(parentSensorEntity, pageRequest)

        fillTablePageButtons(action, page.totalPages, alPageButton)
        val sensorCalibrationEntities = page.content

        for (sensorCalibrationEntity in sensorCalibrationEntities) {
            var col = 0

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = UserRelationEnum.NOBODY,
                userRoles = userConfig.roles
            )

            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorCalibrationEntity.sensorValue?.toString() ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = sensorCalibrationEntity.dataValue?.toString() ?: "-")

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = sensorCalibrationEntity.id,
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

            if (sensorCalibrationEntity.id == action.id) {
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

        val sensorCalibrationEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            sensorCalibrationRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentSensorId = if (action.parentModule == AppModuleMMS.SENSOR) {
            action.parentId ?: 0
        } else {
            0
        }

        formCells += FormSimpleCell(
            name = FIELD_SENSOR,
            caption = "",
            isEditable = false,
            value = parentSensorId.toString(),
        )

        formCells += FormSimpleCell(
            name = FIELD_SENSOR_VALUE,
            caption = "Значение датчика",
            isEditable = changeEnabled,
            value = sensorCalibrationEntity?.sensorValue?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_SENSOR_DATA,
            caption = "Значение измеряемой величины",
            isEditable = changeEnabled,
            value = sensorCalibrationEntity?.dataValue?.toString() ?: "",
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

        val parentSensorId = formActionData[FIELD_SENSOR]?.stringValue?.toIntOrNull() ?: 0
        val parentSensorEntity = sensorRepository.findByIdOrNull(parentSensorId)!!

        val sensorValue = formActionData[FIELD_SENSOR_VALUE]?.stringValue?.toDoubleOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SENSOR_VALUE to "Не введёно значение датчика"))
        val dataValue = formActionData[FIELD_SENSOR_DATA]?.stringValue?.toDoubleOrNull() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_SENSOR_DATA to "Не введёно значение измеряемой величины"))

        val recordId = id ?: getNextId { nextId -> sensorCalibrationRepository.existsById(nextId) }
        val sensorCalibrationEntity = SensorCalibrationEntity(
            id = recordId,
            sensor = parentSensorEntity,
            sensorValue = sensorValue,
            dataValue = dataValue,
        )
        sensorCalibrationRepository.saveAndFlush(sensorCalibrationEntity)

        return FormActionResponse(responseCode = ResponseCode.OK)
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
        sensorCalibrationRepository.deleteById(id)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

}

/*
class cSensorCalibration : cStandart() {

    private val SENSOR_ID = "sensor_id"

    //--- префикс полей со значениями
    private val SENSOR_FIELD_PREFIX = "value_sensor_"
    private val DATA_FIELD_PREFIX = "value_data_"

    override fun getForm(hmOut: MutableMap<String, Any>): FormResponse {

        val id = getIdFromParam()
        //--- мегаформа ввода калибровок открывается только при попытке их создания,
        //--- иначе запускаем обычную привычную форму
        if (id != 0) {
            return super.getForm(hmOut)
        }

        val refererID = hmParam[AppParameter.REFERER]
        val refererURL = refererID?.let { chmSession[AppParameter.REFERER + it] as String }

        //--- подготовка "чистого" appParam для кнопок формы
        //--- ( простое клонирование исходного hmParam здесь не годится,
        //--- т.к. придёт много попутных мусорных параметров, которые могут внезапно выстрелить где-нибудь
        val formParam = getFormParam()

        //--- начало нестандартной части ---------------------------------------------------------------------------------------

        //--- сбор парентов
        val sensorID = getParentId("mms_sensor")!!

        val sqlCalibration = " SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = $sensorID ORDER BY value_sensor "
        val alSensorValue = mutableListOf<Double?>()
        val alDataValue = mutableListOf<Double?>()

        val rs = conn.executeQuery(sqlCalibration)
        while (rs.next()) {
            alSensorValue.add(rs.getDouble(1))
            alDataValue.add(rs.getDouble(2))
        }
        rs.close()

        //--- добавим пустых полей для добавления новых калибровок
        //--- полное кол-во строк = текущему + 50% добавки пустых ( минимум 30 строк, если получается меньше )
        val nextSize = max(30, alSensorValue.size * 3 / 2)
        while (alSensorValue.size < nextSize) {
            alSensorValue.add(null)
            alDataValue.add(null)
        }

        //--- окончание нестандартной части ------------------------------------------------------------------------------------

        //--- заголовок формы
        val alHeader = mutableListOf<Pair<String, String>>()
        fillHeader(null, false, alHeader, hmOut)

        //--- основные поля - применяются сокращенные/оптимизированные варианты getFormCell
        val alFormCell = mutableListOf<FormCell>()
        for (rowIndex in alSensorValue.indices) {
            //--- Значение датчика
            val sensorValue = alSensorValue[rowIndex]
            var fci = FormCell(FormCellType.STRING).apply {
                strName = "$SENSOR_FIELD_PREFIX$rowIndex"
                strValue = if (sensorValue == null) {
                    ""
                } else if (userConfig.upIsUseThousandsDivider) {
                    getSplittedLong(sensorValue.toLong())
                } else {
                    sensorValue.toString()
                }
                strColumn = 10
                isEditable = true
                caption = " "  // совсем нулевая строка даст невидимое поле
            }
            alFormCell.add(fci)

            //--- Значение измеряемой величины
            val dataValue = alDataValue[rowIndex]
            fci = FormCell(FormCellType.STRING).apply {
                strName = "$DATA_FIELD_PREFIX$rowIndex"
                strValue = if (dataValue == null) {
                    ""
                } else {
                    getSplittedDouble(dataValue, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                }
                strColumn = 10
                isEditable = true
                caption = " "  // совсем нулевая строка даст невидимое поле
            }
            alFormCell.add(fci)
        }

        val alFormButton = mutableListOf<FormButton>()
        alFormButton.add(
            FormButton(
                url = AppParameter.setParam(
                    AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.SAVE),
                    SENSOR_ID, sensorID.toString()
                ),
                caption = model.getSaveButonCaption(aliasConfig),
                iconName = ICON_NAME_SAVE,
                withNewData = true,
                key = BUTTON_KEY_SAVE
            )
        )
        if (refererURL != null) {
            alFormButton.add(
                FormButton(
                    url = refererURL,
                    caption = "Выйти",
                    iconName = ICON_NAME_EXIT,
                    withNewData = false,
                    key = BUTTON_KEY_EXIT
                )
            )
        }

        return FormResponse(
            tab = aliasConfig.descr,
            alHeader = alHeader,
            columnCount = 2,
            alFormColumn = listOf("Значение датчика", "Значение измеряемой величины"),
            alFormCell = alFormCell,
            alFormButton = alFormButton,
        )

    }

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {

        val id = getIdFromParam()
        //--- мегаформа ввода калибровок сохраняет только при попытке их создания,
        //--- иначе запускаем обычный процесс сохранения
        if (id != 0) {
            return super.doSave(action, hmFormData, hmOut)
        }

        val sensorID = hmParam[SENSOR_ID]!!.toInt()
        //--- удалить старые записи
        conn.executeUpdate(" DELETE FROM MMS_sensor_calibration WHERE sensor_id = $sensorID ")
        //--- добавить новые
        var rowIndex = 0
        while (rowIndex < hmFormData.size / 2) {
            //--- сокращенный/оптимизированный вариант чтения из соответствующих DataInt/DataDouble
            val strSensor = hmFormData["$SENSOR_FIELD_PREFIX$rowIndex"]?.stringValue
            val strData = hmFormData["$DATA_FIELD_PREFIX$rowIndex"]?.stringValue
            rowIndex++

            //--- строки с пустыми значениями просто пропускаем
            if (strSensor.isNullOrEmpty() || strData.isNullOrEmpty()) {
                continue
            }

            val sensorValue: Double
            val dataValue: Double
            try {
                sensorValue = strSensor.replace(',', '.').replace(" ", "").toDouble()
                dataValue = strData.replace(',', '.').replace(" ", "").toDouble()
            } catch (t: Throwable) {
                continue
            }
            //--- неправильно введенные числа тоже игнорируем
            conn.executeUpdate(
                """
                    INSERT INTO MMS_sensor_calibration ( id, sensor_id , value_sensor , value_data ) VALUES (
                    ${conn.getNextIntId("MMS_sensor_calibration", "id")} , $sensorID , $sensorValue , $dataValue )
                """
            )
        }

        return AppParameter.setParam(chmSession[AppParameter.REFERER + hmParam[AppParameter.REFERER]] as String, AppParameter.ID, id.toString())
    }
}

 */