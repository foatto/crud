package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartGroupDTO(
    val chartTitle: String,
    val alLegend: List<Triple<Int, Boolean, String>>,   // color-index, is-back, descr
    val chartHeight: Float,
    val alAxisYData: List<ChartAxisYDTO>,
    var alGDC: List<ChartElementDTO>,
)

