package foatto.core_server.app.xy.server.document

import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core_server.app.AppParameter
import foatto.core_server.app.xy.XyStartData

abstract class sdcXyScheme : sdcXyAbstract() {

    companion object {
        //--- it was empirically found that if the step size is too small, side effects from integer division begin
        //--- and the edges of the outline can flush against the edges of the window / screen.
        //--- A step of 1 million points allows you to fit up to 2000 circuit elements (2 billion MAX_INTEGER / 1 million = 2000) with sufficient accuracy.
        const val GRID_STEP = 1024 * 1024
    }

    override fun getElements(xyActionRequest: XyActionRequest): XyActionResponse {

        val xyStartDataID = xyActionRequest.startParamId
        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData

        //--- разбор входных параметров
        val hsReadOnlyObject = mutableSetOf<Int>()
        val alObjectParamData = parseObjectParam(false, sd, hsReadOnlyObject)

        val alElement = mutableListOf<XyElement>()
        val hmParam = mutableMapOf<String, String>()
        //--- загрузка динамических объектов
        for (objectParamData in alObjectParamData) {
            loadDynamicElements(
                scale = xyActionRequest.viewCoord!!.scale,
                objectParamData = objectParamData,
                alElement = alElement,
                hmParams = hmParam,
            )
        }

        return XyActionResponse(
            alElement = alElement,
            hmParam = hmParam,
        )
    }

}
