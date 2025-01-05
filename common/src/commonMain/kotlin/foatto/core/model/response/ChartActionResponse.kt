package foatto.core.model.response

import foatto.core.model.response.chart.ChartColorIndex
import foatto.core.model.response.chart.ChartData
import kotlinx.serialization.Serializable

@Serializable
class ChartActionResponse(
    override val responseCode: ResponseCode,

    //--- answer on GET_ELEMENTS
    val colorIndexes: Map<ChartColorIndex, Int> = emptyMap(),
    //--- именно List - важен порядок элементов
    val charts: List<Pair<String, ChartData>> = emptyList(),
    val visibleElements: List<Triple<String, String, Boolean>> = emptyList(),
    val legends: List<Triple<Int, Boolean, String>> = emptyList(),   // color-index, is-back, descr

//    //--- additional custom parameters for any commands
//    val hmParam: Map<String, String> = emptyMap(),
) : BaseResponse()

