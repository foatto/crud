package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
sealed class BaseResponse {
    abstract val responseCode: ResponseCode
}