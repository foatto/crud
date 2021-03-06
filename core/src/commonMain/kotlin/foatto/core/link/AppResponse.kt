package foatto.core.link

class AppResponse(
    val code: ResponseCode,

    val redirect: String? = null,
    val table: TableResponse? = null,
    val form: FormResponse? = null,
    val graphic: GraphicResponse? = null,
    val xy: XyResponse? = null
) {
    //--- temporarily use List instead of Map, because there is no way to deserialize Map in Kotlin / JS (and List is deserialized to Array)
    var hmUserProperty: Array<Pair<String, String>>? = null
//    var hmUserProperty: Map<String,String>? = null

    var alMenuData: Array<MenuData>? = null
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
}

class MenuData(val url: String, val text: String, val alSubMenu: Array<MenuData>? = null, val isHover: Boolean = false)

data class GraphicResponse(val documentTypeName: String, val startParamID: String, val shortTitle: String, val fullTitle: String)
