package foatto.core.model.request.system

import foatto.core.model.ApiUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormActionRequest(
    val action: AppAction,
    val formActionData: Map<String, FormActionData>,
) : BaseRequest(
    url = action.module?.formActionUrl ?: ApiUrls.ERROR,
)