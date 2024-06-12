package foatto.core.model.request.system

import foatto.core.model.ApiUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class AppRequest(
    val action: AppAction,
) : BaseRequest(
    url = action.module?.appUrl ?: ApiUrls.ERROR,
)

//    val find: String? = null,
