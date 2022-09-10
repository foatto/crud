package foatto.sql

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getRandomInt
import foatto.core.util.getRandomLong
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.ConcurrentHashMap

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

abstract class CoreAdvancedConnection(dbConfig: DBConfig) {

    var dialect: CoreSQLDialectEnum = CoreSQLDialectEnum.POSTGRESQL
        protected set

    private val alReplicationName = mutableListOf<String>()
    private val alReplicationFilter = mutableListOf<String>()
    private var replicationFilterIsBlackList = false
    private val replicationPath: String

    private val alReplicationSQL = mutableListOf<String>()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        //--- без имён реплицируемых серверов репликация считается выключенной
        if (dbConfig.replName != null) {
            val replicationNames = dbConfig.replName.split(",", " ").filter { it.isNotBlank() }
            if (replicationNames.isNotEmpty()) {
                alReplicationName.addAll(replicationNames)
                //--- фильтр начинается с символа "!" = фильтр - чёрный список, иначе фильтр - белый список
                var dbReplicationFilter = dbConfig.replFilter
                if (!dbReplicationFilter.isNullOrEmpty()) {
                    if (dbReplicationFilter.first() == '!') {
                        dbReplicationFilter = dbReplicationFilter.substring(1)
                        replicationFilterIsBlackList = true
                    }
                    val replicationFilterTables = dbReplicationFilter.split(",", " ").filter { it.isNotBlank() }
                    alReplicationFilter.addAll(replicationFilterTables)
                }
            }
        }
        //--- в очень частных случаях (например, в утилите репликатора) нужен путь к файлам-репликам,
        //--- несмотря на то, что сам репликатор реплик не производит.
        //--- поэтому должно быть replicationPath != null
        replicationPath = dbConfig.replPath
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun executeUpdate(sql: String, withReplication: Boolean = true): Int {
        val result = executeUpdate(sql)
        if (withReplication) {
            addReplicationSQL(sql)
        }
        return result
    }

    protected abstract fun executeUpdate(sql: String): Int

    abstract fun executeQuery(sql: String): CoreAdvancedResultSet

    open fun commit() {
        //--- коммит обязательно должен быть общий, чтобы реплики не пропускались ни в коем случае
        //--- conn.commit();

        if (alReplicationName.isNotEmpty()) {
            if (alReplicationSQL.isNotEmpty()) {
                //--- один буфер на всех
                val bbOut = getReplicationData(alReplicationSQL)

                //--- для каждого репликанта
                for (name in alReplicationName) {
                    saveReplication(name, bbOut)
                }
            }
            alReplicationSQL.clear()
        }

        //--- commit делается платформозависимо в классах-наследниках
        //conn.commit();
    }

    open fun rollback() {
        alReplicationSQL.clear()
        //--- rollback делается платформозависимо в классах-наследниках
        //conn.rollback();
    }

    open fun close() {
        alReplicationSQL.clear()
        //--- close делается платформозависимо в классах-наследниках
        //conn.close();
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- различные вариации организации ограничения кол-ва возвращаемых строк
    fun getPreLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == CoreSQLDialectEnum.MSSQL) {
            sb.append(" TOP( ").append(limit).append(" ) ")
        }
        return sb
    }

    fun getMidLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == CoreSQLDialectEnum.ORACLE) {
            sb.append(" AND ROWNUM <= ").append(limit)
        }
        return sb
    }

    fun getPostLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == CoreSQLDialectEnum.H2 || dialect == CoreSQLDialectEnum.POSTGRESQL || dialect == CoreSQLDialectEnum.SQLITE) {
            sb.append(" LIMIT ").append(limit)
        }
        return sb
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun addReplicationSQL(sql: String) {
        //--- для черного списка результат по умолчанию положительный, для белого - отрицательный
        var checkResult = alReplicationFilter.isEmpty() || replicationFilterIsBlackList
        for (filter in alReplicationFilter) {
            if (sql.contains(filter)) {
                checkResult = !replicationFilterIsBlackList
                break
            }
        }
        if (checkResult) {
            alReplicationSQL.add(sql)
        }
    }

    fun saveReplication(replicationName: String, bbOut: AdvancedByteBuffer) {
        val dir = File(replicationPath, replicationName)

        var file: File
        while (true) {
            file = File(dir, getRandomInt().toString())
            if (!file.exists()) {
                break
            }
        }
        //--- сохраняем репликацию в файле
        val fileChannel = FileOutputStream(file).channel
        fileChannel.write(bbOut.buffer)
        fileChannel.close()
        //--- этот буфер может использоваться для записи для нескольких репликантов сразу
        bbOut.rewind()
    }

    fun getReplicationList(replicationName: String): SortedMap<Long, MutableList<File>> {
        val tmFile = chmReplicationFile.getOrPut(replicationName) { sortedMapOf() }
        if (tmFile.isEmpty()) {
            Files.walkFileTree(Paths.get(replicationPath, replicationName), ReplicationFileVisitor(tmFile, MAX_REPLICATION_LIST_SIZE))
        }
        return tmFile
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getNextIntId(aTableName: String, aFieldId: String): Int {
        return getNextIntId(arrayOf(aTableName), arrayOf(aFieldId))
    }

    //--- вернуть следующее уникальное значение поля среди нескольких таблиц
    fun getNextIntId(arrTableName: Array<String>, arrFieldIds: Array<String>): Int {
        var nextId: Int
        OUT@
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            for (i in arrTableName.indices) {
                if (checkExisting(arrTableName[i], arrFieldIds[i], nextId, null, 0)) {
                    continue@OUT
                }
            }
            return nextId
        }
    }

    fun getNextLongId(aTableName: String, aFieldId: String): Long {
        return getNextLongId(arrayOf(aTableName), arrayOf(aFieldId))
    }

    //--- вернуть следующее уникальное значение поля среди нескольких таблиц
    fun getNextLongId(arrTableName: Array<String>, arrFieldIds: Array<String>): Long {
        var nextId: Long
        OUT@
        while (true) {
            nextId = getRandomLong()
            if (nextId == 0L) {
                continue
            }
            for (i in arrTableName.indices) {
                if (checkExisting(arrTableName[i], arrFieldIds[i], nextId)) {
                    continue@OUT
                }
            }
            return nextId
        }
    }

    abstract fun checkExisting(aTableName: String, aFieldCheck: String, aValue: Any, aFieldID: String? = null, id: Number = 0): Boolean
    abstract fun checkExisting(aTableName: String, alFieldCheck: List<Pair<String, Any>>, aFieldID: String? = null, id: Number = 0): Boolean

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    // '${bbData.getHex( null, false )}'
    fun getHexValue(bbData: AdvancedByteBuffer): String {
        val hex = bbData.getHex(null, false)
        return when (dialect) {
            CoreSQLDialectEnum.H2 -> "X'$hex'"
            CoreSQLDialectEnum.MSSQL -> "0x$hex"
            CoreSQLDialectEnum.POSTGRESQL -> "'\\x$hex'"
            else -> "'$hex'"
        }
    }
//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class ReplicationFileVisitor(val tmFile: SortedMap<Long, MutableList<File>>, val maxSize: Int) : SimpleFileVisitor<Path>() {
        override fun visitFile(path: Path, fileAttributes: BasicFileAttributes): FileVisitResult {
            if (fileAttributes.isRegularFile) {
                //--- игнорируем (не удаляем! этот файл возможно ещё записывается!) файлы реплик с нулевым размером/длиной
                if (fileAttributes.size() > 0) {
                    val fileTime = fileAttributes.lastModifiedTime().toMillis()

                    if (tmFile.size < maxSize || fileTime <= tmFile.lastKey()) {
                        val alFile = tmFile.getOrPut(fileTime) { mutableListOf() }
                        alFile.add(path.toFile())
                    }
                    if (tmFile.size > maxSize) {
                        tmFile.remove(tmFile.lastKey())
                    }
                }
            }
            return FileVisitResult.CONTINUE
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        //--- макисмально известный размер реплики - 21 кб
        const val START_REPLICATION_SIZE = 32 * 1024

        private const val MAX_REPLICATION_LIST_SIZE = 100_000   // 1_000_000 - out of memory, 10_000 - слишком часто (каждые 50 мин)

        //--- ConcurrentHashMap - потому что для разных репликантов могут придти одновременные запросы
        //--- PriorityQueue - потому что для одного конкретного репликанта запросы идут строго последовательно
        private val chmReplicationFile = ConcurrentHashMap<String, SortedMap<Long, MutableList<File>>>()

        fun getReplicationData(alReplicationSQL: List<String>): AdvancedByteBuffer {
            val bbOut = AdvancedByteBuffer(START_REPLICATION_SIZE)
            bbOut.putInt(alReplicationSQL.size)
            for (sql in alReplicationSQL) {
                bbOut.putLongString(sql)
            }
            bbOut.flip()

            return bbOut
        }

        //--- конвертация SQL-диалектозависимых ключевых слов на другие между парой SQL-диалектов.
        //--- проверяются только ключевые слова, применяемые в системе и именно в таком виде, в котором они вводятся
        fun convertDialect(aSql: String, sourDialect: CoreSQLDialectEnum, destDialect: CoreSQLDialectEnum): String {
            var sql = aSql
            //--- одинаковые диалекты не конвертируем
            if (sourDialect == destDialect) {
                return sql
            }

            sql = sql.replace(sourDialect.integerFieldTypeName, destDialect.integerFieldTypeName)
                .replace(sourDialect.hexFieldTypeName, destDialect.hexFieldTypeName)
            //--- частный случай - убираем clustered-index только в mssql-варианте
            //--- (обязательной необходимости зеркальной операции - добавления clustered-опции - не определено)
            if (sourDialect == CoreSQLDialectEnum.MSSQL) {
                sql = sql.replace(sourDialect.createClusteredIndex, destDialect.createClusteredIndex)
            }

            return sql
        }

    }
}
