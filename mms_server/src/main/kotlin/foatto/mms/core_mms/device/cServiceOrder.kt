package foatto.mms.core_mms.device

import foatto.core_server.app.server.cStandart

class cServiceOrder : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String =
        super.addSQLWhere( hsTableRenameList ) +
            " AND ${renameTableName( hsTableRenameList, model.tableName )}.${( model as mServiceOrder ).columnOrderCompleted.getFieldName()} = 0 "

}
