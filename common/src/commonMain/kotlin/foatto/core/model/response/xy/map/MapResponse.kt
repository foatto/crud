package foatto.core.model.response.xy.map

import foatto.core.model.response.xy.XyDocumentConfig
import kotlinx.serialization.Serializable

@Serializable
class MapResponse(
    val documentConfig: XyDocumentConfig,
    val startParamId: String,

    val tabCaption: String,
    val headers: List<String>,

    val timeRangeType: Int,
    val begTime: Int,
    val endTime: Int,
)
