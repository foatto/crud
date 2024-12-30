package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.SchemeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.SchemeActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.SchemeWorkIndicatorStateService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SchemeWorkIndicatorStateController(
    private val schemeWorkIndicatorStateService: SchemeWorkIndicatorStateService,
) {

    @PostMapping(ApiUrlMMS.SCHEME_WORK_INDICATOR_STATE)
    @Transactional
    fun scheme(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return schemeWorkIndicatorStateService.scheme(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.SCHEME_WORK_INDICATOR_STATE_ACTION)
    @Transactional
    fun schemeAction(
        @RequestBody
        schemeActionRequest: SchemeActionRequest
    ): SchemeActionResponse {
        return schemeWorkIndicatorStateService.schemeAction(schemeActionRequest)
    }

}