package foatto.core.app.chart

import kotlinx.serialization.Serializable

@Serializable
enum class AnalogueValueType(
    val value: Int,
) {
    NONE(0),
    NORMAL(1),
    BELOW(2),
    ABOVE(3),
//    NEUTRAL(-1 или 4),
//    WARNING(5),
//    CRITICAL(6),
}