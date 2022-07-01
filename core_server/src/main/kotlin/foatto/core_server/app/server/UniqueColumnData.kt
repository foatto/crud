package foatto.core_server.app.server

import foatto.core_server.app.server.column.iUniqableColumn

class UniqueColumnData(
    val column: iUniqableColumn,
    val ignore: Any? = null,
)
