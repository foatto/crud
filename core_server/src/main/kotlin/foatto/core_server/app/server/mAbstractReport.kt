package foatto.core_server.app.server

import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedStatement

open class mAbstractReport : mAbstract() {

    companion object {
        val MAX_REPORT_CAP_ROWS = 8
        val MAX_REPORT_SIGNATURE_ROWS = 8
        val MAX_REPORT_SIGNATURE_COLS = 3

        val REPORT_CAP_FIELD_NAME = "report_cap"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private lateinit var columnReporIsShowCap: ColumnBoolean
    var columnReportCap: ColumnString? = null
        protected set
    val alColumnReportSignature: ArrayList<ArrayList<ColumnString>> = ArrayList( MAX_REPORT_SIGNATURE_ROWS )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getSaveButonCaption(aAliasConfig: AliasConfig): String = "Распечатать"

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- именно отдельный метод, а не часть метода init - чтобы были уже проинициализированы tableName и прочие переменные
    protected fun initReportCapAndSignature( aliasConfig: AliasConfig, userConfig: UserConfig ) {

        columnReporIsShowCap = ColumnBoolean( tableName, "is_cap_edit", "Редактировать шапку и подписи", false )
        columnReporIsShowCap.isVirtual = true

        columnReportCap = ColumnString( tableName, REPORT_CAP_FIELD_NAME, "Верхний блок отчёта", MAX_REPORT_CAP_ROWS, STRING_COLUMN_WIDTH, textFieldMaxSize )
        columnReportCap!!.isVirtual = true
        columnReportCap!!.defaultValue = userConfig.getUserProperty( getReportCapPropertyName( aliasConfig ) )
        columnReportCap!!.addFormVisible( FormColumnVisibleData( columnReporIsShowCap, true, intArrayOf( 1 ) ) )

        for( i in 0 until MAX_REPORT_SIGNATURE_ROWS ) {
            val alCS = ArrayList<ColumnString>( MAX_REPORT_SIGNATURE_COLS )
            for( j in 0 until MAX_REPORT_SIGNATURE_COLS ) {
                val cs = ColumnString( tableName, getReportSignatureFieldName( i, j ), StringBuilder( if( j == 0 ) "Подпись отчёта : " else ": " ).append( i + 1 ).toString(), 2,
                                       STRING_COLUMN_WIDTH, textFieldMaxSize )
                cs.isVirtual = true
                cs.defaultValue = userConfig.getUserProperty( getReportSignaturePropertyName( i, j, aliasConfig ) )
                cs.addFormVisible( FormColumnVisibleData( columnReporIsShowCap, true, intArrayOf( 1 ) ) )

                alCS.add( cs )
            }
            alColumnReportSignature.add( alCS )
        }
    }

    protected fun addCapAndSignatureColumns() {
        alFormColumn.add( columnReporIsShowCap )
        alFormColumn.add( columnReportCap!! )
        for( alCS in alColumnReportSignature )
            for( cs in alCS )
                alFormColumn.add( cs )
    }

    fun getReportCapPropertyName( aliasConfig: AliasConfig ): String = "${REPORT_CAP_FIELD_NAME}_${aliasConfig.alias}"

    fun getReportSignatureFieldName( i: Int, j: Int ): String = "report_signature_${i}_$j"

    fun getReportSignaturePropertyName( i: Int, j: Int, aliasConfig: AliasConfig ): String = "${getReportSignatureFieldName( i, j )}_${aliasConfig.alias}"

}
