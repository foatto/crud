package foatto.core.model.response

import foatto.core.model.model.xy.XyElement
import foatto.core.model.response.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class SchemeActionResponse(
    override val responseCode: ResponseCode,

    //--- response on GET_COORDS
    val minCoord: XyPoint? = null,
    val maxCoord: XyPoint? = null,

    //--- response on GET_ELEMENTS
    val elements: List<XyElement> = emptyList(),

//    //--- response on GET_ONE_ELEMENT
//    val element: XyElement? = null,

    //--- additional custom parameters for any commands
    val hmParam: Map<String, String> = emptyMap(),
) : BaseResponse()
