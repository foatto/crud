package foatto.core_server.app.system

class cLogUser : cLogText() {

    override fun doLogRowFilter() {
        super.doLogRowFilter()

        logCurRow?.let {
            val pUID = hmParentData["system_user"]
            pUID?.let {
                val pos = logCurRow!!.indexOf(' ')
                if (pos > 0) {
                    try {
                        //--- убёрем в показе ненужный числовой userID
                        logCurRow = if (pUID == logCurRow!!.substring(0, pos).toInt()) {
                            logCurRow!!.substring(pos)
                        } else {
                            null
                        }
                    }
                    //--- любые неполадки с форматом строки лога - возвращаем строку как есть
                    catch (t: Throwable) {
                    }
                }
            }
        }
    }

}
