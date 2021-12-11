package foatto.ts.core_ts

import foatto.core.link.FormPinMode
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
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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

    protected var useLastTimeOnly = false

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption(aAliasConfig: AliasConfig): String = "Показать"

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val today = ZonedDateTime.now()
        val zdtBeg = ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, zoneId)
        val zdtEnd = ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS)
        val arrDT = intArrayOf(
            zdtBeg.year, zdtBeg.monthValue, zdtBeg.dayOfMonth, zdtBeg.hour, zdtBeg.minute, zdtBeg.second,
            zdtEnd.year, zdtEnd.monthValue, zdtEnd.dayOfMonth, zdtEnd.hour, zdtEnd.minute, zdtEnd.second
        )

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "TS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnShowRangeType = ColumnRadioButton(
            aTableName = modelTableName,
            aFieldName = "show_range_type",
            aCaption = "За какой период показывать",
            aDefaultValue = if (useLastTimeOnly) {
                24 * 60 * 60
            } else {
                0
            }
        ).apply {
            if (!useLastTimeOnly) {
                addChoice(0, "За указанный период")
            }
            addChoice(15 * 60, "За последние 15 минут")
            addChoice(30 * 60, "За последние 30 минут")
            addChoice(60 * 60, "За последний час")
            addChoice(2 * 60 * 60, "За последние 2 часа")
            addChoice(3 * 60 * 60, "За последние 3 часа")
            addChoice(6 * 60 * 60, "За последние 6 часов")
            addChoice(24 * 60 * 60, "За последние сутки")
            isVirtual = true
        }

        columnShowBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода").apply {
            default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
        }

        columnShowBegTime = ColumnTime3Int(modelTableName, "beg_ho", "beg_mi", null, "Время начала периода").apply {
            default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
            formPinMode = FormPinMode.ON
        }

        columnShowEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Дата окончания периода").apply {
            default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
        }

        columnShowEndTime = ColumnTime3Int(modelTableName, "end_ho", "end_mi", null, "Время окончания периода").apply {
            default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
            isVirtual = true
            addFormVisible(columnShowRangeType, true, setOf(0))
            formPinMode = FormPinMode.ON
        }

        //----------------------------------------------------------------------------------------------------

        alFormHiddenColumn += columnID

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = -1
        )

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn += columnShowRangeType
        if (useLastTimeOnly) {
            alFormHiddenColumn += columnShowBegDate
            alFormHiddenColumn += columnShowBegTime
            alFormHiddenColumn += columnShowEndDate
            alFormHiddenColumn += columnShowEndTime
        } else {
            alFormColumn += columnShowBegDate
            alFormColumn += columnShowBegTime
            alFormColumn += columnShowEndDate
            alFormColumn += columnShowEndTime
        }
    }
}
