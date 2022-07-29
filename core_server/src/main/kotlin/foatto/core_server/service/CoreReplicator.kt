package foatto.core_server.service

import foatto.core.link.GetReplicationRequest
import foatto.core.link.GetReplicationResponse
import foatto.core.link.PutReplicationRequest
import foatto.core.link.PutReplicationResponse
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.readFileToBuffer
import foatto.sql.CoreAdvancedConnection
import foatto.sql.SQLDialect
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

abstract class CoreReplicator(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private val ROLE_SENDER = 0
        private val ROLE_RECEIVER = 1

        private val CONFIG_DEST_NAME_ = "dest_name_"
        private val CONFIG_DEST_PROTOCOL_ = "dest_protocol_"
        private val CONFIG_DEST_IP_ = "dest_ip_"
        private val CONFIG_DEST_PORT_ = "dest_port_"
        private val CONFIG_DEST_ROLE_ = "dest_role_"

        private val CONFIG_CYCLE_PAUSE = "cycle_pause"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val isRunOnce: Boolean = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alDestName = mutableListOf<String>()
    private val alDestProtocol = mutableListOf<String>()
    private val alDestIP = mutableListOf<String>()
    private val alDestPort = mutableListOf<Int>()
    private val alDestRole = mutableListOf<Int?>()

    private var cyclePause: Long = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alHttpClient = mutableListOf<HttpClient>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        var index = 0
        while (true) {
            val destName = hmConfig[CONFIG_DEST_NAME_ + index] ?: break

            alDestName.add(destName)
            alDestProtocol += hmConfig[CONFIG_DEST_PROTOCOL_ + index] ?: "HTTP"
            alDestIP += hmConfig[CONFIG_DEST_IP_ + index]!!
            alDestPort += hmConfig[CONFIG_DEST_PORT_ + index]!!.toInt()
            //--- если роль репликатора не указана явно, то он выполняет обе роли - и отправителя и получателя
            alDestRole += hmConfig[CONFIG_DEST_ROLE_ + index]?.toInt()

            index++

            alHttpClient += HttpClient(Apache) {
                engine {
                    sslContext = SSLContext.getInstance("TLS")
                        .apply {
                            init(null, arrayOf(TrustAllX509TrustManager()), SecureRandom())
                        }
                }

                install(JsonFeature) {
                    serializer = JacksonSerializer()
                }

                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.NONE
//                    when {
//                        logOptions.contains("debug") -> LogLevel.ALL
//                        logOptions.contains("error") -> LogLevel.HEADERS
//                        logOptions.contains("info") -> LogLevel.INFO
//                        else -> LogLevel.NONE
//                    }
                }

                install(HttpTimeout)

                defaultRequest {
                    url.protocol = if (alDestProtocol.last().uppercase(Locale.getDefault()) == "HTTPS") URLProtocol.HTTPS else URLProtocol.HTTP
                    host = alDestIP.last()
                    port = alDestPort.last()
                }
            }
        }
/*
val client = HttpClient(Apache) {
    // install other features ....
} */
        cyclePause = hmConfig[CONFIG_CYCLE_PAUSE]!!.toLong() * 1000
    }

    override fun cycle() {
        //--- лог пришедшей SQL-реплики для разбирательств в случае ошибки
        var sqlLog = ""
        //--- флаг хотя бы одной успешной сработки
        var isWorked = false

        for (destIndex in alDestName.indices) {
            val destName = alDestName[destIndex]
            val destRole = alDestRole[destIndex]

            //--- на каждый сервер - отдельный try, чтобы перебирать прочие сервера, пока какие-то из них недоступны
            try {
                if (destRole == null || destRole == ROLE_SENDER) {
                    val tmFile = alConn[0].getReplicationList(destName)
                    if (!tmFile.isEmpty()) {
                        isWorked = true

                        //--- нельзя удалить файл из списка, пока не получено подтверждение
                        val timeKey = tmFile.firstKey()
                        val alFile = tmFile[timeKey]!!

                        val bbIn = AdvancedByteBuffer(CoreAdvancedConnection.START_REPLICATION_SIZE)
                        for (file in alFile) {
                            readFileToBuffer(file, bbIn, false)
                        }

                        bbIn.flip()
                        val alSQL = mutableListOf<String>()
                        while (bbIn.hasRemaining()) {
                            val sqlCount = bbIn.getInt()
                            for (i in 0 until sqlCount) {
                                alSQL.add(bbIn.getLongString())
                            }
                        }

                        val putReplicationRequest = PutReplicationRequest(
                            destName = destName,
                            sourName = alDBConfig[0].name,
                            sourDialect = alConn[0].dialect.dialect,
                            timeKey = timeKey,
                            alSQL = alSQL
                        )

                        runBlocking {
                            val putReplicationResponse: PutReplicationResponse = alHttpClient[destIndex].post("/api/put_replication") {
                                contentType(ContentType.Application.Json)

                                body = putReplicationRequest

                                timeout {
                                    socketTimeoutMillis = 600_000
                                }
                            }

                            //--- окончательно удаляем файл из очереди и его самого
                            if (timeKey == putReplicationResponse.timeKey) {
                                tmFile.remove(timeKey)
                                for (file in alFile) {
                                    file.delete()
                                }
                            }
                        }
                    }
                }

                if (destRole == null || destRole == ROLE_RECEIVER) {
                    sqlLog = ""

                    //--- что мы успешно получили в прошлый раз?
                    val rs = alStm[0].executeQuery(" SELECT time_key FROM SYSTEM_replication_receive WHERE dest_name = '$destName' ")
                    val prevTimeKey = if (rs.next()) {
                        rs.getLong(1)
                    } else {
                        -1
                    }
                    rs.close()

                    val getReplicationRequest = GetReplicationRequest(alDBConfig[0].name, prevTimeKey)

                    val getReplicationResponse: GetReplicationResponse = runBlocking {
                        alHttpClient[destIndex].post("/api/get_replication") {
                            contentType(ContentType.Application.Json)

                            body = getReplicationRequest

                            timeout {
                                socketTimeoutMillis = 600_000   // сервер может долго подбирать самые старые/первые реплики для передачи
                            }
                        }
                    }
                    val sourDialect = SQLDialect.hmDialect[getReplicationResponse.dialect]!!
                    val timeKey = getReplicationResponse.timeKey
                    if (timeKey != -1L) {
                        isWorked = true

                        getReplicationResponse.alSQL.forEach {
                            //--- на время отладки репликатора
                            //AdvancedLogger.debug( it )
                            //println("SQL = '$it'")

                            sqlLog += (if (sqlLog.isEmpty()) "" else "\n") + it
                            alStm[0].executeUpdate(CoreAdvancedConnection.convertDialect(it, sourDialect, alConn[0].dialect), false)
                        }
                        //--- и в этой же транзакции запомним имя/номер реплики
                        if (alStm[0].executeUpdate(" UPDATE SYSTEM_replication_receive SET time_key = $timeKey WHERE dest_name = '$destName' ", false) == 0) {
                            alStm[0].executeUpdate(" INSERT INTO SYSTEM_replication_receive ( dest_name , time_key ) VALUES ( '$destName' , $timeKey ) ", false)
                        }
                        alConn[0].commit()
                    }
                }
            } catch (ioe: IOException) {
                alConn[0].rollback()
                AdvancedLogger.error(ioe)
            } catch (t: Throwable) {
                alConn[0].rollback()
                //--- вывести/сохранить SQL-запрос, при котором (возможно) получена ошибка
                AdvancedLogger.error(sqlLog)
                t.printStackTrace()
                //--- передаём ошибку дальше
                throw t
            }
        }
        //--- если нам нечего было отправлять и получать на все сервера/со всех серверов,
        //--- то выдержим паузу
        if (!isWorked) {
            AdvancedLogger.info("No replication data. Pause ${cyclePause / 1000} sec.")
            Thread.sleep(cyclePause)
        }
    }

}

class TrustAllX509TrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)

    override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}

    override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
}