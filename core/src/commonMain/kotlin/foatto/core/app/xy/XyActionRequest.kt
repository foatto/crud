package foatto.core.app.xy

class XyActionRequest(
    val documentTypeName: String,
    val action: XyAction,
    val startParamID: String,

    //--- GET_ELEMENTS, GET_ONE_ELEMENT
    val viewCoord: XyViewCoord? = null,

    //--- GET_ONE_ELEMENT, ACTION_CLICK_ELEMENT
    val elementID: Int? = null,

    //--- GET_ELEMENTS
    val bitmapTypeName: String? = null,

    //--- CLICK_ELEMENT
    val objectID: Int? = null,

    //--- ADD_ELEMENT, EDIT_ELEMENT_POINT
    val xyElement: XyElement? = null,

    //--- MOVE_ELEMENTS
    val alActionElementIds: List<Int>? = null,
    val dx: Int? = null,
    val dy: Int? = null

) {
    var sessionID: Long = 0

    val hmParam = mutableMapOf<String, String>()
}

enum class XyAction { GET_COORDS, GET_ELEMENTS, GET_ONE_ELEMENT, CLICK_ELEMENT, ADD_ELEMENT, EDIT_ELEMENT_POINT, MOVE_ELEMENTS }

//    String ACTION_ROTATE_ELEMENT = "action_rotate_element";
//    String ACTION_EDIT_ELEMENT_TEXT = "action_edit_element_text";
//    String ACTION_COPY_ELEMENT = "action_copy_element";
//    String ACTION_DELETE_ELEMENT = "action_delete_element";
//    String ACTION_CHANGE_TYPE_ELEMENT = "action_change_type_element";
//    String ACTION_LINK_TO_PARENT = "action_link_to_parent";
//    String ACTION_UNLINK_FROM_PARENT = "action_unlink_from_parent";

//    String PARENT_OBJECT_ID = "parent_object_id";
//
//    String TEXT_PICTURE_MODE = "text_picture_mode";
//    String SKIP_ELEMENT_TYPES = "skip_element_types";
//    String VECTOR_MODE = "vector_mode";
//
//    String ADD_ROTATE_DEGREE = "add_rotate_degree";
//
//    String MOVE_ELEMENT_KEY = "move_element_key";
//    String MOVE_ELEMENT_X = "move_element_x";
//    String MOVE_ELEMENT_Y = "move_element_y";
//
//    String ROTATE_ELEMENT_KEY = "rotate_element_key";
//    String ROTATE_X_POINTS = "rotate_x_points";
//    String ROTATE_Y_POINTS = "rotate_y_points";
//    String ROTATE_DEGREE = "rotate_degree";
//
//    String EDIT_ELEMENT_KEY = "edit_element_key";
//    String EDIT_X_POINTS = "edit_x_points";
//    String EDIT_Y_POINTS = "edit_y_points";
//    String EDIT_TOOL_TIP = "edit_tool_tip";
//    String EDIT_TEXT = "edit_text";
//
//    String DELETE_ELEMENT_KEY = "delete_element_key";
//
//    String CHANGE_TYPE_NEW_ID = "change_type_new_id";
//    String CHANGE_TYPE_ELEMENT_ID = "change_type_element_id";
