package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class AppResponse(
    val code: ResponseCode,

    val redirect: String? = null,
    val table: TableResponse? = null,
    val form: FormResponse? = null,
    val graphic: GraphicResponse? = null,
    val xy: XyResponse? = null,
    val composite: CompositeResponse? = null,
) {

    var currentUserName = ""

    var hmUserProperty: Map<String, String>? = null

    var alMenuData: List<MenuData>? = null
}

enum class ResponseCode {
    LOGON_NEED,

    LOGON_SUCCESS,
    LOGON_SUCCESS_BUT_OLD,
    LOGON_FAILED,
    LOGON_SYSTEM_BLOCKED,
    LOGON_ADMIN_BLOCKED,

    REDIRECT,

    TABLE,
    FORM,

    GRAPHIC,
    XY,
//    VIDEO_ONLINE         ,
//    VIDEO_ARCHIVE

    COMPOSITE,
}

@Serializable
class MenuData(
    val url: String,
    val text: String,
    val alSubMenu: List<MenuData>? = null,
)

@Serializable
class GraphicResponse(
    val documentTypeName: String,
    val startParamId: String,
    val shortTitle: String,
    val fullTitle: String,

    val rangeType: Int,
    val begTime: Int,
    val endTime: Int,
)

@Serializable
class CompositeResponse(
    val xyResponse: XyResponse,
    val graphicResponse: GraphicResponse,
)