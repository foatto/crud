package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.FormActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.SensorService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SensorController(
    private val sensorService: SensorService,
) {

    @PostMapping(ApiUrlMMS.SENSOR)
    @Transactional
    fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return sensorService.app(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.SENSOR_FORM_ACTION)
    @Transactional
    fun formAction(
        @RequestBody
        formActionRequest: FormActionRequest
    ): FormActionResponse {
        return sensorService.formAction(
            sessionId = formActionRequest.sessionId,
            action = formActionRequest.action,
            formActionData = formActionRequest.formActionData,
        )
    }

}