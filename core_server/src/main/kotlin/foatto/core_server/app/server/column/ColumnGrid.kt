package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataGrid

class ColumnGrid(aTableName: String, aFieldName: String, aCaption: String) : ColumnAbstract() {

    init {
        columnTableName = aTableName
        alFieldName.add(aFieldName)
        caption = aCaption
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataGrid(this)

    override fun isSortable(): Boolean = false

    override fun getSortFieldName(index: Int) = alFieldName[index]
}
