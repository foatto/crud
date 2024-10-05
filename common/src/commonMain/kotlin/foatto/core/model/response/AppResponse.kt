package foatto.core.model.response

import foatto.core.model.response.chart.ChartResponse
import foatto.core.model.response.composite.CompositeResponse
import foatto.core.model.response.form.FormResponse
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.xy.map.MapResponse
import foatto.core.model.response.xy.scheme.SchemeResponse
import kotlinx.serialization.Serializable

@Serializable
class AppResponse(
    override val responseCode: ResponseCode,

    val table: TableResponse? = null,
    val form: FormResponse? = null,
    val chart: ChartResponse? = null,
    val map: MapResponse? = null,
    val scheme: SchemeResponse? = null,
    val composite: CompositeResponse? = null,
) : BaseResponse()

