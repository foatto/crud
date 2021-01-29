package foatto.core_server.app.system

import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.iData

class cRole : cStandart() {

    override fun postAdd( id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any> ): String? {
        val postURL = super.postAdd( id, hmColumnData, hmOut )

        //--- загрузить список прав доступа для возможного будущего автоматического создания или удаления
        val alPerm = mutableListOf<Int>()
        val rs = stm.executeQuery( " SELECT id FROM SYSTEM_permission " )
        while( rs.next() ) alPerm.add( rs.getInt( 1 ) )
        rs.close()

        for( permID in alPerm ) {
            val nextRolePermID = stm.getNextID( "SYSTEM_role_permission", "id" )
            stm.executeUpdate( " INSERT INTO SYSTEM_role_permission ( id, role_id, permission_id, permission_value ) VALUES ( $nextRolePermID , $id , $permID , 0 ) " )
        }
        return postURL
    }
}
