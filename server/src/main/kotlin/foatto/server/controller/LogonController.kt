package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.LogonRequest
import foatto.core.model.response.LogonResponse
import foatto.core.model.response.ResponseCode
import foatto.server.service.LogonService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LogonController(
    private val logonService: LogonService,
) {

    @PostMapping(ApiUrl.LOGON)
    fun logon(
        @RequestBody
        logonRequest: LogonRequest
    ): LogonResponse {
        val result = logonService.logon(
            sessionId = logonRequest.sessionId,
            login = logonRequest.login,
            password = logonRequest.password,
        )
        return if (result.responseCode == ResponseCode.LOGON_SUCCESS) {
            LogonResponse(
                responseCode = result.responseCode,
                appUserConfig = result.appUserConfig,
                alMenuData = result.alMenuData,
                redirectOnLogon = result.redirectOnLogon,
            )
        } else {
            LogonResponse(result.responseCode)
        }
    }

}