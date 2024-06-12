package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
class FormActionResponse(
    override val responseCode: ResponseCode,
    val errors: Map<String, String>? = null,
) : BaseResponse()