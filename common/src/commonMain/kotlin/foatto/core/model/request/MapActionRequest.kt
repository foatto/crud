package foatto.core.model.request

import foatto.core.ApiUrl
import foatto.core.appModuleUrls
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyElement
import foatto.core.model.model.xy.XyViewCoord
import kotlinx.serialization.Serializable

@Serializable
class MapActionRequest(
    val action: AppAction,
//    val documentTypeName: String,
//    val type: XyActionType,
//    val startParamId: String,

    //--- GET_ELEMENTS, GET_ONE_ELEMENT
    val viewCoord: XyViewCoord? = null,

    //--- GET_ONE_ELEMENT, ACTION_CLICK_ELEMENT
    val elementId: Int? = null,

    //--- GET_ELEMENTS
    val bitmapTypeName: String? = null,

    //--- CLICK_ELEMENT
    val objectId: Int? = null,

    //--- ADD_ELEMENT, EDIT_ELEMENT_POINT
    val xyElement: XyElement? = null,

    //--- MOVE_ELEMENTS
    val alActionElementIds: List<Int>? = null,
    val dx: Int? = null,
    val dy: Int? = null

) : BaseRequest(
    url = appModuleUrls[action.module]?.mapActionUrl ?: ApiUrl.ERROR,
)
//{
//    val hmParam: MutableMap<String, String> = mutableMapOf()
//}

