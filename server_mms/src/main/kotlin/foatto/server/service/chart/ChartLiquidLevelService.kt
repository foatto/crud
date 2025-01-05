package foatto.server.service.chart

import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.ChartActionResponse
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class ChartLiquidLevelService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getCharts(chartActionRequest: ChartActionRequest): ChartActionResponse {
        TODO("Not yet implemented")
    }

}
