package foatto.server.service

import foatto.server.SpringApp
import org.springframework.stereotype.Service

@Service
class LogoffService {

    fun logoff(
        sessionId: Long,
    ) {
        SpringApp.removeSessionData(sessionId)
    }
}