package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.SensorDataService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SensorDataController(
    private val sensorDataService: SensorDataService,
) {

    @PostMapping(ApiUrlMMS.SENSOR_DATA)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return sensorDataService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

//    @PostMapping(ApiUrlMMS.SENSOR_DATA_FORM_ACTION)
//    @Transactional
//    fun formAction(
//        @RequestBody
//        formActionRequest: FormActionRequest
//    ): FormActionResponse {
//        return sensorDataService.formAction(
//            sessionId = formActionRequest.sessionId,
//            action = formActionRequest.action,
//            formActionData = formActionRequest.formActionData,
//        )
//    }

}