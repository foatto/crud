package foatto.core.model.response.chart

import kotlinx.serialization.Serializable

@Serializable
enum class ChartColorIndex {
    FILL_NEUTRAL,       // neutral text background
    FILL_NORMAL,        // normal value text background
    FILL_WARNING,       // warning text background
    FILL_CRITICAL,      // critical text background

    BORDER_NEUTRAL,     // neutral text border
    BORDER_NORMAL,      // normal value text border
    BORDER_WARNING,     // warning text border
    BORDER_CRITICAL,    // critical text border

    TEXT_NEUTRAL,       // neutral text color
    TEXT_NORMAL,        // normal value text color
    TEXT_WARNING,       // warning text color
    TEXT_CRITICAL,      // critical text color

    POINT_NEUTRAL,      // point with neutral value
    POINT_NORMAL,       // point with normal value
    POINT_BELOW,        // point below the corresponding threshold
    POINT_ABOVE,        // point above the corresponding threshold

    AXIS_0,             // Y-axis for main plot
    AXIS_1,             // Y-axis for additional plot
    AXIS_2,             // Y-axis for additional plot
    AXIS_3,             // Y-axis for additional plot

    LINE_LIMIT,         // line showing boundary values

    //--- for the main plot
    LINE_NONE_0,        // line of no values
    LINE_NORMAL_0,      // normal line
    LINE_BELOW_0,       // line below the corresponding threshold
    LINE_ABOVE_0,       // line above the corresponding threshold

    //--- for the additional plot
    LINE_NONE_1,        // line of no values
    LINE_NORMAL_1,      // normal line

    //--- for the additional plot
    LINE_NONE_2,        // line of no values
    LINE_NORMAL_2,      // normal line

    //--- for the additional plot
    LINE_NONE_3,        // line of no values
    LINE_NORMAL_3,      // normal line
}

private val chartAxisColorIndexes: List<ChartColorIndex> = listOf(
    ChartColorIndex.AXIS_0,
    ChartColorIndex.AXIS_1,
    ChartColorIndex.AXIS_2,
    ChartColorIndex.AXIS_3,
)

private val chartLineNoneColorIndexes: List<ChartColorIndex> = listOf(
    ChartColorIndex.LINE_NONE_0,
    ChartColorIndex.LINE_NONE_1,
    ChartColorIndex.LINE_NONE_2,
    ChartColorIndex.LINE_NONE_3,
)

private val chartLineNormalColorIndexes: List<ChartColorIndex> = listOf(
    ChartColorIndex.LINE_NORMAL_0,
    ChartColorIndex.LINE_NORMAL_1,
    ChartColorIndex.LINE_NORMAL_2,
    ChartColorIndex.LINE_NORMAL_3,
)

private val chartLineBelowColorIndexes: List<ChartColorIndex> = listOf(
    ChartColorIndex.LINE_BELOW_0,
    ChartColorIndex.LINE_BELOW_0,
    ChartColorIndex.LINE_BELOW_0,
    ChartColorIndex.LINE_BELOW_0,
)

private val chartLineAboveColorIndexes: List<ChartColorIndex> = listOf(
    ChartColorIndex.LINE_ABOVE_0,
    ChartColorIndex.LINE_ABOVE_0,
    ChartColorIndex.LINE_ABOVE_0,
    ChartColorIndex.LINE_ABOVE_0,
)

fun getChartAxisColorIndexes(index: Int): ChartColorIndex = chartAxisColorIndexes[index % chartAxisColorIndexes.size]
fun getChartLineNoneColorIndexes(index: Int): ChartColorIndex = chartLineNoneColorIndexes[index % chartLineNoneColorIndexes.size]
fun getChartLineNormalColorIndexes(index: Int): ChartColorIndex = chartLineNormalColorIndexes[index % chartLineNormalColorIndexes.size]
fun getChartLineBelowColorIndexes(index: Int): ChartColorIndex = chartLineBelowColorIndexes[index % chartLineBelowColorIndexes.size]
fun getChartLineAboveColorIndexes(index: Int): ChartColorIndex = chartLineAboveColorIndexes[index % chartLineAboveColorIndexes.size]

