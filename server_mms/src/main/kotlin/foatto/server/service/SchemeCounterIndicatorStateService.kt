package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyElement
import foatto.core.model.request.SchemeActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.SchemeActionResponse
import foatto.core.model.response.TitleData
import foatto.core.model.response.xy.XyElementClientType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.scheme.SchemeResponse
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.initXyElementConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SchemeCounterIndicatorStateService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) {

    companion object {

        private const val MIN_SCALE: Int = 1
        private const val MAX_SCALE: Int = 1024 * 1024 * 1024

        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        private const val GRID_STEP: Int = 1024 * 1024

        private const val TYPE_SCHEME_CI_TITLE_TEXT: String = "mms_scheme_ci_title_text"

        private const val TYPE_SCHEME_CI_CUR_VALUE_TEXT: String = "mms_scheme_ci_cur_value_text"
        private const val TYPE_SCHEME_CI_DESCR_TEXT: String = "mms_scheme_ci_descr_text"

        private const val TEXT_COLOR = 0xFF_00_00_00.toInt()

        val elementConfigs: MutableMap<String, XyElementConfig> = initXyElementConfig(level = 10, minScale = MIN_SCALE, maxScale = MAX_SCALE).apply {
            this[TYPE_SCHEME_CI_TITLE_TEXT] = getTextConfig(TYPE_SCHEME_CI_TITLE_TEXT, 1)

            this[TYPE_SCHEME_CI_CUR_VALUE_TEXT] = getTextConfig(TYPE_SCHEME_CI_CUR_VALUE_TEXT, 1)
            this[TYPE_SCHEME_CI_DESCR_TEXT] = getTextConfig(TYPE_SCHEME_CI_DESCR_TEXT, 1)
        }

        private fun getTextConfig(name: String, layer: Int) = XyElementConfig(
            name = name,
            clientType = XyElementClientType.TEXT,
            layer = layer,
            scaleMin = MIN_SCALE,
            scaleMax = MAX_SCALE,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )
    }

    fun scheme(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }
        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)
        val objectEntity = objectRepository.findByIdOrNull(action.id) ?: return AppResponse(ResponseCode.LOGON_NEED)

        val caption = moduleConfig.caption
        val rows = listOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель" to (objectEntity.model ?: "-"),
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_SCHEME,
            scheme = SchemeResponse(
                elementConfigs = elementConfigs,
                tabCaption = caption,
                headerData = HeaderData(
                    titles = listOf(
                        TitleData(
                            action = null,
                            text = caption,
                            isBold = true,
                        )
                    ),
                    rows = rows,
                ),
            )
        )
    }

    fun schemeAction(schemeActionRequest: SchemeActionRequest): SchemeActionResponse {
        val actionModule = schemeActionRequest.action.module

        val sessionData = SpringApp.getSessionData(schemeActionRequest.sessionId) ?: return SchemeActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return SchemeActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return SchemeActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (schemeActionRequest.action.type) {
            ActionType.GET_COORDS -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.OK,
                    minCoord = XyPoint(0, 0),
                    maxCoord = XyPoint(12 * GRID_STEP, 8 * GRID_STEP),
                )
            }

            ActionType.GET_ELEMENTS -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.OK,
                    elements = getElements(userConfig, schemeActionRequest.action.id!!),
                )
            }

            else -> {
                SchemeActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    private fun getElements(userConfig: ServerUserConfig, sensorId: Int): List<XyElement> {
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