package foatto.core.link

//import kotlinx.serialization.Serializable

//@Serializable
class AppResponse(
    val code: ResponseCode,

    val redirect: String? = null,
    val table: TableResponse? = null,
    val form: FormResponse? = null,
    val graphic: GraphicResponse? = null,
    val xy: XyResponse? = null
) {
    //--- временно используем List вместо Map, т.к. в Kotlin/JS нет возможности десериализовать Map (а List десериализуется в Array)
    var hmUserProperty: List<Pair<String,String>>? = null
//    var hmUserProperty: Map<String,String>? = null

    var alMenuData: List<MenuData>? = null
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//@Serializable
enum class ResponseCode {
    LOGON_NEED           ,

    LOGON_SUCCESS        ,   // успешный логон
    LOGON_SUCCESS_BUT_OLD,   // успешный логон, но пароль устарел
    LOGON_FAILED         ,   // неправильное имя или пароль
    LOGON_SYSTEM_BLOCKED ,   // слишком много попыток входа, аккаунт временно заблокирован системой
    LOGON_ADMIN_BLOCKED  ,   // пользователь заблокирован администратором

    REDIRECT             ,

    TABLE                ,
    FORM                 ,

    GRAPHIC              ,
    XY                   ,
//    VIDEO_ONLINE         ,
//    VIDEO_ARCHIVE
}

//@Serializable
class MenuData( val url: String, val text: String, val alSubMenu: List<MenuData>? = null )

//@Serializable
data class GraphicResponse( val documentTypeName: String, val startParamID: String, val shortTitle: String, val fullTitle: String )