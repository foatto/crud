package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class AppRequest(
    val action: AppAction,
) : BaseRequest(
    url = appModuleUrls[action.module]?.appUrl ?: ApiUrl.ERROR,
)
