package foatto.core.link

class TableResponse(
    val tab: String,
    val arrHeader: Array<Pair<String, String>>,
    val selectorCancelURL: String,
    val findURL: String,
    val findText: String,
    val arrAddActionButton: Array<AddActionButton>,
    val arrServerActionButton: Array<ServerActionButton>,
    val arrClientActionButton: Array<ClientActionButton>,
    val arrColumnCaption: Array<Pair<String, String>>,
    val arrTableCell: Array<TableCell>,
    val arrTableRowData: Array<TableRowData>,
    val selectedRow: Int,
    val arrPageButton: Array<Pair<String, String>>
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
    val inNewWindow: Boolean,
    val isForWideScreenOnly: Boolean,
)

class ClientActionButton(
    val caption: String,
    val tooltip: String,
    val icon: String,
    val action: String,
    val params: Array<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)

class TableCell(val row: Int, val col: Int) {

    //--- common data for all cell types

    var rowSpan = 1
    var colSpan = 1

    //--- number of logic/data row
    var dataRow = -1

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

    //--- data for CHECKBOX
    var booleanValue = false

    //--- data for TEXT
    var textCellData = TableTextCellData()

    //--- data for BUTTON
    var arrButtonCellData = arrayOf<TableButtonCellData>()

    //--- data for GRID
    var arrGridCellData: Array<Array<TableGridCellData>> = arrayOf()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private constructor(aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int, aDataRow: Int, aMinWidth: Int, aCellType: TableCellType) : this(aRow, aCol) {
        rowSpan = aRowSpan
        colSpan = aColSpan
        dataRow = aDataRow
        minWidth = aMinWidth
        cellType = aCellType
    }

    //--- empty colorless cell with a stretch
    constructor(aRow: Int, aCol: Int, aRowSpan: Int, aColSpan: Int, aDataRow: Int) : this(aRow, aCol, aRowSpan, aColSpan, aDataRow, 0, TableCellType.TEXT)

    //--- checkbox cell
    constructor(
        aRow: Int,
        aCol: Int,
        aRowSpan: Int,
        aColSpan: Int,
        aDataRow: Int,

        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aBooleanValue: Boolean

    ) : this(aRow, aCol, aRowSpan, aColSpan, aDataRow, aMinWidth, TableCellType.CHECKBOX) {

        align = aAlign
        tooltip = aTooltip

        booleanValue = aBooleanValue
    }

    //--- colorless cell with one TEXT with icon / picture / text
    constructor(
        aRow: Int,
        aCol: Int,
        aRowSpan: Int,
        aColSpan: Int,
        aDataRow: Int,

        aAlign: TableCellAlign,
        aMinWidth: Int,
        aIsWordWrap: Boolean,
        aTooltip: String,

        aIcon: String = "",
        aImage: String = "",
        aText: String = ""

    ) : this(aRow, aCol, aRowSpan, aColSpan, aDataRow, aMinWidth, TableCellType.TEXT) {

        align = aAlign
        isWordWrap = aIsWordWrap
        tooltip = aTooltip

        textCellData = TableTextCellData(
            icon = aIcon,
            image = aImage,
            text = aText,
        )
    }

    //--- colorless cell with one BUTTON with an icon / picture / text
    constructor(
        aRow: Int,
        aCol: Int,
        aRowSpan: Int,
        aColSpan: Int,
        aDataRow: Int,

        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aIcon: String = "",
        aImage: String = "",
        aText: String = "",

        aUrl: String,
        aInNewWindow: Boolean = false

    ) : this(aRow, aCol, aRowSpan, aColSpan, aDataRow, aMinWidth, TableCellType.BUTTON) {

        align = aAlign
        tooltip = aTooltip

        addButtonCellData(
            aIcon = aIcon,
            aImage = aImage,
            aText = aText,
            aUrl = aUrl,
            aInNewWindow = aInNewWindow
        )
    }

    //--- colorless empty blank cell for several BUTTONs or GRIDs with an icon / picture / text
    constructor(
        aRow: Int,
        aCol: Int,
        aRowSpan: Int,
        aColSpan: Int,
        aDataRow: Int,

        aAlign: TableCellAlign,
        aMinWidth: Int,
        aTooltip: String,

        aCellType: TableCellType,

    ) : this(aRow, aCol, aRowSpan, aColSpan, aDataRow, aMinWidth, aCellType) {

        align = aAlign
        tooltip = aTooltip
    }

    //--- add a line to BUTTON
    fun addButtonCellData(
        aIcon: String = "",
        aImage: String = "",
        aText: String = "",
        aUrl: String = "",
        aInNewWindow: Boolean = false
    ) {
        arrButtonCellData = arrButtonCellData.toMutableList().apply {
            add(
                TableButtonCellData(
                    icon = aIcon,
                    image = aImage,
                    text = aText,
                    url = aUrl,
                    inNewWindow = aInNewWindow
                )
            )
        }.toTypedArray()
    }

    //--- add a cell of GRID
    fun addGridCellData(
        aIcon: String = "",
        aImage: String = "",
        aText: String = "",
        aNewRow: Boolean = false,
    ) {
        if (aNewRow) {
            arrGridCellData = arrGridCellData.toMutableList().apply {
                add(arrayOf())
            }.toTypedArray()
        }
        arrGridCellData[arrGridCellData.lastIndex] = arrGridCellData[arrGridCellData.lastIndex].toMutableList().apply {
            add(
                TableGridCellData(
                    icon = aIcon,
                    image = aImage,
                    text = aText,
                )
            )
        }.toTypedArray()
    }

}

enum class TableCellType {
    CHECKBOX,
    TEXT,
    BUTTON,
    GRID
}

enum class TableCellAlign {
    LEFT,
    CENTER,
    RIGHT
}

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

class TableTextCellData(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
)

class TableButtonCellData(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
    val url: String = "",
    val inNewWindow: Boolean = false
)

class TableGridCellData(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
//    val url: String = "",
//    val inNewWindow: Boolean = false
)

class TableRowData(
    val formURL: String = "",
    val rowURL: String = "",
    val itRowURLInNewWindow: Boolean = false,
    val gotoURL: String = "",
    val itGotoURLInNewWindow: Boolean = false,
    val alPopupData: Array<TablePopupData> = arrayOf()
)

class TablePopupData(
    val group: String,
    val url: String,
    val text: String,
    val inNewWindow: Boolean
)


