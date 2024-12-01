package foatto.server.service

import foatto.core.model.AppAction
import foatto.core.model.response.AppResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.xy.XyDocumentConfig
import foatto.core.model.response.xy.map.MapResponse
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.repository.ObjectRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MapService(
    private val objectRepository: ObjectRepository,
) {

    fun map(
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

        //--- заполнение текста заголовка информацией по периоду времени - пока отключено, т.к. заголовок становится слишком длинным
//        if (action.timeRangeType != 0) {
//            caption += " за последние " +
//                if (action.timeRangeType % 3600 == 0) {
//                    "${action.timeRangeType / 3600} час(а,ов) "
//                } else if (action.timeRangeType % 60 == 0) {
//                    "${action.timeRangeType / 60} минут "
//                } else {
//                    "${action.timeRangeType} секунд "
//                }
//        }

        return AppResponse(
            responseCode = ResponseCode.MODULE_MAP,
            map = MapResponse(
                documentConfig = XyDocumentConfig("", "", "", true, emptyMap()), //!!!
                startParamId = "0", //!!!
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
                timeRangeType = action.timeRangeType,
                begTime = action.begTime ?: 0,
                endTime = action.endTime ?: 0,
            )
        )
    }

}
/*
    protected fun getXyResponse(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse {

//        val docTypeName = hmParam[AppParameter.ALIAS]
//        val xyStartDataID = hmParam[AppParameter.XY_START_DATA]!!
//
//        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData
/*
class XyStartData(
    val alStartObjectData: MutableList<XyStartObjectData>,
    val rangeType: Int = 0, // для статических элементов неюзабельно
    val begTime: Int = 0,
    val endTime: Int = 0,
    var title: String = "",
    var tooltip: String = "",
    val alServerActionButton: MutableList<ServerActionButton> = mutableListOf(),
)

class XyStartObjectData(
    val objectId: Int,
    var typeName: String = "",
    var isStart: Boolean = false,
    var isTimed: Boolean = false,
    var isReadOnly: Boolean = false
)

class XyStartObjectParsedData(val objectId: Int) {
    val hsType = mutableSetOf<String>()
    var begTime = 0
    var endTime = 0
}
 */
        return AppResponse(
            responseCode = ResponseCode.MODULE_MAP,
            map = MapResponse(
                documentConfig = CoreSpringApp.hmXyDocumentConfig[docTypeName]!!,
                startParamId = xyStartDataID,
                title = sd.title.substring(0, min(32000, sd.title.length)),
                alServerActionButton = sd.alServerActionButton,
            )
        )
    }


 */