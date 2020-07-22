package foatto.core.app.graphic

//import kotlinx.serialization.Serializable

//@Serializable
enum class GraphicColorIndex {
    FILL_NEUTRAL, // фон нейтрального текста
    FILL_NORMAL, // фон текста нормального значения
    FILL_WARNING, // фон текста предупредительного значения
    FILL_CRITICAL, // фон текста критического значения

    BORDER_NEUTRAL, // рамка нейтрального текста
    BORDER_NORMAL, // рамка текста нормального значения
    BORDER_WARNING, // рамка текста предупредительного значения
    BORDER_CRITICAL, // рамка текста критического значения

    TEXT_NEUTRAL, // цвет нейтрального текста
    TEXT_NORMAL, // цвет текста нормального значения
    TEXT_WARNING, // цвет текста предупредительного значения
    TEXT_CRITICAL, // цвет текста критического значения

    POINT_NEUTRAL, // точка с нейтральным значением
    POINT_NORMAL, // точка с нормальным значением
    POINT_WARNING, // точка ниже соответствующего порогового значения
    POINT_CRITICAL, // точка выше соответствующего порогового значения

    AXIS_NEUTRAL, // ось Y для нейтрального графика
    AXIS_0, // ось Y для основного графика
    AXIS_1, // ось Y для дополнительного графика
    AXIS_2, // ось Y для дополнительного графика

    LINE_LIMIT, // линия, отображающая граничные значения
    LINE_NEUTRAL, // линия с нейтральным значением

    //--- для основного графика
    LINE_NONE_0, // линия отсутствия значений
    LINE_NORMAL_0, // линия с нормальным значением
    LINE_WARNING_0, // линия ниже соответствующего порогового значения
    LINE_CRITICAL_0, // линия выше соответствующего порогового значения

    //--- для дополнительного графика
    LINE_NONE_1, // линия отсутствия значений
    LINE_NORMAL_1, // линия с нормальным значением
    LINE_WARNING_1, // линия ниже соответствующего порогового значения
    LINE_CRITICAL_1, // линия выше соответствующего порогового значения

    //--- для дополнительного графика
    LINE_NONE_2, // линия отсутствия значений
    LINE_NORMAL_2, // линия с нормальным значением
    LINE_WARNING_2, // линия ниже соответствующего порогового значения
    LINE_CRITICAL_2, // линия выше соответствующего порогового значения
}