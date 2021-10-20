package foatto.core_server.app.xy.server

object XyProperty {
    const val TOOL_TIP_TEXT = "tool_tip_text"

    //--- единственный случай использования - флаг замкнутости полигона
    //--- (его не выразить заданием цвета рисования/закрашивания)
    const val IS_CLOSED = "is_closed"
    const val DRAW_COLOR = "draw_color"
    const val FILL_COLOR = "fill_color"
    const val LINE_WIDTH = "line_width"
    const val ROTATE_DEGREE = "rotate_degree"

    const val IMAGE_NAME = "image_name"
    const val IMAGE_WIDTH = "image_width"
    const val IMAGE_HEIGHT = "image_height"

    const val IMAGE_ANCHOR_X = "image_anchor_x"
    const val IMAGE_ANCHOR_Y = "image_anchor_y"

    const val MARKER_TYPE = "marker_type"
    const val MARKER_SIZE = "marker_size"
    const val MARKER_SIZE_2 = "marker_size_2"

    const val ARROW_POS = "arrow_pos"
    const val ARROW_LEN = "arrow_len"
    const val ARROW_HEIGHT = "arrow_height"
    const val ARROW_LINE_WIDTH = "arrow_line_width"

    const val TEXT = "text"
    const val TEXT_COLOR = "text_color"
    const val FONT_SIZE = "font_size"
    const val FONT_BOLD = "font_bold"
}
