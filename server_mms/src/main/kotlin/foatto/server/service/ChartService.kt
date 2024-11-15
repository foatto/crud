package foatto.server.service

import foatto.core.model.AppAction
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.chart.ChartResponse
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.repository.ObjectRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChartService(
    private val objectRepository: ObjectRepository,
) {

    fun chart(
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

        val caption = moduleConfig.caption

        val headers = mutableListOf(caption)
        objectRepository.findByIdOrNull(action.id)?.let { objectEntity ->
            headers += "\n${objectEntity.name}"
            if (!objectEntity.model.isNullOrBlank()) {
                headers += ", ${objectEntity.model}"
            }
        }

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
            responseCode = ResponseCode.MODULE_CHART,
            chart = ChartResponse(
                documentTypeName = "",  //!!!aliasName,
                startParamId = "",   //!!!graphicStartDataID,
                tabCaption = caption,
                headers = headers,
                timeRangeType = action.timeRangeType,
                begTime = action.begTime ?: 0,
                endTime = action.endTime ?: 0,
            )
        )
    }

}
