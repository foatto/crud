package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class SchemeActionRequest(
    val action: AppAction,

//    //--- GET_ELEMENTS, GET_ONE_ELEMENT
//    val viewCoord: XyViewCoord? = null,
//
//    //--- GET_ONE_ELEMENT, ACTION_CLICK_ELEMENT
//    val elementId: Int? = null,
//
//    //--- GET_ELEMENTS
//    val bitmapTypeName: String? = null,
//
//    //--- CLICK_ELEMENT
//    val objectId: Int? = null,
//
//    //--- ADD_ELEMENT, EDIT_ELEMENT_POINT
//    val xyElement: XyElement? = null,
//
//    //--- MOVE_ELEMENTS
//    val alActionElementIds: List<Int>? = null,
//    val dx: Int? = null,
//    val dy: Int? = null

) : BaseRequest(
    url = appModuleUrls[action.module]?.schemeActionUrl ?: ApiUrl.ERROR,
)
//{
//    val hmParam: MutableMap<String, String> = mutableMapOf()
//}
