package foatto.mms.core_mms.report

import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.iColumn

class SumOptionSelector {

    lateinit var columnSumOnly: ColumnBoolean
        private set
    lateinit var columnSumUser: ColumnBoolean
        private set
    lateinit var columnSumObject: ColumnBoolean
        private set

    fun fillColumns( userConfig: UserConfig, tableSOS: String, alFormColumn: MutableList<iColumn> ) {

        columnSumOnly = ColumnBoolean( tableSOS, "sum_only", "Выводить только суммы", false )
            columnSumOnly.isVirtual = true
            columnSumOnly.setSavedDefault( userConfig )
        columnSumUser = ColumnBoolean( tableSOS, "sum_user", "Выводить суммы по владельцам", true )
            columnSumUser.isVirtual = true
            columnSumUser.setSavedDefault( userConfig )
        columnSumObject = ColumnBoolean( tableSOS, "sum_object", "Выводить суммы по объектам", true )
            columnSumObject.isVirtual = true
            columnSumObject.setSavedDefault( userConfig )
            columnSumObject.addFormVisible( FormColumnVisibleData( columnSumUser, true, intArrayOf( 1 ) ) )

        //----------------------------------------------------------------------------------------------------------------------

        alFormColumn.add( columnSumOnly )
        alFormColumn.add( columnSumUser )
        alFormColumn.add( columnSumObject )
    }
}
