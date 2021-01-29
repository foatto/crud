package foatto.sql

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getRandomInt
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

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

abstract class CoreAdvancedConnection(dbConfig: DBConfig) {

    var dialect: SQLDialect = SQLDialect.POSTGRESQL
        protected set

    private val alReplicationName = mutableListOf<String>()
    private val alReplicationFilter = mutableListOf<String>()
    private var replicationFilterIsBlackList = false
    private val replicationPath: String

    private val alReplicationSQL = mutableListOf<String>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        var dbReplicationFilter = dbConfig.replFilter
        //--- без имён реплицируемых серверов репликация считается выключенной
        if (dbConfig.replName != null) {
            val stRN = StringTokenizer(dbConfig.replName, ", ")
            if (stRN.hasMoreTokens()) {
                while (stRN.hasMoreTokens())
                    alReplicationName.add(stRN.nextToken())
                //--- фильтр начинается с символа "!" = фильтр - чёрный список, иначе фильтр - белый список
                if (dbReplicationFilter != null && !dbReplicationFilter.isEmpty()) {
                    if (dbReplicationFilter.first() == '!') {
                        dbReplicationFilter = dbReplicationFilter.substring(1)
                        replicationFilterIsBlackList = true
                    }
                    val stRF = StringTokenizer(dbReplicationFilter, ", ")
                    while (stRF.hasMoreTokens()) alReplicationFilter.add(stRF.nextToken())
                }
            }
        }
        //--- в очень частных случаях (например, в утилите репликатора) нужен путь к файлам-репликам,
        //--- несмотря на то, что сам репликатор реплик не производит.
        //--- поэтому должно быть replicationPath != null
        replicationPath = dbConfig.replPath
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    abstract fun createStatement(): CoreAdvancedStatement

    open fun commit() {
        //--- коммит обязательно должен быть общий, чтобы реплики не пропускались ни в коем случае
        //--- conn.commit();

        if (!alReplicationSQL.isEmpty()) {
            //--- один буфер на всех
            val bbOut = getReplicationData(alReplicationSQL)

            //--- для каждого репликанта
            for (name in alReplicationName)
                saveReplication(name, bbOut)
        }
        alReplicationSQL.clear()

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

    //--- различные вариации организации ограничения кол-ва возвращаемых строк
    fun getPreLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == SQLDialect.MSSQL)
            sb.append(" TOP( ").append(limit).append(" ) ")
        return sb
    }

    fun getMidLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == SQLDialect.ORACLE)
            sb.append(" AND ROWNUM <= ").append(limit)
        return sb
    }

    fun getPostLimit(limit: Int): StringBuilder {
        val sb = StringBuilder()
        if (dialect == SQLDialect.H2 || dialect == SQLDialect.POSTGRESQL || dialect == SQLDialect.SQLITE)
            sb.append(" LIMIT ").append(limit)
        return sb
    }

    fun addReplicationSQL(sql: String) {
        //--- для черного списка результат по умолчанию положительный, для белого - отрицательный
        var checkResult = alReplicationFilter.isEmpty() || replicationFilterIsBlackList
        for (filter in alReplicationFilter)
            if (sql.contains(filter)) {
                checkResult = !replicationFilterIsBlackList
                break
            }
        if (checkResult)
            alReplicationSQL.add(sql)
    }

    fun saveReplication(replicationName: String, bbOut: AdvancedByteBuffer) {
        val dir = File(replicationPath, replicationName)

        var file: File
        while (true) {
            file = File(dir, getRandomInt().toString())
            if (!file.exists()) break
        }
        //--- сохраняем репликацию в файле
        val fileChannel = FileOutputStream(file).channel
        fileChannel.write(bbOut.buffer)
        fileChannel.close()
        //--- этот буфер может использоваться для записи для нескольких репликантов сразу
        bbOut.rewind()
    }

    fun getReplicationList(replicationName: String): TreeMap<Long, MutableList<File>> {
        var tmFile: TreeMap<Long, MutableList<File>>? = chmReplicationFile[replicationName]
        if (tmFile == null) {
            tmFile = TreeMap()
            chmReplicationFile[replicationName] = tmFile
        }
        if (tmFile.isEmpty()) {
            Files.walkFileTree(Paths.get(replicationPath, replicationName), ReplicationFileVisitor(tmFile, MAX_REPLICATION_LIST_SIZE))
        }
        return tmFile
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private class ReplicationFileVisitor(val tmFile: TreeMap<Long, MutableList<File>>, val maxSize: Int) : SimpleFileVisitor<Path>() {
        override fun visitFile(path: Path, fileAttributes: BasicFileAttributes): FileVisitResult {
            if (fileAttributes.isRegularFile) {
                //--- игнорируем (не удаляем! этот файл возможно ещё записывается!) файлы реплик с нулевым размером/длиной
                if (fileAttributes.size() > 0) {
                    val fileTime = fileAttributes.lastModifiedTime().toMillis()

                    if (tmFile.size < maxSize || fileTime <= tmFile.lastKey()) {
                        var alFile = tmFile[fileTime]
                        if (alFile == null) {
                            alFile = ArrayList()
                            tmFile[fileTime] = alFile
                        }
                        alFile.add(path.toFile())
                    }
                    if (tmFile.size > maxSize) tmFile.remove(tmFile.lastKey())
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
        private val chmReplicationFile = ConcurrentHashMap<String, TreeMap<Long, MutableList<File>>>()

        fun getReplicationData(alReplicationSQL: List<String>): AdvancedByteBuffer {
            val bbOut = AdvancedByteBuffer(START_REPLICATION_SIZE)
            bbOut.putInt(alReplicationSQL.size)
            for (sql in alReplicationSQL) bbOut.putLongString(sql)
            bbOut.flip()

            return bbOut
        }

        //--- конвертация SQL-диалектозависимых ключевых слов на другие между парой SQL-диалектов.
        //--- проверяются только ключевые слова, применяемые в системе и именно в таком виде, в котором они вводятся
        fun convertDialect(aSql: String, sourDialect: SQLDialect, destDialect: SQLDialect): String {
            var sql = aSql
            //--- одинаковые диалекты не конвертируем
            if (sourDialect == destDialect) return sql

            sql = sql.replace(sourDialect.integerFieldTypeName, destDialect.integerFieldTypeName)
                .replace(sourDialect.hexFieldTypeName, destDialect.hexFieldTypeName)
            //--- частный случай - убираем clustered-index только в mssql-варианте
            //--- (обязательной необходимости зеркальной операций - добавления clustered-опции - не определено)
            if (sourDialect == SQLDialect.MSSQL) sql = sql.replace(sourDialect.createClusteredIndex, destDialect.createClusteredIndex)

            return sql
        }

    }
}
