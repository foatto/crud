package foatto.core_server.app.server.column

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.data.DataFile

class ColumnFile(
    val application: iApplication,
    aTableName: String,
    aFieldName: String,
    aCaption: String
) : ColumnSimple() {

//    //--- for pictures
//    private int imageBigWidth = 0;
//    private int imageBigHeight = 0;
//    private int imageSmallWidth = 0;
//    private int imageSmallHeight = 0;

    init {
        columnTableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption

        tableAlign = TableCellAlign.CENTER
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataFile(application, this)

    override fun isSortable(): Boolean = false
}
