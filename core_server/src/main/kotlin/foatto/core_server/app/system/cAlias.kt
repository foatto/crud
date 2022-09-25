package foatto.core_server.app.system

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

        application.deletePermissions(conn, id)
    }

    private fun refreshPerm(id: Int) {
        //--- обновляем список permission и связанных с ними записей
        //--- загрузить список ролей для возможного будущего автоматического создания или удаления
        val alRoleIds = application.loadRoleIdList(conn)

        //--- загрузить существующий список прав доступа
        val hmOldPermissions = mutableMapOf<String, Int>()
        var rs = conn.executeQuery(" SELECT name, id FROM SYSTEM_permission WHERE class_id = $id ")
        while (rs.next()) {
            hmOldPermissions[rs.getString(1)] = rs.getInt(2)
        }
        rs.close()

        //--- загрузить данные по только что сохраненному классу
        val ac = application.getAliasConfig(conn, aliasId = id).values.first()

        //--- вытащить (новый) список прав доступа класса
        val page = Class.forName(ac.controlClassName).getConstructor().newInstance() as cStandart
        page.init(application, conn, chmSession, hmParam, hmAliasConfig, ac, hmXyDocumentConfig, userConfig)
        val alPermNameDescr = page.alPermission

        //--- обновить список прав доступа с проверкой зависимых таблиц
        for ((permName, permDescr) in alPermNameDescr) {
            rs = conn.executeQuery(" SELECT * FROM SYSTEM_permission WHERE class_id = $id AND name = '$permName' ")
            val isExist = rs.next()
            rs.close()
            //--- если такое право доступа существует, на всякий случай обновим ему описание
            if (isExist) {
                conn.executeUpdate(" UPDATE SYSTEM_permission SET descr = '$permDescr' WHERE class_id = $id AND name = '$permName' ")
            }
            //--- если его нет, то добавим
            else {
                val nextPermID = conn.getNextIntId("SYSTEM_permission", "id")
                conn.executeUpdate(
                    " INSERT INTO SYSTEM_permission ( id, class_id, name, descr ) VALUES ( $nextPermID , $id , '$permName' , '$permDescr' ) "
                )
                alRoleIds.forEach { roleId ->
                    val nextRolePermID = conn.getNextIntId("SYSTEM_role_permission", "id")
                    conn.executeUpdate(
                        " INSERT INTO SYSTEM_role_permission ( id, role_id, permission_id, permission_value ) VALUES ( $nextRolePermID , $roleId , $nextPermID , 0 ) "
                    )
                }
            }
            //--- удалим его из списка загруженных прав доступа
            hmOldPermissions.remove(permName)
        }
        //--- теперь удаляем лишние права доступа
        for ((_, permID) in hmOldPermissions) {
            conn.executeUpdate(" DELETE FROM SYSTEM_permission WHERE id = $permID ")
            conn.executeUpdate(" DELETE FROM SYSTEM_role_permission WHERE permission_id = $permID ")
        }
    }
}
