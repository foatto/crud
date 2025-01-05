package foatto.compose.control.model.chart

internal class ChartGroupClient(
    val title: ChartTextDrawData,
    val alAxisYLine: MutableList<ChartLineDrawData>,
    val alAxisYText: MutableList<ChartTextDrawData>,
    val alAxisXLine: MutableList<ChartLineDrawData>,
    val alAxisXText: MutableList<ChartTextDrawData>,
    val alChartBack: MutableList<ChartRectDrawData>,
    val alChartLine: MutableList<ChartLineDrawData>,
//    val alChartPoint: MutableList<ChartCircleDrawData>, - пок не используется (?)
    val alChartText: MutableList<ChartTextDrawData>,
    val alLegendBack: MutableList<ChartRectDrawData>,
    val alLegendText: MutableList<ChartTextDrawData>,
)
