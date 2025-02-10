package foatto.server

import foatto.core.SESSION_EXPIRE_TIME
import foatto.core.util.getCurrentTimeInt
import foatto.server.model.SessionData
import foatto.server.util.MinioProxy
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.ConcurrentHashMap

abstract class SpringApp {

    companion object {
        var minioProxy: MinioProxy? = null

        private val sessionDataTime = ConcurrentHashMap<Long, Int>()
        private val sessionDataStore = ConcurrentHashMap<Long, SessionData>()

        fun getSessionData(sessionId: Long): SessionData? {
            val sessionExpireTime = sessionDataTime[sessionId] ?: 0
            return if (getCurrentTimeInt() < sessionExpireTime) {
                sessionDataTime[sessionId] = getCurrentTimeInt() + SESSION_EXPIRE_TIME
                sessionDataStore[sessionId]
            } else {
                removeSessionData(sessionId)
                null
            }
        }

        fun putSessionData(sessionId: Long, sessionData: SessionData) {
            sessionDataTime[sessionId] = getCurrentTimeInt() + SESSION_EXPIRE_TIME
            sessionDataStore[sessionId] = sessionData
        }

        fun removeSessionData(sessionId: Long) {
            sessionDataTime.remove(sessionId)
            sessionDataStore.remove(sessionId)
        }
    }

    @Value("\${minio.endpoint}")
    val minioEndpoint: String = ""

    @Value("\${minio.accessKey}")
    val minioAccessKey: String = ""

    @Value("\${minio.secretKey}")
    val minioSecretKey: String = ""

    @Value("\${minio.timeout}")
    val minioTimeout: String = ""

    @Value("\${minio.bucket}")
    val minioBucket: String = ""

    open fun init() {
        if (minioEndpoint.isNotBlank()) {
            minioProxy = MinioProxy(minioEndpoint, minioAccessKey, minioSecretKey, minioTimeout.toLong(), minioBucket)
        }
    }
}