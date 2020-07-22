package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core.util.getDateTimeArray
import foatto.core.util.clearOldFiles
import foatto.core.util.clearStorage
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.SQLDialect
import java.io.File
import java.time.ZoneId
import java.util.*
import kotlin.math.max

abstract class CoreDataClean(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private val CONFIG_DB_EXPIRE_PERIOD_ = "db_expire_period_"

        private val CONFIG_PATH_ = "path_"

        private val CONFIG_STORAGE_ = "storage_"
        private val CONFIG_STORAGE_SPACE_ = "storage_space_"
        private val CONFIG_STORAGE_DELETE_EMPTY_DIR_ = "storage_delete_empty_dir_"

        private val CONFIG_STORAGE_EXT = "storage_ext"
    }

    //----------------------------------------------------------------------------------------------------------------------

    private val alExpirePeriod = mutableListOf<Int>()
    private val alPath = mutableListOf<File>()
    private val alStorage = mutableListOf<TreeSet<File>>()
    private val alStorageSpaceByte = mutableListOf<Long>()
    private val alStorageSpacePercent = mutableListOf<Int>()
    private val alStorageDeleteEmptyDir = mutableListOf<Boolean>()
    private val hsStorageExt = mutableSetOf<String>()

    //----------------------------------------------------------------------------------------------------------------------

    private val zoneId = ZoneId.systemDefault()

    override val isRunOnce: Boolean
        get() = true

    override fun loadConfig() {
        super.loadConfig()


        for(i in alDBConfig.indices) alExpirePeriod.add(hmConfig[CONFIG_DB_EXPIRE_PERIOD_ + i]!!.toInt() * 7 * 24 * 60 * 60)

        var index = 0
        while(true) {
            val path = hmConfig[CONFIG_PATH_ + index] ?: break

            alPath.add(File(path))

            index++
        }

        index = 0
        while(true) {
            val sStorage = hmConfig[CONFIG_STORAGE_ + index] ?: break

            val tsFile = TreeSet<File>()
            //--- специально без деления по пробелу, чтобы принимать пути с пробелами
            var st = StringTokenizer(sStorage, ",;")
            while(st.hasMoreTokens()) tsFile.add(File(st.nextToken().trim { it <= ' ' }))
            alStorage.add(tsFile)

            st = StringTokenizer(hmConfig[CONFIG_STORAGE_SPACE_ + index], ",; ")
            alStorageSpaceByte.add(st.nextToken().toLong() * 1024 * 1024)
            alStorageSpacePercent.add(if(st.hasMoreTokens()) st.nextToken().toInt() else 0)

            alStorageDeleteEmptyDir.add(hmConfig[CONFIG_STORAGE_DELETE_EMPTY_DIR_ + index] != "0")

            index++
        }

        val st = StringTokenizer(hmConfig[CONFIG_STORAGE_EXT], ",; ")
        while(st.hasMoreTokens()) hsStorageExt.add(st.nextToken().toLowerCase())
    }

    override fun cycle() {
        //--- загрузка списка обрабатываемых в этом цикле а/м
        val hmObject = mutableMapOf<Int, String>()
        val rsObject = alStm[0].executeQuery(" SELECT id , name FROM MMS_object WHERE id <> 0 ")
        while(rsObject.next()) hmObject[rsObject.getInt(1)] = rsObject.getString(2)
        rsObject.close()

        //--- полный срок хранения: expire_period считается в неделях
        val alExpireTime = mutableListOf<Int>()
        var maxExpirePeriod = 0
        for(i in alDBConfig.indices) {
            alExpireTime.add((getCurrentTimeInt() - alExpirePeriod[i]).toInt())
            maxExpirePeriod = max(maxExpirePeriod, alExpirePeriod[i])
        }

        //--- теперь по каждому объекту
        var rowSum = 0
        for(objectID in hmObject.keys) {
            //--- стираем старые данные
            var row = 0
            for(i in alDBConfig.indices) {
                row += alStm[i].executeUpdate( " DELETE FROM MMS_data_$objectID WHERE ontime < ${alExpireTime[i]} " )
                alConn[i].commit()
            }

            //--- для H2 базы команды ALTER INDEX не реализовано
            if(alConn[0].dialect == SQLDialect.H2) {
            }
            else if(alConn[0].dialect == SQLDialect.SQLITE) {
            }
            else if(alConn[0].dialect == SQLDialect.POSTGRESQL) for(i in alDBConfig.indices) {
                alStm[i].executeUpdate( " CLUSTER MMS_data_$objectID USING MMS_data_${objectID}_ontime ")
                alConn[i].commit()
            }
            else for(i in alDBConfig.indices) {
                alStm[i].executeUpdate( " ALTER INDEX ALL ON MMS_data_$objectID REBUILD ")
                alConn[i].commit()
            }//--- у прочих диалектов просто перестраиваем индексы (где была кластерность - там она сохраняется)
            //--- для MMS_data_NNN в PostgreSQL периодически делаем специфическую "кластерную" переиндексацию,
            //--- т.к. она не сохраняется по времени
            //--- для SQLite базы команды ALTER INDEX не реализовано

            AdvancedLogger.debug( "MMS_data_$objectID: ${hmObject[objectID]} = $row rows")
            rowSum += row
        }
        AdvancedLogger.debug( "MMS_data_ALL = $rowSum rows")

        //--- стираем старые суточные работы, журнал простоев и путевые листы/смены

        val arrDT = getDateTimeArray(zoneId, getCurrentTimeInt() - maxExpirePeriod)

        var rowCountDW = alStm[0].executeUpdate(
            " DELETE FROM MMS_day_work WHERE ye < ${arrDT[0]} OR ye = ${arrDT[0]} AND mo < ${arrDT[1]} OR ye = ${arrDT[0]} AND mo = ${arrDT[1]} AND da < ${arrDT[2]} " )
        alConn[0].commit()

        //--- для H2 базы команды ALTER INDEX не реализовано
        if(alConn[0].dialect == SQLDialect.H2) {
        }
        else if(alConn[0].dialect == SQLDialect.SQLITE) {
        }
        else if(alConn[0].dialect == SQLDialect.POSTGRESQL) alStm[0].executeUpdate(" REINDEX TABLE MMS_day_work ")
        else alStm[0].executeUpdate(" ALTER INDEX ALL ON MMS_day_work REBUILD ")//--- у прочих диалектов просто перестраиваем индексы
        //--- для PostgreSQL свой синтаксис
        //--- для SQLite базы команды ALTER INDEX не реализовано
        alConn[0].commit()
        AdvancedLogger.debug("MMS_day_work = $rowCountDW rows")

        rowCountDW = alStm[0].executeUpdate(
            " DELETE FROM MMS_downtime WHERE ye < ${arrDT[0]} OR ye = ${arrDT[0]} AND mo < ${arrDT[1]} OR ye = ${arrDT[0]} AND mo = ${arrDT[1]} AND da < ${arrDT[2]} " )
        alConn[0].commit()

        //--- для H2 базы команды ALTER INDEX не реализовано
        if(alConn[0].dialect == SQLDialect.H2) {
        }
        else if(alConn[0].dialect == SQLDialect.SQLITE) {
        }
        else if(alConn[0].dialect == SQLDialect.POSTGRESQL) alStm[0].executeUpdate(" REINDEX TABLE MMS_downtime ")
        else alStm[0].executeUpdate(" ALTER INDEX ALL ON MMS_downtime REBUILD ")//--- у прочих диалектов просто перестраиваем индексы
        //--- для PostgreSQL свой синтаксис
        //--- для SQLite базы команды ALTER INDEX не реализовано
        alConn[0].commit()
        AdvancedLogger.debug("MMS_downtime = $rowCountDW rows")

        val rowCountWS = alStm[0].executeUpdate( " DELETE FROM MMS_work_shift WHERE end_dt < ${getCurrentTimeInt() - maxExpirePeriod}" )
        if(rowCountWS > 0) alStm[0].executeUpdate( " DELETE FROM MMS_work_shift_data WHERE shift_id NOT IN ( SELECT id FROM MMS_work_shift ) " )
        alConn[0].commit()

        //--- для H2 базы команды ALTER INDEX не реализовано
        if(alConn[0].dialect == SQLDialect.H2) {
        }
        else if(alConn[0].dialect == SQLDialect.SQLITE) {
        }
        else if(alConn[0].dialect == SQLDialect.POSTGRESQL) alStm[0].executeUpdate(" REINDEX TABLE MMS_work_shift_data ")
        else alStm[0].executeUpdate(" ALTER INDEX ALL ON MMS_work_shift_data REBUILD ")//--- у прочих диалектов просто перестраиваем индексы
        //--- для PostgreSQL свой синтаксис
        //--- для SQLite базы команды ALTER INDEX не реализовано
        alConn[0].commit()
        AdvancedLogger.debug("MMS_work_shift_data = $rowCountWS rows")

        //--- стираем устаревшие файлы
        for(path in alPath) clearOldFiles(path, maxExpirePeriod)

        //--- чистим место в файловых хранилищах под требуемое свободное место
        for(vdIndex in alStorage.indices)
            clearStorage(
                alStorage[vdIndex], hsStorageExt, alStorageSpaceByte[vdIndex], alStorageSpacePercent[vdIndex], alStorageDeleteEmptyDir[vdIndex]
            )
    }

}
