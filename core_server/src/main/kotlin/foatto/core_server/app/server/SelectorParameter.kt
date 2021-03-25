package foatto.core_server.app.server

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.iData

class SelectorParameter {
    lateinit var formAlias: String
    var recordId = 0
    var refererId: String? = null
    lateinit var hmParentData: Map<String, Int>
    var parentUserId: Int = 0

    lateinit var selectorAlias: String
    var selectedId = 0
    var selectedParentId = 0
    lateinit var alColumnTo: List<iColumn>
    lateinit var alColumnFrom: List<iColumn>

    var forClear = false    // флаг очистки выбора

    lateinit var hmColumnData: Map<iColumn, iData>
}
