package foatto.server.service.report

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.util.getCurrentTimeInt
import foatto.core_mms.AppModuleMMS
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.CalcService
import foatto.server.service.FileStoreService
import foatto.server.service.ObjectService
import jakarta.persistence.EntityManager
import jxl.write.WritableSheet
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SummaryReportService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
) : AbstractPeriodSummaryService(
    calcService,
    fileStoreService,
) {

    companion object {
        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_OBJECT_NAME = "obj.name"
        private const val FIELD_OBJECT_MODEL = "obj.model"

        private const val FIELD_BEGIN_DATE_TIME = "_begin_date_time"
        private const val FIELD_END_DATE_TIME = "_end_date_time"

        private const val FIELD_KEEP_PLACE_FOR_COMMENT = "_keep_place_for_comment"
        private const val FIELD_OUT_LIQUID_LEVEL_MAIN_CONTAINER_USING = "_out_liquid_level_main_container_using"
        private const val FIELD_OUT_TEMPERATURE = "_out_temperature"
        private const val FIELD_OUT_DENSITY = "_out_density"
        private const val FIELD_OUT_TROUBLES = "_out_troubles"

        private const val FIELD_OUT_GROUP_SUM = "_out_group_sum"
        private const val FIELD_SUM_ONLY = "_sum_only"
        private const val FIELD_SUM_USER = "_sum_user"
        private const val FIELD_SUM_OBJECT = "_sum_object"
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> {

        val formCells = mutableListOf<FormBaseCell>()

        /*
                //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
                val arrDT = MMSFunction.getDayShiftWorkParent(conn, zoneId, hmParentData, false)
         */
        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId ?: 0
        } else {
            0
        }
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)

        formCells += FormSimpleCell(
            name = FIELD_OBJECT_ID,
            caption = "",
            isEditable = false,
            value = parentObjectEntity?.id?.toString() ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_NAME,
            caption = "Наименование",
            isEditable = false,
            value = parentObjectEntity?.name ?: "",
            selectorAction = AppAction(
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
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_OBJECT_MODEL,
            caption = "Модель",
            isEditable = false,
            value = parentObjectEntity?.model ?: "",
        )

        formCells += FormDateTimeCell(
            name = FIELD_BEGIN_DATE_TIME,
            caption = "Дата/время начала периода",
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.begTime,
        )
        formCells += FormDateTimeCell(
            name = FIELD_END_DATE_TIME,
            caption = "Дата/время окончания периода",
            isEditable = true,
            mode = FormDateTimeCellMode.DMYHMS,
            value = action.endTime
        )

//!!!             setSavedDefault(userConfig)
        formCells += FormBooleanCell(
            name = FIELD_KEEP_PLACE_FOR_COMMENT,
            caption = "Оставлять место под комментарии",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_OUT_LIQUID_LEVEL_MAIN_CONTAINER_USING,
            caption = "Выводить показания расхода основных ёмкостей",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_OUT_TEMPERATURE,
            caption = "Выводить показания температуры",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_OUT_DENSITY,
            caption = "Выводить показания плотности",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_OUT_TROUBLES,
            caption = "Выводить неисправности",
            isEditable = true,
            value = false,
        )

        formCells += FormBooleanCell(
            name = FIELD_OUT_GROUP_SUM,
            caption = "Выводить суммы по группам",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_SUM_ONLY,
            caption = "Выводить только суммы",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_SUM_USER,
            caption = "Выводить суммы по владельцам",
            isEditable = true,
            value = false,
        )
        formCells += FormBooleanCell(
            name = FIELD_SUM_OBJECT,
            caption = "Выводить суммы по объектам",
            isEditable = true,
            value = false,
        )

        return formCells
    }

    override fun getReport(
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>,
        sheet: WritableSheet,
    ) {
        val parentObjectId = formActionData[FIELD_OBJECT_ID]?.stringValue?.toIntOrNull() ?: 0
        val parentObjectEntity = objectRepository.findByIdOrNull(parentObjectId)!!

//!!! выровнять дату/время по началу суток
        val begDateTime = formActionData[FIELD_BEGIN_DATE_TIME]?.dateTimeValue ?: (getCurrentTimeInt() - 86_400)
        val endDateTime = formActionData[FIELD_END_DATE_TIME]?.dateTimeValue ?: getCurrentTimeInt()

        val keepPlaceForComment = formActionData[FIELD_KEEP_PLACE_FOR_COMMENT]?.booleanValue ?: false
        /*
        private const val FIELD_OUT_LIQUID_LEVEL_MAIN_CONTAINER_USING = "_out_liquid_level_main_container_using"
        private const val FIELD_OUT_TEMPERATURE = "_out_temperature"
        private const val FIELD_OUT_DENSITY = "_out_density"
        private const val FIELD_OUT_TROUBLES = "_out_troubles"

        private const val FIELD_OUT_GROUP_SUM = "_out_group_sum"
        private const val FIELD_SUM_ONLY = "_sum_only"
        private const val FIELD_SUM_USER = "_sum_user"
        private const val FIELD_SUM_OBJECT = "_sum_object"
         */

    }

}