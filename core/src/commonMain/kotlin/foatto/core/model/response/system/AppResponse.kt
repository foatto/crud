package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
class AppResponse(
    override val responseCode: ResponseCode,

    val table: TableResponse? = null,
    val form: FormResponse? = null,
) : BaseResponse()

