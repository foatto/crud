package foatto.core_server.app.server

import foatto.sql.CoreAdvancedConnection

object OtherOwnerData {

    //--- передаем именно уже существующий/открытый Connection, а Statement по нему будет открываться по необходимости
    fun getOtherOwner(
        conn: CoreAdvancedConnection,
        aliasID: Int,
        rowID: Int,
        rowUserID: Int,
        otherUserID: Int
    ): Int {
        val hmOtherOwnerData = reloadOtherOwnerData(conn)

        //--- данных может и не быть
        val hmRowData = hmOtherOwnerData[aliasID] ?: return rowUserID
        val hsUser = hmRowData[rowID] ?: return rowUserID

        return if (hsUser.contains(otherUserID)) otherUserID else rowUserID
    }

    fun checkOtherOwner(
        conn: CoreAdvancedConnection,
        aliasID: Int,
        otherUserID: Int
    ): Boolean {
        val hmOtherOwnerData = reloadOtherOwnerData(conn)

        val hmRowData = hmOtherOwnerData[aliasID] ?: return false

        for (hsUser in hmRowData.values) {
            if (hsUser.contains(otherUserID)) {
                return true
            }
        }

        return false
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun reloadOtherOwnerData(conn: CoreAdvancedConnection): Map<Int, Map<Int, Set<Int>>> {
        val hmOtherOwnerData = mutableMapOf<Int, MutableMap<Int, MutableSet<Int>>>()

        val rs = conn.executeQuery(" SELECT alias_id , row_id , user_id FROM SYSTEM_other_owner ")
        while (rs.next()) {
            val aliasID = rs.getInt(1)
            val rowID = rs.getInt(2)
            val userID = rs.getInt(3)

            val hmRowData = hmOtherOwnerData.getOrPut(aliasID) { mutableMapOf() }
            val hsUser = hmRowData.getOrPut(rowID) { mutableSetOf() }

            hsUser += userID
        }
        rs.close()

        return hmOtherOwnerData
    }
}

