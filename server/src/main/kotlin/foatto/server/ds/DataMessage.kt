package foatto.server.ds

import foatto.server.util.AdvancedByteBuffer

class DataMessage(val cmd: Int = CMD_DATA, val byteBuffer: AdvancedByteBuffer? = null) {

    companion object {
        //--- входящая команда "ошибка" - от некорректного закрывшегося (по различным причинам) канала
        const val CMD_ERROR = -1

        //--- двусторонняя команда "данные" - входящие данные на обработку или исходящие на запись с установленным byteBuffer
        const val CMD_DATA = 0

        //--- входящая команда на закрытие - от корректно закрытого клиентом канала (byteBuffer не используется)
        const val CMD_CLOSE = 1

        //--- входящая команда на предобработку (byteBuffer не используется)
        const val CMD_PRE_WORK = 2
    }
}
