@file:JvmName("OtherOwnerData")
package foatto.core_server.app.server

import foatto.sql.CoreAdvancedStatement
import java.util.HashMap
import java.util.HashSet

object OtherOwnerData {

    //--- передаем именно уже существующий/открытый Connection, а Statement по нему будет открываться по необходимости
    @JvmStatic
    fun getOtherOwner(stm: CoreAdvancedStatement, aliasID: Int, rowID: Int, rowUserID: Int, otherUserID: Int ): Int {
        val hmOtherOwnerData = reloadOtherOwnerData( stm )

        //--- данных может и не быть
        val hmRowData = hmOtherOwnerData[ aliasID ] ?: return rowUserID

        val hsUser = hmRowData[rowID] ?: return rowUserID

        return if( hsUser.contains( otherUserID ) ) otherUserID else rowUserID
    }

    @JvmStatic
    fun checkOtherOwner(stm: CoreAdvancedStatement, aliasID: Int, otherUserID: Int ): Boolean {
        val hmOtherOwnerData = reloadOtherOwnerData( stm )

        val hmRowData = hmOtherOwnerData[ aliasID ] ?: return false

        for( hsUser in hmRowData.values )
            if( hsUser.contains( otherUserID ) ) return true

        return false
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun reloadOtherOwnerData( stm: CoreAdvancedStatement): HashMap<Int, HashMap<Int, HashSet<Int>>> {
        val hmOtherOwnerData = HashMap<Int, HashMap<Int, HashSet<Int>>>()

        val rs = stm.executeQuery( " SELECT alias_id , row_id , user_id FROM SYSTEM_other_owner " )
        while( rs.next() ) {
            val aliasID = rs.getInt( 1 )
            val rowID = rs.getInt( 2 )
            val userID = rs.getInt( 3 )

            var hmRowData: HashMap<Int, HashSet<Int>>? = hmOtherOwnerData[ aliasID ]
            if( hmRowData == null ) {
                hmRowData = HashMap()
                hmOtherOwnerData[ aliasID ] = hmRowData
            }
            var hsUser: HashSet<Int>? = hmRowData[ rowID ]
            if( hsUser == null ) {
                hsUser = HashSet()
                hmRowData[ rowID ] = hsUser
            }
            hsUser.add( userID )
        }
        rs.close()

        return hmOtherOwnerData
    }
}

