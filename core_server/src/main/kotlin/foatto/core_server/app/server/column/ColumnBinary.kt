package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataBinary

class ColumnBinary( aTableName: String, aFieldName: String ) : ColumnSimple() {

    init {
        tableName = aTableName
        addFieldName( aFieldName )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isSortable() = false
    override fun getSortFieldName( index: Int ) = ""

    override fun getData() = DataBinary( this )
}
