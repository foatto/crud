package foatto.core_server.app.system

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.iData

class cAlias : cStandart() {

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)
        refreshPerm(id)
        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)
        refreshPerm(id)
        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)

        //--- удаляем зависимые cPermission & cRolePermission
        //--- сначала запомним все permissionID (нужно для удаления cRolePermission)
        var sIn = ""
        val rs = stm.executeQuery(" SELECT id FROM SYSTEM_permission WHERE class_id = $id ")
        while (rs.next()) {
            sIn += (if (sIn.isEmpty()) "" else " , ") + rs.getInt(1).toString()
        }
        rs.close()

        //--- удаляем все cPermission от класса
        stm.executeUpdate(" DELETE FROM SYSTEM_permission WHERE class_id = $id ")
        //--- удаляем все cRolePermission
        stm.executeUpdate(" DELETE FROM SYSTEM_role_permission WHERE permission_id IN ( $sIn ) ")
    }

    private fun refreshPerm(id: Int) {
        //--- обновляем список permission и связанных с ними записей
        //--- загрузить список ролей для возможного будущего автоматического создания или удаления
        val alRole = mutableListOf<Int>()
        var rs = stm.executeQuery(" SELECT id FROM SYSTEM_role WHERE id <> 0 ")
        while (rs.next()) {
            alRole.add(rs.getInt(1))
        }
        rs.close()

        //--- загрузить существующий список прав доступа
        val hmPerm = mutableMapOf<String, Int>()
        rs = stm.executeQuery(" SELECT name, id FROM SYSTEM_permission WHERE class_id = $id ")
        while (rs.next()) {
            hmPerm[rs.getString(1)] = rs.getInt(2)
        }
        rs.close()

        //--- загрузить данные по только что сохраненному классу
        val ac = AliasConfig.getConfig(stm, id)!!

        //--- вытащить (новый) список прав доступа класса
        val page = Class.forName(ac.controlClassName).getConstructor().newInstance() as cStandart
        page.init(application, conn, stm, chmSession, hmParam, hmAliasConfig, ac, hmXyDocumentConfig, userConfig)
        val alPermNameDescr = page.alPermission

        //--- обновить список прав доступа с проверкой зависимых таблиц
        for ((permName, permDescr) in alPermNameDescr) {
            rs = stm.executeQuery(" SELECT * FROM SYSTEM_permission WHERE class_id = $id AND name = '$permName' ")
            val isExist = rs.next()
            rs.close()
            //--- если такое право доступа существует, на всякий случай обновим ему описание
            if (isExist) {
                stm.executeUpdate(" UPDATE SYSTEM_permission SET descr = '$permDescr' WHERE class_id = $id AND name = '$permName' ")
            }
            //--- если его нет, то добавим
            else {
                val nextPermID = stm.getNextID("SYSTEM_permission", "id")
                stm.executeUpdate(
                    " INSERT INTO SYSTEM_permission ( id, class_id, name, descr ) VALUES ( $nextPermID , $id , '$permName' , '$permDescr' ) "
                )
                for (roleID in alRole) {
                    val nextRolePermID = stm.getNextID("SYSTEM_role_permission", "id")
                    stm.executeUpdate(
                        " INSERT INTO SYSTEM_role_permission ( id, role_id, permission_id, permission_value ) VALUES ( $nextRolePermID , $roleID , $nextPermID , 0 ) "
                    )
                }
            }
            //--- удалим его из списка загруженных прав доступа
            hmPerm.remove(permName)
        }
        //--- теперь удаляем лишние права доступа
        for ((_, permID) in hmPerm) {
            stm.executeUpdate(" DELETE FROM SYSTEM_permission WHERE id = $permID ")
            stm.executeUpdate(" DELETE FROM SYSTEM_role_permission WHERE permission_id = $permID ")
        }
    }
}
