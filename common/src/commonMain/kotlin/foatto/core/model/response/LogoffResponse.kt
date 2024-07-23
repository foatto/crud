package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
class LogoffResponse(
    override val responseCode: ResponseCode = ResponseCode.NOTHING,
) : BaseResponse()
