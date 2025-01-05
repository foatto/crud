package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.chart.ChartLiquidLevelService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartLiquidLevelController(
    private val chartLiquidLevelService: ChartLiquidLevelService,
) {

    @PostMapping(ApiUrlMMS.CHART_LIQUID_LEVEL)
    @Transactional
    fun chart(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse = chartLiquidLevelService.chart(
        sessionId = appRequest.sessionId,
        action = appRequest.action,
    )

    @PostMapping(ApiUrlMMS.CHART_LIQUID_LEVEL_ACTION)
    @Transactional
    fun chartAction(
        @RequestBody
        chartActionRequest: ChartActionRequest,
    ): ChartActionResponse = chartLiquidLevelService.chartAction(
        chartActionRequest = chartActionRequest,
    )
}
