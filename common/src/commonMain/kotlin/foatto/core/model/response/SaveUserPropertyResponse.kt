package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
class SaveUserPropertyResponse(
    override val responseCode: ResponseCode,
) : BaseResponse()
