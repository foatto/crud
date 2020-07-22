package foatto.core_server.app.server

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.iData

class SelectorParameter {
    lateinit var formAlias: String
    var recordID = 0
    var refererID: String? = null
    lateinit var hmParentData: Map<String, Int>
    var parentUserID: Int = 0

    lateinit var selectorAlias: String
    var selectID = 0                            //!!!текущее значение селектора - убрать после "сокращенного выбора пользователя"
    lateinit var alColumnTo: List<iColumn>
    lateinit var alColumnFrom: List<iColumn>

    var forClear = false    // флаг очистки выбора

    lateinit var hmColumnData: Map<iColumn, iData>
}
