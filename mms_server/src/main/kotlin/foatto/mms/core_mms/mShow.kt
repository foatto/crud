package foatto.mms.core_mms

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

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        val arrDT = MMSFunction.getDayShiftWorkParent(stm, zoneId, hmParentData, false)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

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

        alFormHiddenColumn.add(columnId)

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

        alFormColumn.add(columnShowRangeType)
        alFormColumn.add(columnShowBegDate)
        alFormColumn.add(columnShowBegTime)
        alFormColumn.add(columnShowEndDate)
        alFormColumn.add(columnShowEndTime)
    }
}
