package foatto.server.service.scheme

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
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.model.ServerUserConfig
import foatto.server.repository.ObjectRepository
import org.springframework.data.repository.findByIdOrNull

abstract class AbstractSchemeIndicatorStateService(
    private val objectRepository: ObjectRepository,
) {

    companion object {

        const val MIN_SCALE: Int = 1
        const val MAX_SCALE: Int = 1024 * 1024 * 1024

        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        const val GRID_STEP: Int = 1024 * 1024
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
                elementConfigs = getElementConfigs(),
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

    abstract fun getElementConfigs(): Map<String, XyElementConfig>

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

    protected abstract fun getElements(userConfig: ServerUserConfig, sensorId: Int): List<XyElement>

    protected fun getArcConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.ARC,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )

    protected fun getLineConfig(name: String, layer: Int) = XyElementConfig(
        name = name,
        clientType = XyElementClientType.POLY,
        layer = layer,
        scaleMin = MIN_SCALE,
        scaleMax = MAX_SCALE,
        descrForAction = "",
        isRotatable = false,
        isMoveable = false,
        isEditablePoint = false
    )

    protected fun getTextConfig(name: String, layer: Int) = XyElementConfig(
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