package foatto.core_server.app.server.column

import foatto.sql.CoreAdvancedStatement

abstract class ColumnSimple : ColumnAbstract() {

    //--- одновременно как флаг режима работы - как ComboBox с возможностью редактирования (для String/Int/Double)
    val alCombo = mutableListOf<String>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getSortFieldName(index: Int) = alFieldName[index]

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected val savedDefaultPropertyName: String
        get() = "${tableName}_${alFieldName[0]}"

    protected fun addFieldName(vararg fieldNames: String) {
        alFieldName.addAll(fieldNames)
    }

//    protected fun setFieldName(aArrFieldName: Array<String>) {
//        arrFieldName = aArrFieldName
//    }

    fun fillCombo(stm: CoreAdvancedStatement, tableName: String, fieldName: String) {
        val rs = stm.executeQuery(
            """
                SELECT DISTINCT $fieldName 
                FROM $tableName 
                WHERE $fieldName IS NOT NULL 
                ORDER BY $fieldName
            """
        )
        while (rs.next()) {
            alCombo += rs.getString(1).trim()
        }
        rs.close()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun addCombo(s: String) {
        alCombo += s
    }
}
