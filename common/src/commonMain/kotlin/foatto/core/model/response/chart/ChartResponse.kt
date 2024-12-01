package foatto.core.model.response.chart

import foatto.core.model.response.HeaderData
import kotlinx.serialization.Serializable

@Serializable
class ChartResponse(
    val documentTypeName: String,
    val startParamId: String,

    val tabCaption: String,
    val headerData: HeaderData,

    val timeRangeType: Int,
    val begTime: Int,
    val endTime: Int,
)
