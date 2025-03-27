package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.ObjectListService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ObjectListController(
    private val objectListService: ObjectListService,
) {

    @PostMapping(ApiUrlMMS.OBJECT_LIST)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return objectListService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

//    @PostMapping(ApiUrlMMS.OBJECT_LIST_FORM_ACTION)
//    @Transactional
//    fun formAction(
//        @RequestBody
//        formActionRequest: FormActionRequest
//    ): FormActionResponse {
//        return objectListService.formAction(
//            sessionId = formActionRequest.sessionId,
//            action = formActionRequest.action,
//            formActionData = formActionRequest.formActionData,
//        )
//    }

}