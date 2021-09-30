package foatto.spring.controllers

import foatto.core.link.GetReplicationRequest
import foatto.core.link.GetReplicationResponse
import foatto.core.link.PutReplicationRequest
import foatto.core.link.PutReplicationResponse
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.readFileToBuffer
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLDialect
import java.io.File

//--- добавлять у каждого наследника
//@RestController
abstract class CoreReplicationController {

    //--- прописывать у каждого наследника
//    @PostMapping("/api/get_replication")
    open fun getReplication(
        //@RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        val getReplicationBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        val getReplicationResponse = GetReplicationResponse(conn.dialect.dialect)

        val tmFile = conn.getReplicationList(getReplicationRequest.destName)
        //--- нельзя удалить файл из списка, пока не получено подтверждение
        var timeKey = if (tmFile.isEmpty()) {
            -1
        } else {
            tmFile.firstKey()
        }
        if (timeKey != -1L && timeKey == getReplicationRequest.prevTimeKey) {
            //--- окончательно удаляем файлы из очереди и их самих самого
            tmFile.remove(timeKey)?.forEach(File::delete)

            timeKey = if (tmFile.isEmpty()) {
                -1
            } else {
                tmFile.firstKey()
            }
        }
        if (timeKey != -1L) {
            val alFile = tmFile[timeKey]!!
            getReplicationResponse.timeKey = timeKey

            val bbIn = AdvancedByteBuffer(CoreAdvancedConnection.START_REPLICATION_SIZE)
            //--- skip a manually deleted files
            alFile.filter(File::exists).forEach { file ->
                readFileToBuffer(file, bbIn, false)
            }

            bbIn.flip()
            while (bbIn.hasRemaining()) {
                val sqlCount = bbIn.getInt()
                for (i in 0 until sqlCount) {
                    getReplicationResponse.alSQL.add(bbIn.getLongString())
                }
            }
        }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - getReplicationBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Update Query = " + (getCurrentTimeInt() - getReplicationBegTime))
            AdvancedLogger.error(getReplicationRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return getReplicationResponse
    }

    //--- прописывать у каждого наследника
//    @PostMapping("/api/put_replication")
    open fun putReplication(
        //@RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        val putReplicationBegTime = getCurrentTimeInt()

        val destName = putReplicationRequest.destName
        val sourName = putReplicationRequest.sourName
        val sourDialect = putReplicationRequest.sourDialect
        val timeKey = putReplicationRequest.timeKey

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        val alReplicationSQL = putReplicationRequest.alSQL.map {
            CoreAdvancedConnection.convertDialect(it, SQLDialect.hmDialect[sourDialect]!!, conn.dialect)
        }

        //--- проверка на приём этой реплики в предыдущей сессии связи
        val rs = stm.executeQuery(" SELECT 1 FROM SYSTEM_replication_send WHERE dest_name = '$destName' AND sour_name = '$sourName' AND time_key = $timeKey")
        val isAlReadyReceived = rs.next()
        rs.close()
        //--- такую реплику мы ещё не получали
        if (!isAlReadyReceived) {
            //--- реплика предназначена этому серверу - работаем как обычно
            if (CoreSpringApp.dbConfig.name == destName) {
                //--- выполнить реплику, возможно, с последующей перерепликацией:
                //--- если партнёр безымянный, то считаем, что к другим серверам-партнёрам в кластере он уже
                //--- не будет обращаться (т.е. ведёт себя как типовая клиентская программа) -
                //--- поэтому передадим реплику другим именованым партнёрам
                for (sql in alReplicationSQL) {
                    stm.executeUpdate(sql, sourName.isEmpty())
                    //                            //--- на время отладки репликатора
                    //                            AdvancedLogger.debug( sql );
                }
            }
            //--- реплика предназначена другому серверу - её надо просто отложить в соответствующую папку
            else if (alReplicationSQL.isNotEmpty()) {
                val bbReplicationData = CoreAdvancedConnection.getReplicationData(alReplicationSQL)
                conn.saveReplication(destName, bbReplicationData)
            }

            //--- и в этой же транзакции запомним имя/номер реплики
            if (stm.executeUpdate(" UPDATE SYSTEM_replication_send SET time_key = $timeKey WHERE dest_name = '$destName' AND sour_name = '$sourName' ", false) == 0) {

                stm.executeUpdate(" INSERT INTO SYSTEM_replication_send ( dest_name , sour_name , time_key ) VALUES ( '$destName' , '$sourName' , $timeKey ) ", false)
            }
        }
        //--- просто ответ
        val putReplicationResponse = PutReplicationResponse(timeKey)

        //--- зафиксировать любые изменения в базе
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - putReplicationBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Put Replication Query = " + (getCurrentTimeInt() - putReplicationBegTime))
            AdvancedLogger.error(putReplicationRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return putReplicationResponse
    }
}