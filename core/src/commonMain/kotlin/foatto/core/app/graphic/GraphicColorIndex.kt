package foatto.core.app.graphic

import kotlinx.serialization.Serializable

@Serializable
enum class GraphicColorIndex {
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

    //--- для дополнительного графика
    LINE_NONE_2,        // line of no values
    LINE_NORMAL_2,      // normal line

    //--- для дополнительного графика
    LINE_NONE_3,        // line of no values
    LINE_NORMAL_3,      // normal line
}

val graphicAxisColorIndexes = listOf(
    GraphicColorIndex.AXIS_0,
    GraphicColorIndex.AXIS_1,
    GraphicColorIndex.AXIS_2,
    GraphicColorIndex.AXIS_3,
)

val graphicLineNoneColorIndexes = listOf(
    GraphicColorIndex.LINE_NONE_0,
    GraphicColorIndex.LINE_NONE_1,
    GraphicColorIndex.LINE_NONE_2,
    GraphicColorIndex.LINE_NONE_3,
)

val graphicLineNormalColorIndexes = listOf(
    GraphicColorIndex.LINE_NORMAL_0,
    GraphicColorIndex.LINE_NORMAL_1,
    GraphicColorIndex.LINE_NORMAL_2,
    GraphicColorIndex.LINE_NORMAL_3,
)

val graphicLineBelowColorIndexes = listOf(
    GraphicColorIndex.LINE_BELOW_0,
    GraphicColorIndex.LINE_BELOW_0,
    GraphicColorIndex.LINE_BELOW_0,
    GraphicColorIndex.LINE_BELOW_0,
)

val graphicLineAboveColorIndexes = listOf(
    GraphicColorIndex.LINE_ABOVE_0,
    GraphicColorIndex.LINE_ABOVE_0,
    GraphicColorIndex.LINE_ABOVE_0,
    GraphicColorIndex.LINE_ABOVE_0,
)