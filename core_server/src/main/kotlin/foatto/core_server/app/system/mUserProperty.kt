package foatto.core_server.app.system

import foatto.core.app.UP_DECIMAL_DIVIDER
import foatto.core.app.UP_IS_USE_THOUSANDS_DIVIDER
import foatto.core.app.UP_TIME_OFFSET
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mUserProperty : mAbstract() {

    private val arrTZOffset = arrayOf(
        Pair(0, "UTC+00:00"),
        Pair(1 * 60 * 60, "UTC+01:00"),
        Pair(2 * 60 * 60, "UTC+02:00"),
        Pair(3 * 60 * 60, "UTC+03:00"),
        Pair(4 * 60 * 60, "UTC+04:00"),
        Pair(5 * 60 * 60, "UTC+05:00"),
        Pair(6 * 60 * 60, "UTC+06:00"),
        Pair(7 * 60 * 60, "UTC+07:00"),
        Pair(8 * 60 * 60, "UTC+08:00"),
        Pair(9 * 60 * 60, "UTC+09:00"),
        Pair(10 * 60 * 60, "UTC+10:00"),
        Pair(11 * 60 * 60, "UTC+11:00"),
        Pair(12 * 60 * 60, "UTC+12:00"),
    )

    lateinit var columnTimeShift: ColumnComboBox
    lateinit var columnDivideThousands: ColumnBoolean
    lateinit var columnDividerChar: ColumnRadioButton

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = FAKE_TABLE_NAME

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id").apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnTimeShift = ColumnComboBox(modelTableName, UP_TIME_OFFSET, "Часовой пояс").apply {
            isVirtual = true
            arrTZOffset.forEach { (offset, descr) ->
                addChoice(offset, descr)
            }
        }

        columnDivideThousands = ColumnBoolean(modelTableName, UP_IS_USE_THOUSANDS_DIVIDER, "Разделять тысячи пробелами").apply {
            isVirtual = true
        }

        columnDividerChar = ColumnRadioButton(modelTableName, UP_DECIMAL_DIVIDER, "Разделитель дробной части").apply {
            isVirtual = true
            addChoice(0, "Точка")
            addChoice(1, "Запятая")
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnTimeShift)
        alFormColumn.add(columnDivideThousands)
        alFormColumn.add(columnDividerChar)
    }
}