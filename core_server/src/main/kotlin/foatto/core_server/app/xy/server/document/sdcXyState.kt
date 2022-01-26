package foatto.core_server.app.xy.server.document

import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core_server.app.AppParameter
import foatto.core_server.app.xy.XyStartData

abstract class sdcXyState : sdcXyAbstract() {

    override fun getElements(xyActionRequest: XyActionRequest): XyActionResponse {

        val xyStartDataID = xyActionRequest.startParamId
        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData

        //--- разбор входных параметров
        val hsReadOnlyObject = mutableSetOf<Int>()
        val alObjectParamData = parseObjectParam(false, sd, hsReadOnlyObject)

        val alElement = mutableListOf<XyElement>()
        val alParams = mutableMapOf<String, String>()
        //--- загрузка динамических объектов
        for (objectParamData in alObjectParamData) {
            loadDynamicElements(
                scale = xyActionRequest.viewCoord!!.scale,
                objectParamData = objectParamData,
                alElement = alElement,
                hmParams = alParams,
            )
        }

        return XyActionResponse(
            arrElement = alElement.toTypedArray(),
            arrParams = alParams.toList().toTypedArray(),
        )
    }

}
