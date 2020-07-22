package foatto.core.app.xy

import foatto.core.app.xy.geom.XyPoint

class XyActionResponse(
    //--- ответ на GET_COORDS
    val minCoord: XyPoint? = null,
    val maxCoord: XyPoint? = null,

    //--- ответ на GET_ELEMENTS
    val alElement: Array<XyElement>? = null,

    //--- ответ на GET_ONE_ELEMENT
    val element: XyElement? = null
)
