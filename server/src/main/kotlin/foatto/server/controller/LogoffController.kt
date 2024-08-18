package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.LogoffRequest
import foatto.core.model.response.LogoffResponse
import foatto.server.service.LogoffService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LogoffController(
    val logoffService: LogoffService,
) {

    @PostMapping(ApiUrl.LOGOFF)
    fun logoff(
        @RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        logoffService.logoff(logoffRequest.sessionId)

        return LogoffResponse()
    }

}
