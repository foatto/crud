package foatto.server

import foatto.server.model.SessionData
import java.util.concurrent.ConcurrentHashMap

abstract class SpringApp {

    companion object {
        private val sessionDataStore = ConcurrentHashMap<Long, SessionData>()

        fun getSessionData(sessionId: Long): SessionData? = sessionDataStore[sessionId]
        fun putSessionData(sessionId: Long, sessionData: SessionData) {
            sessionDataStore[sessionId] = sessionData
        }

        fun removeSessionData(sessionId: Long) {
            sessionDataStore.remove(sessionId)
        }
    }
}