package foatto.server.controller

import foatto.core.model.request.AppRequest
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core_mms.ApiUrlMMS
import foatto.server.service.chart.ChartAnalogueByTypeService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartAnalogueByTypeController(
    private val chartAnalogueByTypeService: ChartAnalogueByTypeService,
) {

    @PostMapping(ApiUrlMMS.CHART_ANALOGUE_BY_TYPE)
    @Transactional
    fun chart(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return chartAnalogueByTypeService.chart(
            sessionId = appRequest.sessionId,
            action = appRequest.action,
        )
    }

    @PostMapping(ApiUrlMMS.CHART_ANALOGUE_BY_TYPE_ACTION)
    @Transactional
    fun chartAction(
        @RequestBody
        chartActionRequest: ChartActionRequest,
    ): ChartActionResponse = chartAnalogueByTypeService.chartAction(
        chartActionRequest = chartActionRequest,
    )
}