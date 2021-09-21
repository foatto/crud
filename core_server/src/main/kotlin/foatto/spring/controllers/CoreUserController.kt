package foatto.spring.controllers

import foatto.core.link.ChangePasswordRequest
import foatto.core.link.ChangePasswordResponse
import foatto.core.link.LogoffRequest
import foatto.core.link.LogoffResponse
import foatto.core.link.SaveUserPropertyRequest
import foatto.core.link.SaveUserPropertyResponse
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.spring.CoreSpringApp
import foatto.sql.AdvancedConnection
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

//--- добавлять у каждого наследника
//@RestController
abstract class CoreUserController {

    //--- прописывать у каждого наследника
//    @PostMapping("/api/save_user_property")
//    @Transactional
    open fun saveUserProperty(
        //@RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        val saveUserPropertyBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

//        logQuery( hmParam )
        val upName = saveUserPropertyRequest.name
        val upValue = saveUserPropertyRequest.value

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(saveUserPropertyRequest.sessionID) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[iApplication.USER_CONFIG] as? UserConfig

        if (userConfig != null) {
            userConfig.saveUserProperty(conn, upName, upValue)
        }
        else {
            AdvancedLogger.error("User config not defined for saved property, name = '$upName', value = '$upValue'.")
        }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - saveUserPropertyBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Save User Property Query = " + (getCurrentTimeInt() - saveUserPropertyBegTime))
            AdvancedLogger.error(saveUserPropertyRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return SaveUserPropertyResponse()
    }

    //--- прописывать у каждого наследника
//    @PostMapping("/api/change_password")
//    @Transactional
    open fun changePassword(
        //@RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {
        val changePasswordBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

//        logQuery( hmParam )

        val newPassword = changePasswordRequest.password
        val toDay = ZonedDateTime.now()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(changePasswordRequest.sessionID) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[iApplication.USER_CONFIG] as? UserConfig

        if (userConfig != null)
            stm.executeUpdate(
                " UPDATE SYSTEM_users SET pwd = '$newPassword' , " +
                    " pwd_ye = ${toDay.year} , pwd_mo = ${toDay.monthValue} , pwd_da = ${toDay.dayOfMonth}" +
                    " WHERE id = ${userConfig.userId} "
            )

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - changePasswordBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Change Password Query = " + (getCurrentTimeInt() - changePasswordBegTime))
            AdvancedLogger.error(changePasswordRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return ChangePasswordResponse()
    }

    //--- прописывать у каждого наследника
//    @PostMapping("/api/logoff")
    open fun logoff(
        //@RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        val logoffBegTime = getCurrentTimeInt()

        CoreSpringApp.chmSessionStore.remove(logoffRequest.sessionID)

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - logoffBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Logoff Query = " + (getCurrentTimeInt() - logoffBegTime))
            AdvancedLogger.error(logoffRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return LogoffResponse()
    }

}