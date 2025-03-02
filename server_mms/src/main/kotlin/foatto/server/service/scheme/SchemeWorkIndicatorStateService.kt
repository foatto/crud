package foatto.server.service.scheme

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
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
class SchemeWorkIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractSchemeIndicatorStateService(
    objectRepository = objectRepository,
) {

    companion object {
        private const val TYPE_SCHEME_WI_TITLE_TEXT: String = "type_scheme_wi_title_text"

        private const val TYPE_SCHEME_WI_VALUE: String = "type_scheme_wi_value"

        private const val TYPE_SCHEME_WI_DESCR_TEXT: String = "type_scheme_wi_descr_text"

        private const val INDICATOR_BACK_COLOR_NO_DATA = 0xFF_D0_D0_D0.toInt()
        private const val INDICATOR_BACK_COLOR_OFF = 0xFF_FF_E0_E0.toInt()
        private const val INDICATOR_BACK_COLOR_ON = 0xFF_E0_FF_E0.toInt()

        private const val INDICATOR_BORDER_COLOR_NO_DATA = 0xFF_A0_A0_A0.toInt()
        private const val INDICATOR_BORDER_COLOR_OFF = 0xFF_FF_00_00.toInt()
        private const val INDICATOR_BORDER_COLOR_ON = 0xFF_00_B0_00.toInt()

        private const val TEXT_COLOR = 0xFF_00_00_00.toInt()
    }

    override fun getElementConfigs(): Map<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
        this[TYPE_SCHEME_WI_TITLE_TEXT] = getTextConfig(TYPE_SCHEME_WI_TITLE_TEXT, 1)

        this[TYPE_SCHEME_WI_VALUE] = getArcConfig(TYPE_SCHEME_WI_VALUE, 1)

        this[TYPE_SCHEME_WI_DESCR_TEXT] = getTextConfig(TYPE_SCHEME_WI_DESCR_TEXT, 1)
    }

    override fun getElements(userConfig: ServerUserConfig, sensorId: Int): List<XyElement> {
        val alResult = mutableListOf<XyElement>()

        val sensorEntity = sensorRepository.findByIdOrNull(sensorId) ?: return emptyList()

//        val valueMultiplicator = sensorEntity.indicatorMultiplicator ?: 1.0

        var sensorTime: Int? = null
        var sensorValue: Boolean? = null

        SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_1, type_0
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_1 = (
                        SELECT MAX(ontime_1) FROM MMS_agg_${sensorEntity.id} 
                    )
                """
            )
            if (rs.next()) {
                sensorTime = rs.getInt(1)
                sensorValue = rs.getInt(2) != 0
            }
            rs.close()
        }

        val x0 = 6 * GRID_STEP

        //--- заголовок

        XyElement(TYPE_SCHEME_WI_TITLE_TEXT, -getRandomInt(), sensorId).apply {
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

        XyElement(TYPE_SCHEME_WI_VALUE, -getRandomInt(), sensorId).apply {
            isReadOnly = true
            alPoint = listOf(XyPoint(x0, 4 * GRID_STEP))
            radius = 2 * GRID_STEP
            // отсчёт углов в compose - в обратную сторону (по часовой стрелке)
            startAngle = 180
            sweepAngle = 360
            fillColor = when (sensorValue) {
                null -> INDICATOR_BACK_COLOR_NO_DATA
                false -> INDICATOR_BACK_COLOR_OFF
                true -> INDICATOR_BACK_COLOR_ON
            }
            drawColor = when (sensorValue) {
                null -> INDICATOR_BORDER_COLOR_NO_DATA
                false -> INDICATOR_BORDER_COLOR_OFF
                true -> INDICATOR_BORDER_COLOR_ON
            }
            lineWidth = 4
        }.let { xyElement ->
            alResult.add(xyElement)
        }

        sensorTime?.let { lastDataTime ->
            XyElement(TYPE_SCHEME_WI_DESCR_TEXT, -getRandomInt(), sensorId).apply {
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