package foatto.core.model.response.composite

import foatto.core.model.AppAction
import foatto.core.model.response.chart.ChartResponse
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.xy.map.MapResponse
import foatto.core.model.response.xy.scheme.SchemeResponse
import kotlinx.serialization.Serializable

@Serializable
class CompositeBlock(
    val id: Int,

    val isStatic: Boolean,

    val isHidden: Boolean,

    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,

    val action: AppAction,

    val chartResponse: ChartResponse? = null,
    val mapResponse: MapResponse? = null,
    val schemeResponse: SchemeResponse? = null,
    val tableResponse: TableResponse? = null,
)