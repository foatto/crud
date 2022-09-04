package foatto.core_server.app.server

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnSimple
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.column.iUniqableColumn
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId
import java.util.*
import kotlin.math.max

abstract class mAbstract {

    companion object {
        const val ALIAS_NAME_ARCHIVE_POSTFIX = "_archive"
        const val FAKE_TABLE_NAME = "-"
        const val STRING_COLUMN_WIDTH = 40
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var stm: CoreAdvancedStatement
    protected lateinit var hmParam: Map<String, String>

    lateinit var modelTableName: String
        protected set
    lateinit var columnId: ColumnInt

    //--- задается поле для userID, если есть (может быть ColumnInt (вместе с селектором) или ColumnComboBox для частных случаев)
    var columnUser: ColumnSimple? = null

    //--- поля для поддержки архивности
    var isArchiveAlias: Boolean = false
    var columnActive: ColumnBoolean? = null
    var columnArchive: ColumnBoolean? = null

    //--- поля для поддержки версионности
    var columnVersionId: ColumnInt? = null
    var columnVersionNo: ColumnString? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    val alUniqueColumnData = mutableListOf<List<UniqueColumnData>>()

    val hsAdditionalTables = mutableSetOf<String>()   // вручную добавленные имена таблиц для запроса (бывает нужно для сетевых структур)

    val alTableHiddenColumn = mutableListOf<iColumn>()
    val alTableGroupColumn = mutableListOf<iColumn>()

    //--- работать будем исключительно через get/set-методы, ибо напрямую неудобно - есть нюансы доступа
    private val tmTableColumn = TreeMap<Int, TreeMap<Int, iColumn>>()

    val alTableSort = mutableListOf<TableSortData>()

    val alFormHiddenColumn = mutableListOf<iColumn>()
    val alFormColumn = mutableListOf<iColumn>()

    val hmParentColumn = mutableMapOf<String, iColumn>()

    val alChildData = mutableListOf<ChildData>()

    val alDependData = mutableListOf<DependData>()

    var expandParentIDColumn: ColumnInt? = null
    var expandParentNameColumn: ColumnString? = null

    protected lateinit var zoneId: ZoneId

    protected var textFieldMaxSize = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {
        stm = aStm

        isArchiveAlias = aliasConfig.alias.endsWith(ALIAS_NAME_ARCHIVE_POSTFIX)

        zoneId = userConfig.upZoneId

        hmParam = aHmParam

        textFieldMaxSize = stm.dialect.textFieldMaxSize
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun isUseParentUserId(): Boolean = false
    open fun isExpandable(): Boolean = false

    open fun getAddAlertTag(): String? = null
    open fun getEditAlertTag(): String? = null
    open fun isUniqueAlertRowID(): Boolean = false
    open fun getDateTimeColumns(): Array<iColumn>? = null

    open fun getAdditionalTables(hsTableRenameList: MutableSet<String>): MutableSet<String> = hsAdditionalTables

    open fun getSaveButonCaption(aAliasConfig: AliasConfig): String = "Сохранить"

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- сколько всего физических строк в этой логической строке
    fun getTableColumnRowCount(): Int {
        var lastRow = 0
        for ((row, tmRow) in tmTableColumn) {
            for (column in tmRow.values) {
                lastRow = max(lastRow, row + column.rowSpan - 1)
            }
        }
        return lastRow + 1
    }

    //--- сколько всего столбцов - новая версия с учётом colSpan
    fun getTableColumnColCount(): Int {
        var lastCol = 0
        for (tmRow in tmTableColumn.values) {
            for ((col, column) in tmRow) {
                lastCol = max(lastCol, col + column.colSpan - 1)
            }
        }
        return lastCol + 1
    }

    //--- сколько логических столбцов в заданном столбце
    fun getTableColumnColCount(colNo: Int): Int {
        var colCount = 0
        for (tmRow in tmTableColumn.values) {
            for ((col, column) in tmRow) {
                if (colNo >= col && colNo <= col + column.colSpan - 1) {
                    colCount++
                }
            }
        }
        return colCount
    }

    fun getTableColumn(row: Int, col: Int): iColumn? {
        val tmRow = tmTableColumn[row]
        return if (tmRow == null) {
            null
        } else {
            tmRow[col]
        }
    }

    fun fillTableColumn(alColumnList: MutableList<iColumn>) {
        for (tmRow in tmTableColumn.values) {
            alColumnList.addAll(tmRow.values)
        }
    }

    //--- Обычное/типовое добавление столбцов для типовых применений - в последнюю строку, вслед за последним столбцом в этой строке.
    //--- Более упрощённый/специализированный/быстрый вариант, чем addTableColumnHorizAdd:
    //--- - при выборе текущей/последней строки не учитывает rowSpan предыдущих строк
    fun addTableColumn(vararg arrColumn: iColumn): Pair<Int, Int> {
        val row: Int
        val col: Int

        val tmRow: TreeMap<Int, iColumn>
        if (tmTableColumn.isEmpty()) {
            tmRow = TreeMap()
            row = 0
            tmTableColumn[row] = tmRow
        } else {
            row = tmTableColumn.lastKey()
            tmRow = tmTableColumn[row]!!
        }

        if (tmRow.isEmpty()) {
            col = 0
        } else {
            val lastKey = tmRow.lastKey()!!
            col = lastKey + tmRow[lastKey]!!.colSpan
        }
        for (i in arrColumn.indices) {
            tmRow[col + i] = arrColumn[i]
        }
        return Pair(row, col)
    }

    //--- добавление столбцов с новой строки
    fun addTableColumnHorizNew(vararg arrColumn: iColumn): Int {
        val row = getTableColumnRowCount()
        addTableColumnHoriz(row, 0, *arrColumn)
        return row
    }

    //--- добавление столбцов с нового столбца
    fun addTableColumnVertNew(vararg arrColumn: iColumn): Int {
        val col = getTableColumnColCount()
        addTableColumnVert(0, col, *arrColumn)
        return col
    }

    //--- добавление столбцов в последнюю существующую строку или в новую, если строк ещё нет
    //--- (с учётом rowSpan - т.е. ячейки начнут добавляться после этого поля)
    fun addTableColumnHorizAdd(vararg arrColumn: iColumn): Int {
        var lastRow = 0
        var lastCol = 0
        for ((row, tmRow) in tmTableColumn) {
            for ((col, column) in tmRow) {
                val lr = row + column.rowSpan - 1
                //--- нам нужен _последний/обновлённый_ максимум, поэтому используем >=
                if (lr >= lastRow) {
                    lastRow = lr
                    lastCol = col
                }
            }
        }
        addTableColumnVert(lastRow, lastCol + 1, *arrColumn)
        return lastCol + 1
    }

    //--- добавление столбцов в последний существующий столбец или в новый, если столбцов ещё нет
    //--- (с учётом colSpan - т.е. ячейки начнут добавляться после этого поля)
    fun addTableColumnVertAdd(vararg arrColumn: iColumn): Int {
        var lastRow = 0
        var lastCol = 0
        for ((row, tmRow) in tmTableColumn) {
            for ((col, column) in tmRow) {
                val lc = col + column.colSpan - 1
                //--- нам нужен _последний/обновлённый_ максимум, поэтому используем >=
                if (lc >= lastCol) {
                    lastRow = row
                    lastCol = lc
                }
            }
        }
        addTableColumnVert(lastRow + 1, lastCol, *arrColumn)
        return lastRow + 1
    }

    //--- добавление столбцов с указанными стартовыми координатами в указанном направлении
    private fun addTableColumnHoriz(row: Int, col: Int, vararg arrColumn: iColumn) {
        var tmRow: TreeMap<Int, iColumn>? = tmTableColumn[row]
        //--- возможное добавление/вставка новой строки
        if (tmRow == null) {
            tmRow = TreeMap()
            tmTableColumn[row] = tmRow
        }
        for (i in arrColumn.indices) {
            tmRow[col + i] = arrColumn[i]
        }
    }

    private fun addTableColumnVert(row: Int, col: Int, vararg arrColumn: iColumn) {
        for (i in arrColumn.indices) {
            var tmRow: TreeMap<Int, iColumn>? = tmTableColumn[row + i]
            //--- возможное добавление/вставка новой строки
            if (tmRow == null) {
                tmRow = TreeMap()
                tmTableColumn[row + i] = tmRow
            }
            tmRow[col] = arrColumn[i]
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun addUniqueColumn(column: iUniqableColumn, ignore: Any? = null) {
        addUniqueColumn(UniqueColumnData(column, ignore))
    }

    protected fun addUniqueColumn(uniqueColumnData: UniqueColumnData) {
        alUniqueColumnData += listOf(uniqueColumnData)
    }

    protected fun addUniqueColumn(columnList: List<UniqueColumnData>) {
        alUniqueColumnData += columnList
    }

    protected fun addTableSort(column: iColumn, direct: Boolean) {
        alTableSort += TableSortData(column, direct)
    }

}
