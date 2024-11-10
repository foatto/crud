package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.SchemeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.SchemeActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.SchemeAnalogueIndicatorStateService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SchemeAnalogueIndicatorStateController(
    private val schemeAnalogueIndicatorStateService: SchemeAnalogueIndicatorStateService,
) {

    @PostMapping(ApiUrlMMS.SCHEME_ANALOGUE_INDICATOR_STATE)
    @Transactional
    fun scheme(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return schemeAnalogueIndicatorStateService.scheme(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }
    
    @PostMapping(ApiUrlMMS.SCHEME_ANALOGUE_INDICATOR_STATE_ACTION)
    @Transactional
    fun schemeAction(
        @RequestBody
        schemeActionRequest: SchemeActionRequest
    ): SchemeActionResponse {
        return schemeAnalogueIndicatorStateService.schemeAction(schemeActionRequest)
    }

}