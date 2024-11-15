package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.ChartService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartController(
    private val chartService: ChartService,
) {

    @PostMapping(ApiUrlMMS.CHART_LIQUID_LEVEL)
    @Transactional
    fun chartLiquidLevel(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return chartService.chart(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

}