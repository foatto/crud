package foatto.core_server.app.server.column

import foatto.core.link.TableCellAlign
import foatto.core_server.app.server.data.DataFile

class ColumnFile( aTableName: String, aFieldName: String, aCaption: String ) : ColumnSimple() {

//    //--- для картинок
//    private int imageBigWidth = 0;
//    private int imageBigHeight = 0;
//    private int imageSmallWidth = 0;
//    private int imageSmallHeight = 0;

    init {
        tableName = aTableName
        addFieldName( aFieldName )
        caption = aCaption

        tableAlign = TableCellAlign.CENTER
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataFile( this )

    override fun isSortable(): Boolean = false
}
