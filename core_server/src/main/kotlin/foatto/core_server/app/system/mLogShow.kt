package foatto.core_server.app.system

import foatto.core.link.FormPinMode
import foatto.core.util.getCurrentDayStart
import foatto.core.util.getDateTimeArray
import foatto.core.util.getNextDayStart
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
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
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val gcBeg = getCurrentDayStart(zoneId)
        val gcEnd = getNextDayStart(zoneId)

        val arrBegDT = getDateTimeArray(gcBeg)
        val arrEndDT = getDateTimeArray(gcEnd)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = FAKE_TABLE_NAME

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnShowRangeType = ColumnRadioButton(modelTableName, "show_range_type", "За какой период показывать", 0).apply {
            addChoice(0, "За указанный период")
            addChoice(15 * 60, "За последние 15 минут")
            addChoice(30 * 60, "За последние 30 минут")
            addChoice(60 * 60, "За последний час")
            addChoice(2 * 60 * 60, "За последние 2 часа")
            addChoice(3 * 60 * 60, "За последние 3 часа")
            addChoice(6 * 60 * 60, "За последние 6 часов")
            isVirtual = true
        }

        columnShowBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода").apply {
            default = LocalDate.of(arrBegDT[0], arrBegDT[1], arrBegDT[2])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
        }
        columnShowBegTime = ColumnTime3Int(modelTableName, "beg_ho", "beg_mi", null, "Время начала периода").apply {
            default = LocalTime.of(arrBegDT[3], arrBegDT[4], arrBegDT[5])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
            formPinMode = FormPinMode.ON
        }

        columnShowEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Дата окончания периода").apply {
            default = LocalDate.of(arrEndDT[0], arrEndDT[1], arrEndDT[2])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
        }
        columnShowEndTime = ColumnTime3Int(modelTableName, "end_ho", "end_mi", null, "Время окончания периода").apply {
            default = LocalTime.of(arrEndDT[3], arrEndDT[4], arrEndDT[5])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
            formPinMode = FormPinMode.ON
        }

        //----------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add(columnShowRangeType)
        alFormColumn.add(columnShowBegDate)
        alFormColumn.add(columnShowBegTime)
        alFormColumn.add(columnShowEndDate)
        alFormColumn.add(columnShowEndTime)
    }
}
