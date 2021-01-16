package foatto.core.app.xy

import foatto.core.app.xy.geom.XyPoint

class XyActionResponse(
    //--- response on GET_COORDS
    val minCoord: XyPoint? = null,
    val maxCoord: XyPoint? = null,

    //--- response on GET_ELEMENTS
    val alElement: Array<XyElement>? = null,

    //--- response on GET_ONE_ELEMENT
    val element: XyElement? = null
)
