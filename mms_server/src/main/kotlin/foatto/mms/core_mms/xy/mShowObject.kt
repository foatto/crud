package foatto.mms.core_mms.xy

import foatto.app.CoreSpringController
import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.LocalTime

class mShowObject : mAbstract() {

    lateinit var uodg: UODGSelector
        protected set
    lateinit var columnShowRangeType: ColumnRadioButton
        private set
    lateinit var columnShowBegDate: ColumnDate3Int
        private set
    lateinit var columnShowBegTime: ColumnTime3Int
        private set
    lateinit var columnShowEndDate: ColumnDate3Int
        private set
    lateinit var columnShowEndTime: ColumnTime3Int
        private set
    lateinit var columnShowZoneType: ColumnComboBox
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption(aAliasConfig: AliasConfig) = "Показать"

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val isShowObjectOnly = aliasConfig.alias == "mms_show_object"

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnShowRangeType = ColumnRadioButton(tableName, "show_range_type", "Показать траекторию", if (isShowObjectOnly) 0 else -1).apply {
            addChoice(0, "Не показывать")  // хоть и не порядку значений, зато логичнее выглядит :)
            addChoice(-1, "За указанный период")
            addChoice(5 * 60, "За последние 5 минут")
            addChoice(15 * 60, "За последние 15 минут")
            addChoice(30 * 60, "За последние 30 минут")
            addChoice(60 * 60, "За последний час")
            addChoice(2 * 60 * 60, "За последние 2 часа")
            addChoice(3 * 60 * 60, "За последние 3 часа")
            addChoice(6 * 60 * 60, "За последние 6 часов")
            isVirtual = true
        }

        columnShowBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода").apply {
            default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(-1))
        }
        columnShowBegTime = ColumnTime3Int(tableName, "beg_ho", "beg_mi", null, "Время начала периода").apply {
            default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(-1))
            formPinMode = FormPinMode.ON
        }

        columnShowEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода").apply {
            default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(-1))
        }
        columnShowEndTime = ColumnTime3Int(tableName, "end_ho", "end_mi", null, "Время окончания периода").apply {
            default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(-1))
            formPinMode = FormPinMode.ON
        }

        columnShowZoneType = ColumnComboBox(tableName, "show_zone_type", "Показывать геозоны", cShowAbstractObject.ZONE_SHOW_NONE).apply {
            addChoice(cShowAbstractObject.ZONE_SHOW_NONE, "нет")
            addChoice(cShowAbstractObject.ZONE_SHOW_ACTUAL, "актуальные")
            addChoice(cShowAbstractObject.ZONE_SHOW_ALL, "все")
            isVirtual = true
            setSavedDefault(userConfig)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        //----------------------------------------------------------------------------------------------------------------------

        (if (isShowObjectOnly) alFormHiddenColumn else alFormColumn).add(columnShowRangeType)
        (if (isShowObjectOnly) alFormHiddenColumn else alFormColumn).add(columnShowBegDate)
        (if (isShowObjectOnly) alFormHiddenColumn else alFormColumn).add(columnShowBegTime)
        (if (isShowObjectOnly) alFormHiddenColumn else alFormColumn).add(columnShowEndDate)
        (if (isShowObjectOnly) alFormHiddenColumn else alFormColumn).add(columnShowEndTime)
        alFormColumn.add(columnShowZoneType)
    }
}
