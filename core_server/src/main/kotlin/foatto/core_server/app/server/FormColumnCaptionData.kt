@file:JvmName("FormColumnCaptionData")
package foatto.core_server.app.server

import foatto.core_server.app.server.column.iColumn

class FormColumnCaptionData( val columnMaster: iColumn, val caption: String, val arrValue: IntArray )
