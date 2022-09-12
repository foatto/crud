package foatto.core_server.app.system

import foatto.core.link.TableResponse
import foatto.core.util.DescendingFileTimeComparator
import foatto.core.util.loadTextFile
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedResultSet
import java.io.File
import java.util.*

open class cLogText : cStandart() {

    private val pqFile = PriorityQueue(24, DescendingFileTimeComparator)
    private var logFileName: String? = null
    private var alLogRow: MutableList<String>? = null
    protected var logCurRow: String? = null

    //---------------------------------------------------------------------------------------------------------------

    //--- по умолчанию возвращаем просто соответствующую папку логов
    protected open fun getLogDir(): File? = application.hmAliasLogDir[aliasConfig.name]?.let { File(it) }

    //--- понятно что будет регулироваться правами доступа, но лишняя предосторожность не помешает
    override fun isAddEnabled() = false

    override fun isEditEnabled(hmColumnData: Map<iColumn, iData>, id: Int) = false

    override fun isDeleteEnabled(hmColumnData: Map<iColumn, iData>, id: Int) = false

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        getLogDir()?.let { dirLog ->
            if (dirLog.exists()) {
                dirLog.walkTopDown().toList().onEach { file ->
                    if (file.isFile) {
                        pqFile.offer(file)
                    }
                }
            }
        }
        return super.getTable(hmOut)
    }

    override fun isNextDataInTable(rs: CoreAdvancedResultSet?): Boolean {
        //--- возможно что нам попадётся пустой лог-файл
        while (alLogRow.isNullOrEmpty()) {
            val logFile = pqFile.poll()
            logFile?.let {
                //--- только имя лог-файла, без пути, чтобы не светить расположение логов и папки сервера
                logFileName = it.name
                alLogRow = loadTextFile(it.canonicalPath).toMutableList()
            } ?: return false
        }
        //--- сначала самые свежие строки в логе
        logCurRow = alLogRow!!.removeLast()
        return true
    }

    override fun getColumnData(rs: CoreAdvancedResultSet?, isForTable: Boolean, alColumnList: List<iColumn>): MutableMap<iColumn, iData>? {
        //--- возможные предварительные фильтрация/преобразование строки лога
        doLogRowFilter()

        return if (logCurRow == null) {
            null
        } else {
            super.getColumnData(rs, isForTable, alColumnList)
        }
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ДО фильтров поиска и страничной разбивки
    override fun generateColumnDataBeforeFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mtl = model as mLogText

        (hmColumnData[mtl.columnLogFileName] as DataString).text = logFileName ?: ""
        (hmColumnData[mtl.columnLogRow] as DataString).text = logCurRow ?: ""
    }

    //--- по умолчанию никаких дополнительных изменений/фильтраций не производим
    protected open fun doLogRowFilter() {}
}
