package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.FormActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.DayWorkService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DayWorkController(
    private val dayWorkService: DayWorkService,
) {

    @PostMapping(ApiUrlMMS.DAY_WORK)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return dayWorkService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.DAY_WORK_FORM_ACTION)
    @Transactional
    fun formAction(
        @RequestBody
        formActionRequest: FormActionRequest
    ): FormActionResponse {
        return dayWorkService.formAction(
            sessionId = formActionRequest.sessionId,
            action = formActionRequest.action,
            formActionData = formActionRequest.formActionData,
        )
    }

}