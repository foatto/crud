package foatto.core.link
 
class TableResponse(
    val tab: String,
    val alHeader: List<Pair<String,String>>,
    val selectorCancelURL: String,
    val findURL: String,
    val findText: String,
    val alAddActionButton: List<AddActionButton>,
    val alServerActionButton: List<ServerActionButton>,
    val alClientActionButton: List<ClientActionButton>,
    val alColumnCaption: List<Pair<String,String>>,
    val alTableCell: List<TableCell>,
    val alTableRowData: List<TableRowData>,
    val selectedRow: Int,
    val alPageButton: List<Pair<String,String>>
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

    var alCellData = mutableListOf<TableCellData>()

    //--- данные для CHECKBOX

    var booleanValue = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- вспомогательный конструктор
    private constructor( aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int, aMinWidth: Int, aCellType: TableCellType ) : this( aRow, aCol ) {
        rowSpan = aRowSpan
        colSpan = aColSpan
        minWidth = aMinWidth
        cellType = aCellType
    }


    //--- пустая бесцветная ячейка с растяжкой
    constructor( aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int ) : this( aRow, aCol, aRowSpan, aColSpan, 0, TableCellType.TEXT )

    //--- бесцветная ячейка с одним TEXT с иконкой / картинкой / текстом
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aIsWordWrap: Boolean,
        aTooltip: String,

        aIcon: String = "",
        aImage: String = "",
        aText: String = ""

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, TableCellType.TEXT )  {

        align = aAlign
        isWordWrap = aIsWordWrap
        tooltip = aTooltip

        addCellData(
            aIcon = aIcon,
            aImage = aImage,
            aText = aText
        )
    }

    //--- бесцветная ячейка с одним BUTTON с иконкой / картинкой / текстом
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aIcon: String = "",
        aImage: String = "",
        aText: String = "",

        aUrl: String,
        aInNewWindow: Boolean = false

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, TableCellType.BUTTON )  {

        align = aAlign
        tooltip = aTooltip

        addCellData(
            aIcon = aIcon,
            aImage = aImage,
            aText = aText,
            aUrl = aUrl,
            aInNewWindow = aInNewWindow
        )
    }

    //--- бесцветная пустая ячейка-заготовка для нескольких BUTTON с иконкой / картинкой / текстом
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, TableCellType.BUTTON )  {

        align = aAlign
        tooltip = aTooltip
    }

    //--- бесцветная ячейка с одним TEXT / BUTTON с иконкой / картинкой / текстом
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aCellType: TableCellType,

        aIcon: String = "",
        aImage: String = "",
        aText: String = "",

        aUrl: String = "",
        aInNewWindow: Boolean = false

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, aCellType )  {

        align = aAlign
        tooltip = aTooltip

        addCellData(
            aIcon = aIcon,
            aImage = aImage,
            aText = aText,
            aUrl = aUrl,
            aInNewWindow = aInNewWindow
        )
    }

    //--- чекбокс
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aBooleanValue: Boolean

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, TableCellType.CHECKBOX )  {

        align = aAlign
        tooltip = aTooltip

        booleanValue = aBooleanValue
    }

    //--- добавляем строк в TEXT / BUTTON

    fun addCellData(
        aIcon: String = "",
        aImage: String = "",
        aText: String = "",
        aUrl: String = "",
        aInNewWindow: Boolean = false
    ) {
        alCellData.add( TableCellData(
            icon = aIcon,
            image = aImage,
            text = aText,
            url = aUrl,
            inNewWindow = aInNewWindow
        ) )
    }

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
    val formURL: String = "",
    val rowURL: String = "",
    val itRowURLInNewWindow: Boolean = false,
    val gotoURL: String = "",
    val itGotoURLInNewWindow: Boolean = false,
    val alPopupData: List<TablePopupData> = listOf()
)

class TablePopupData( val group: String, val url: String, val text: String, val inNewWindow: Boolean )


