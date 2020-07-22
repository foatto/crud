package foatto.mms.core_mms

import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.LocalTime

open class mShow : mAbstract() {

    private lateinit var os: ObjectSelector
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

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption(aAliasConfig: AliasConfig): String = "Показать"

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnShowRangeType = ColumnRadioButton(tableName, "show_range_type", "За какой период показывать", 0)
        columnShowRangeType.addChoice(0, "За указанный период")
        columnShowRangeType.addChoice(15 * 60, "За последние 15 минут")
        columnShowRangeType.addChoice(30 * 60, "За последние 30 минут")
        columnShowRangeType.addChoice(60 * 60, "За последний час")
        columnShowRangeType.addChoice(2 * 60 * 60, "За последние 2 часа")
        columnShowRangeType.addChoice(3 * 60 * 60, "За последние 3 часа")
        columnShowRangeType.addChoice(6 * 60 * 60, "За последние 6 часов")
        columnShowRangeType.isVirtual = true

        columnShowBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода")
        columnShowBegDate.default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
        columnShowBegDate.isVirtual = true
        columnShowBegDate.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))

        columnShowBegTime = ColumnTime3Int(tableName, "beg_ho", "beg_mi", null, "Время начала периода")
        columnShowBegTime.default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
        columnShowBegTime.isVirtual = true
        columnShowBegTime.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowBegTime.formPinMode = FormPinMode.ON

        columnShowEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
        columnShowEndDate.default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
        columnShowEndDate.isVirtual = true
        columnShowEndDate.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))

        columnShowEndTime = ColumnTime3Int(tableName, "end_ho", "end_mi", null, "Время окончания периода")
        columnShowEndTime.default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
        columnShowEndTime.isVirtual = true
        columnShowEndTime.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowEndTime.formPinMode = FormPinMode.ON

        //----------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, true, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnShowRangeType)
        alFormColumn.add(columnShowBegDate)
        alFormColumn.add(columnShowBegTime)
        alFormColumn.add(columnShowEndDate)
        alFormColumn.add(columnShowEndTime)
    }
}
