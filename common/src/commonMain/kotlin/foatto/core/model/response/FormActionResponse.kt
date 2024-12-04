package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class FormActionResponse(
    override val responseCode: ResponseCode,
    val errors: Map<String, String>? = null,
    val newTabAction: AppAction? = null,
    val nextAction: AppAction? = null,
) : BaseResponse()