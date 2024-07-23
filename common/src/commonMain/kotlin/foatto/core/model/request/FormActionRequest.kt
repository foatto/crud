package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormActionRequest(
    val action: AppAction,
    val formActionData: Map<String, FormActionData>,
) : BaseRequest(
    url = appModuleUrls[action.module]?.formActionUrl ?: ApiUrl.ERROR,
)