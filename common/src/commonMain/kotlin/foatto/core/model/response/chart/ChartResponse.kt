package foatto.core.model.response.chart

import foatto.core.model.response.HeaderData
import kotlinx.serialization.Serializable

@Serializable
class ChartResponse(
    val tabCaption: String,
    val headerData: HeaderData,
)
