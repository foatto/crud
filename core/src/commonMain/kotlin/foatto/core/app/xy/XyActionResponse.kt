package foatto.core.app.xy

import foatto.core.app.xy.geom.XyPoint
import kotlinx.serialization.Serializable

@Serializable
class XyActionResponse(
    //--- response on GET_COORDS
    val minCoord: XyPoint? = null,
    val maxCoord: XyPoint? = null,

    //--- response on GET_ELEMENTS
    val alElement: List<XyElement> = emptyList(),

    //--- response on GET_ONE_ELEMENT
    val element: XyElement? = null,

    //--- additional custom parameters for any commands
    val hmParam: Map<String, String> = emptyMap(),
)
