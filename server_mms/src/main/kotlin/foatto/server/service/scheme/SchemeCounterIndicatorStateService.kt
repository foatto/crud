package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SchemeCounterIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractSchemeIndicatorStateService(
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_CI_TITLE_TEXT: String = "mms_scheme_ci_title_text"

        private const val TYPE_SCHEME_CI_CUR_VALUE_TEXT: String = "mms_scheme_ci_cur_value_text"
        private const val TYPE_SCHEME_CI_DESCR_TEXT: String = "mms_scheme_ci_descr_text"

        private const val TEXT_COLOR = 0xFF_00_00_00.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_CI_TITLE_TEXT] = getTextConfig(TYPE_SCHEME_CI_TITLE_TEXT, 1)

        this[TYPE_SCHEME_CI_CUR_VALUE_TEXT] = getTextConfig(TYPE_SCHEME_CI_CUR_VALUE_TEXT, 1)
        this[TYPE_SCHEME_CI_DESCR_TEXT] = getTextConfig(TYPE_SCHEME_CI_DESCR_TEXT, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

//        val valueMultiplicator = sensorEntity.indicatorMultiplicator ?: 1.0

        var sensorTime: Int? = null
        var sensorValue: Double? = null

        SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0, value_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 = (
                        SELECT MAX(ontime_0) FROM MMS_agg_${sensorEntity.id} 
                    )
                """
            )
            if (rs.next()) {
                sensorTime = rs.getInt(1)
                sensorValue = rs.getDouble(2)
            }
            rs.close()
        }

        val x0 = 6 * GRID_STEP

        //--- заголовок

        XyElement(TYPE_SCHEME_CI_TITLE_TEXT, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, 1 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorEntity.descr ?: "-"
            textColor = TEXT_COLOR
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = 12
            isFontBold = true
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        XyElement(TYPE_SCHEME_CI_CUR_VALUE_TEXT, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, 6 * GRID_STEP))
            anchorX = XyElement.Anchor.CC
            anchorY = XyElement.Anchor.RB
            text = sensorValue?.let { sv ->
                getSplittedDouble(sv, 1)
            } ?: "-"
            textColor = TEXT_COLOR
            fillColor = null
            drawColor = null
            lineWidth = null
            fontSize = 24
            isFontBold = true
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        sensorTime?.let { lastDataTime ->
            XyElement(TYPE_SCHEME_CI_DESCR_TEXT, -getRandomInt(), sensorId).apply {
                isReadOnly = true
                alPoint = listOf(XyPoint(x0, 7 * GRID_STEP))
                anchorX = XyElement.Anchor.CC
                anchorY = XyElement.Anchor.LT
                text = getDateTimeDMYHMSString(userConfig.timeOffset, lastDataTime)
                textColor = TEXT_COLOR
                fillColor = null
                drawColor = null
                lineWidth = null
                fontSize = 12
                isFontBold = false
            }.let { xyElement ->
                alResult.add(xyElement)
            }
        }

        return alResult
    }

}