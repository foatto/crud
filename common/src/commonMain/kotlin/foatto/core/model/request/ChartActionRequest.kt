package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class ChartActionRequest(
    val action: AppAction,

    val documentTypeName: String,
    val startParamId: String,

    val graphicCoords: Pair<Int, Int>? = null,
    val viewSize: Pair<Int, Int>? = null
) : BaseRequest(
    url = appModuleUrls[action.module]?.chartActionUrl ?: ApiUrl.ERROR,
)

