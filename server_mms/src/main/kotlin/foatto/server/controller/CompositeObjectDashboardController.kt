package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.CompositeObjectDashboardService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CompositeObjectDashboardController(
    private val compositeObjectDashboardService: CompositeObjectDashboardService,
) {

    @PostMapping(ApiUrlMMS.COMPOSITE_OBJECT_DASHBOARD)
    @Transactional
    fun compositeObjectDashboard(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse = compositeObjectDashboardService.composite(
        sessionId = appRequest.sessionId,
        action = appRequest.action,
    )

}