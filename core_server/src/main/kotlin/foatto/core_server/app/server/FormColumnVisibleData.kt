package foatto.core_server.app.server

import foatto.core_server.app.server.column.iColumn

class FormColumnVisibleData(val columnMaster: iColumn, val state: Boolean, val values: Set<Int>)
