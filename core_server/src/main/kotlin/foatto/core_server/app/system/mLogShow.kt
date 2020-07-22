package foatto.core_server.app.system

import foatto.app.CoreSpringController
import foatto.core.link.FormPinMode
import foatto.core.util.getCurrentDayStart
import foatto.core.util.getDateTimeArray
import foatto.core.util.getNextDayStart
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

class mLogShow : mAbstract() {

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

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption(aAliasConfig: AliasConfig) = "Показать"

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val gcBeg = getCurrentDayStart(zoneId)
        val gcEnd = getNextDayStart(zoneId)

        val arrBegDT = getDateTimeArray(gcBeg)
        val arrEndDT = getDateTimeArray(gcEnd)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = FAKE_TABLE_NAME

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
        columnShowBegDate.default = LocalDate.of(arrBegDT[0], arrBegDT[1], arrBegDT[2])
        columnShowBegDate.isVirtual = true
        columnShowBegDate.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowBegTime = ColumnTime3Int(tableName, "beg_ho", "beg_mi", null, "Время начала периода")
        columnShowBegTime.default = LocalTime.of(arrBegDT[3], arrBegDT[4], arrBegDT[5])
        columnShowBegTime.isVirtual = true
        columnShowBegTime.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowBegTime.formPinMode = FormPinMode.ON

        columnShowEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
        columnShowEndDate.default = LocalDate.of(arrEndDT[0], arrEndDT[1], arrEndDT[2])
        columnShowEndDate.isVirtual = true
        columnShowEndDate.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowEndTime = ColumnTime3Int(tableName, "end_ho", "end_mi", null, "Время окончания периода")
        columnShowEndTime.default = LocalTime.of(arrEndDT[3], arrEndDT[4], arrEndDT[5])
        columnShowEndTime.isVirtual = true
        columnShowEndTime.addFormVisible(FormColumnVisibleData(columnShowRangeType, true, intArrayOf(0)))
        columnShowEndTime.formPinMode = FormPinMode.ON

        //----------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnShowRangeType)
        alFormColumn.add(columnShowBegDate)
        alFormColumn.add(columnShowBegTime)
        alFormColumn.add(columnShowEndDate)
        alFormColumn.add(columnShowEndTime)
    }
}
