package foatto.ts.core_ts

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.app.system.cUser
import foatto.ts.iTSApplication

class cCompany : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val tn = renameTableName(hsTableRenameList, model.modelTableName)
        val mc = model as mCompany

        val companiesParentId = (application as iTSApplication).companiesParentId.toIntOrNull() ?: 0

        return super.addSQLWhere(hsTableRenameList) +
            """    
                AND $tn.${mc.columnId.getFieldName()} > 0 
                AND $tn.${mc.columnRecordType.getFieldName()} = ${OrgType.ORG_TYPE_DIVISION} 
                AND $tn.${mc.columnParent.getFieldName()} = $companiesParentId 
            """
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mc = model as mCompany
        if (column == mc.columnRecordFullName) {
            val isDisabled = (hmColumnData[mc.columnDisabled] as DataBoolean).value

            //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = cUser.getUserNameColor(isDisabled, getCurrentTimeInt())
        }
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val mc = model as mCompany
        val disabledValue = if ((hmColumnData[mc.columnDisabled] as DataBoolean).value) {
            1
        } else {
            0
        }
        val fullName = (hmColumnData[mc.columnRecordFullName] as DataString).text

        val stmUser = conn.createStatement()
        stmUser.executeUpdate(
            """
                INSERT INTO SYSTEM_users( 
                    id , parent_id , user_id , is_disabled , org_type , 
                    login , pwd , full_name , short_name , file_id ,
                    at_ye , at_mo , at_da , at_ho , at_mi ,  
                    pwd_ye , pwd_mo , pwd_da                     
                ) VALUES (
                    ${stm.getNextIntId("SYSTEM_users", "id")} , $id , 0 , $disabledValue , ${OrgType.ORG_TYPE_WORKER} ,
                    '${getRandomInt()}' , '${getRandomInt()}' , '$fullName' , '' , 0 ,
                    2000 , 1 , 1 , 0 , 0 ,
                    2100 , 1 , 1
                );
            """
        )
        stmUser.close()

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        cUser.refreshUserConfig(conn, userConfig.userId, hmOut)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- изменить параметры sub-юзера
        val mc = model as mCompany
        val disabledValue = if ((hmColumnData[mc.columnDisabled] as DataBoolean).value) {
            1
        } else {
            0
        }
        val fullName = (hmColumnData[mc.columnRecordFullName] as DataString).text

        val stmUser = conn.createStatement()
        stmUser.executeUpdate(
            """
                UPDATE SYSTEM_users SET
                    is_disabled = $disabledValue ,
                    full_name = '$fullName'                  
                WHERE parent_id = $id
                AND org_type = ${OrgType.ORG_TYPE_WORKER}  
            """
        )
        stmUser.close()

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name
        cUser.refreshUserConfig(conn, userConfig.userId, hmOut)

        return postURL
    }

    override fun isExistDepencies(id: Int): Boolean {
        var result = super.isExistDepencies(id)

        //--- check for pseudo-sub-user's objects
        if (!result) {
            val checkStm = conn.createStatement()
            val rs = checkStm.executeQuery(
                """
                    SELECT id FROM TS_object WHERE user_id IN (
                        SELECT id FROM SYSTEM_users 
                        WHERE parent_id = $id
                        AND org_type = ${OrgType.ORG_TYPE_WORKER}
                    )
                """
            )
            result = rs.next()
            rs.close()
            checkStm.close()
        }

        //--- check real sub-user's existing
        if (!result) {
            val checkStm = conn.createStatement()
            val rs = checkStm.executeQuery(
                """
                    SELECT id FROM SYSTEM_users 
                    WHERE parent_id = $id
                    AND org_type = ${OrgType.ORG_TYPE_BOSS}  
                """
            )
            result = rs.next()
            rs.close()
            checkStm.close()
        }

        return result
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)

        //--- delete pseudo sub-user
        val delStm = conn.createStatement()
        delStm.executeUpdate(
            """
                DELETE FROM SYSTEM_users 
                WHERE parent_id = $id
                AND org_type = ${OrgType.ORG_TYPE_WORKER}  
            """
        )
        delStm.close()
    }
}