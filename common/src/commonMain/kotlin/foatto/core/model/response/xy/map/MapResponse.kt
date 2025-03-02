package foatto.core.model.response.xy.map

import foatto.core.model.response.HeaderData
import foatto.core.model.response.xy.XyElementConfig
import kotlinx.serialization.Serializable

@Serializable
class MapResponse(
    val elementConfigs: Map<String, XyElementConfig>,

    val tabCaption: String,
    val headerData: HeaderData,

    val timeRangeType: Int,
    val begTime: Int,
    val endTime: Int,
)
