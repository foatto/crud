package foatto.core.link

class TableResponse(
    val tab: String,
    val alHeader: Array<Pair<String,String>>,
    val selectorCancelURL: String,
    val findURL: String,
    val findText: String,
    val alAddActionButton: Array<AddActionButton>,
    val alServerActionButton: Array<ServerActionButton>,
    val alClientActionButton: Array<ClientActionButton>,
    val alColumnCaption: Array<Pair<String,String>>,
    val alTableCell: Array<TableCell>,
    val alTableRowData: Array<TableRowData>,
    val selectedRow: Int,
    val alPageButton: Array<Pair<String,String>>
)

class AddActionButton(
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String
)

class ServerActionButton(
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String,
    val inNewWindow: Boolean
)

class ClientActionButton(
    val caption: String,
    val tooltip: String,
    val icon: String,
    val className: String,
    val param: String
)

class TableCell( val row: Int, val col: Int ) {

    //--- общие данные для всех типов ячеек ---

    var rowSpan = 1
    var colSpan = 1

    var cellType = TableCellType.TEXT

    var align = TableCellAlign.LEFT

    var minWidth = 0

    var isWordWrap = true

    var tooltip = ""

    var foreColorType = TableCellForeColorType.DEFAULT
    var foreColor = 0   // для foreColorType == FORE_COLOR_TYPE_DEFINED

    var backColorType = TableCellBackColorType.DEFAULT
    var backColor = 0   // для backColorType == BACK_COLOR_TYPE_DEFINED

    var fontStyle = 0

    //--- общие данные для TEXT / BUTTON

    var alCellData = arrayOf<TableCellData>()

    //--- данные для CHECKBOX

    var booleanValue = false

}

enum class TableCellType { TEXT, BUTTON, CHECKBOX }

enum class TableCellAlign { LEFT, CENTER, RIGHT }

enum class TableCellForeColorType {
    DEFINED,    // явно заданный цвет текста
    DEFAULT     // цвет текста по умолчанию (т.е. не меняется)
}

enum class TableCellBackColorType {
    DEFINED,    // явно заданный фоновый цвет
    DEFAULT,    // фоновый цвет по умолчанию (т.е. не меняется)
    GROUP_0,    // фоновый цвет группировки нечётный уровней
    GROUP_1     // фоновый цвет группировки чётных уровней
}

class TableCellData( val icon: String = "", val image: String = "", val text: String = "", val url: String = "", val inNewWindow: Boolean = false )

class TableRowData(
    val formURL: String,
    val rowURL: String,
    val itRowURLInNewWindow: Boolean,
    val gotoURL: String = "",
    val itGotoURLInNewWindow: Boolean = false,
    val alPopupData: Array<TablePopupData>
)

class TablePopupData( val group: String, val url: String, val text: String, val inNewWindow: Boolean )
