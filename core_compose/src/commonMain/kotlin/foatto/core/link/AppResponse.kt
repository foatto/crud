package foatto.core.link

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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

    //--- temporarily use Array of Pair instead of Map, because there is no way to deserialize Map in Kotlin / JS (and List is deserialized to Array)
    var hmUserProperty: Array<Pair<String, String>>? = null
//    var hmUserProperty: Map<String,String>? = null

    var arrMenuData: Array<MenuData>? = null
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
    val arrSubMenu: Array<MenuData>? = null,
    var itHover: Boolean = false,   //!!! unused, убрать после полного перехода на Compose
    @Transient
    val inNewWindow: Boolean = false,
    @Transient
    val isHover: MutableState<Boolean> = mutableStateOf(false),
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