package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.MapService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MapController(
    private val mapService: MapService,
) {

    @PostMapping(ApiUrlMMS.MAP_TRACE)
    @Transactional
    fun mapTrace(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return mapService.map(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

}