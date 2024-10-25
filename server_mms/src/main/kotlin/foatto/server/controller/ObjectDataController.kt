package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.ObjectDataService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ObjectDataController(
    private val objectDataService: ObjectDataService,
) {

    @PostMapping(ApiUrlMMS.OBJECT_DATA)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return objectDataService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

//    @PostMapping(ApiUrlMMS.OBJECT_DATA_FORM_ACTION)
//    @Transactional
//    fun formAction(
//        @RequestBody
//        formActionRequest: FormActionRequest
//    ): FormActionResponse {
//        return objectDataService.formAction(
//            sessionId = formActionRequest.sessionId,
//            action = formActionRequest.action,
//            formActionData = formActionRequest.formActionData,
//        )
//    }

}