package foatto.core.app.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartActionResponse(
    //--- answer on GET_ELEMENTS
    val hmIndexColor: Map<ChartColorIndex, Int> = emptyMap(),
    //--- именно List - важен порядок элементов
    val alCharts: List<Pair<String, ChartGroupDTO>> = emptyList(),
    val alVisibleElement: List<Triple<String, String, Boolean>> = emptyList(),
    val alLegend: List<Triple<Int, Boolean, String>> = emptyList(),   // color-index, is-back, descr
)

