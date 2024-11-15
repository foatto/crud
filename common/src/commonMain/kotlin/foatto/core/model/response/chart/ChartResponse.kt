package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
class ChartResponse(
    val documentTypeName: String,
    val startParamId: String,

    val tabCaption: String,
    val headers: List<String>,

    val timeRangeType: Int,
    val begTime: Int,
    val endTime: Int,
)
