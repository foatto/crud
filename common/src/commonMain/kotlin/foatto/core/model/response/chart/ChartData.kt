package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartData(
    val title: String,
    val legends: List<ChartLegend>,
    val height: Float,
    val alAxisYData: List<ChartAxisY>,
    var elements: List<ChartElement>,
)

