package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
class AppResponse(
    override val responseCode: ResponseCode,

    val table: TableResponse? = null,
    val form: FormResponse? = null,
//    val chart: ChartResponse? = null,
//    val xy: XyResponse? = null,
//    val composite: CompositeResponse? = null,
) : BaseResponse()

//    var currentUserName = ""
//    var hmUserProperty: Map<String, String>? = null
//    var alMenuData: List<MenuData>? = null

