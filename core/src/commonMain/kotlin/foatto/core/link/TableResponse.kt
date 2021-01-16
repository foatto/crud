package foatto.core.link
 
class TableResponse(
    val tab: String,
    val alHeader: Array<Pair<String, String>>,
    val selectorCancelURL: String,
    val findURL: String,
    val findText: String,
    val alAddActionButton: Array<AddActionButton>,
    val alServerActionButton: Array<ServerActionButton>,
    val alClientActionButton: Array<ClientActionButton>,
    val alColumnCaption: Array<Pair<String, String>>,
    val alTableCell: Array<TableCell>,
    val alTableRowData: Array<TableRowData>,
    val selectedRow: Int,
    val alPageButton: Array<Pair<String, String>>
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

    //--- common data for all cell types

    var rowSpan = 1
    var colSpan = 1

    var cellType = TableCellType.TEXT

    var align = TableCellAlign.LEFT

    var minWidth = 0

    var isWordWrap = true

    var tooltip = ""

    var foreColorType = TableCellForeColorType.DEFAULT
    var foreColor = 0   // for foreColorType == FORE_COLOR_TYPE_DEFINED

    var backColorType = TableCellBackColorType.DEFAULT
    var backColor = 0   // for backColorType == BACK_COLOR_TYPE_DEFINED

    var fontStyle = 0

    //--- common data for TEXT / BUTTON

    var alCellData = arrayOf<TableCellData>()

    //--- data for CHECKBOX

    var booleanValue = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private constructor( aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int, aMinWidth: Int, aCellType: TableCellType ) : this( aRow, aCol ) {
        rowSpan = aRowSpan
        colSpan = aColSpan
        minWidth = aMinWidth
        cellType = aCellType
    }

    //--- empty colorless cell with a stretch
    constructor( aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int ) : this( aRow, aCol, aRowSpan, aColSpan, 0, TableCellType.TEXT )

    //--- colorless cell with one TEXT with icon / picture / text
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

    //--- colorless cell with one BUTTON with an icon / picture / text
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

    //--- colorless empty blank cell for several BUTTONs with an icon / picture / text
    constructor(
        aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int,
        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String

    ) : this( aRow, aCol, aRowSpan, aColSpan, aMinWidth, TableCellType.BUTTON )  {

        align = aAlign
        tooltip = aTooltip
    }

    //--- colorless cell with one TEXT / BUTTON with an icon / picture / text
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

    //--- checkbox
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

    //--- add a line to TEXT / BUTTON

    fun addCellData(
        aIcon: String = "",
        aImage: String = "",
        aText: String = "",
        aUrl: String = "",
        aInNewWindow: Boolean = false
    ) {
        alCellData = alCellData.toMutableList().apply {
            add(
                TableCellData(
                    icon = aIcon,
                    image = aImage,
                    text = aText,
                    url = aUrl,
                    inNewWindow = aInNewWindow
                )
            )
        }.toTypedArray()
    }

}

enum class TableCellType { TEXT, BUTTON, CHECKBOX }

enum class TableCellAlign { LEFT, CENTER, RIGHT }

enum class TableCellForeColorType {
    DEFINED,
    DEFAULT
}

enum class TableCellBackColorType {
    DEFINED,
    DEFAULT,
    GROUP_0,
    GROUP_1
}

class TableCellData( val icon: String = "", val image: String = "", val text: String = "", val url: String = "", val inNewWindow: Boolean = false )

class TableRowData(
    val formURL: String = "",
    val rowURL: String = "",
    val itRowURLInNewWindow: Boolean = false,
    val gotoURL: String = "",
    val itGotoURLInNewWindow: Boolean = false,
    val alPopupData: Array<TablePopupData> = arrayOf()
)

class TablePopupData( val group: String, val url: String, val text: String, val inNewWindow: Boolean )


