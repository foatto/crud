package foatto.ts.core_ts

import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.mAbstract

class ObjectSelector {

    private var fieldObject = "object_id"

    private var selectorAlias: String? = null
    private var selectorColumnTo: iColumn? = null
    private var selectorColumnFrom: iColumn? = null

    lateinit var columnObject: ColumnInt
        private set
    private lateinit var columnObjectName: ColumnString
    private lateinit var columnObjectModel: ColumnString

    fun fillColumns(
        model: mAbstract,
        isRequired: Boolean,
        isSelector: Boolean,
        alTableHiddenColumn: MutableList<iColumn>,
        alFormHiddenColumn: MutableList<iColumn>,
        alFormColumn: MutableList<iColumn>,
        hmParentColumn: MutableMap<String, iColumn>,
        aSingleObjectMode: Boolean,
    ) {

        val columnObjectId = ColumnInt("TS_object", "id")
        columnObject = ColumnInt(model.modelTableName, fieldObject, columnObjectId)

        columnObjectName = ColumnString("TS_object", "name", "Наименование объекта", mAbstract.STRING_COLUMN_WIDTH)
        columnObjectName.isRequired = isRequired
        columnObjectModel = ColumnString("TS_object", "model", "Модель", mAbstract.STRING_COLUMN_WIDTH)

        if (isSelector) {
            columnObjectName.selectorAlias = if (selectorAlias == null) {
                "ts_object"
            } else {
                selectorAlias
            }
            //--- только для "родного" алиаса
            if (selectorAlias == null) {
                columnObjectName.addSelectorColumn(columnObject, columnObjectId)
            } else {
                columnObjectName.addSelectorColumn(selectorColumnTo!!, selectorColumnFrom!!)
            }
            columnObjectName.addSelectorColumn(columnObjectName)
            columnObjectName.addSelectorColumn(columnObjectModel)
        }

        //--------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnObject)

        if (aSingleObjectMode) {
            alTableHiddenColumn.add(columnObjectName)
            alTableHiddenColumn.add(columnObjectModel)
        } else {
            model.addTableColumn(columnObjectName)
            model.addTableColumn(columnObjectModel)
        }

        alFormHiddenColumn.add(columnObject)

        if (aSingleObjectMode) {
            alFormHiddenColumn.add(columnObjectName)
            alFormHiddenColumn.add(columnObjectModel)
        } else {
            alFormColumn.add(columnObjectName)
            alFormColumn.add(columnObjectModel)
        }

        hmParentColumn["ts_object"] = columnObject
    }

    fun setSelectorAlias(aSelectorAlias: String, aSelectorColumnTo: iColumn, aSelectorColumnFrom: iColumn) {
        selectorAlias = aSelectorAlias
        selectorColumnTo = aSelectorColumnTo
        selectorColumnFrom = aSelectorColumnFrom
    }
}
