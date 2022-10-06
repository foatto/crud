package foatto.spring.controllers

import foatto.core.link.ChangePasswordRequest
import foatto.core.link.ChangePasswordResponse
import foatto.core.link.LogoffRequest
import foatto.core.link.LogoffResponse
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@RestController
class CoreUserController {

    @PostMapping("/api/change_password")
    fun changePassword(
        @RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {
        val changePasswordBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

//        logQuery( hmParam )

        val newPassword = changePasswordRequest.password
        val toDay = ZonedDateTime.now()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(changePasswordRequest.sessionId) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[iApplication.USER_CONFIG] as? UserConfig

        if (userConfig != null)
            conn.executeUpdate(
                " UPDATE SYSTEM_users SET pwd = '$newPassword' , " +
                    " pwd_ye = ${toDay.year} , pwd_mo = ${toDay.monthValue} , pwd_da = ${toDay.dayOfMonth}" +
                    " WHERE id = ${userConfig.userId} "
            )

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - changePasswordBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Change Password Query = " + (getCurrentTimeInt() - changePasswordBegTime))
            AdvancedLogger.error(changePasswordRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return ChangePasswordResponse()
    }

    @PostMapping("/api/logoff")
    fun logoff(
        @RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        val logoffBegTime = getCurrentTimeInt()

        CoreSpringApp.chmSessionStore.remove(logoffRequest.sessionId)

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - logoffBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Logoff Query = " + (getCurrentTimeInt() - logoffBegTime))
            AdvancedLogger.error(logoffRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return LogoffResponse()
    }

}