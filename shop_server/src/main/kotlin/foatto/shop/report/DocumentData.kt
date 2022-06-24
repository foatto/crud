package foatto.shop.report

import foatto.sql.CoreAdvancedStatement

class DocumentData(
    val id: Int,
    val type: Int,
    val whSour: Int,
    val whDest: Int,
    val docNo: String,
    val arrDT: Array<Int>,
    val clientID: Int,
    val discount: Double,
    val descr: String,
) {

//  create_time       INT,
//  edit_time         INT,
//  content_edit_time INT,
//  is_deleted        INT, - учитывается при загрузке

    lateinit var alDCD: MutableList<DocumentContentData>

    companion object {
        fun loadDocumentData(stm: CoreAdvancedStatement): List<DocumentData> {
            val alDD = mutableListOf<DocumentData>()

            val rs = stm.executeQuery(
                """
                    SELECT id , doc_type , sour_id , dest_id , doc_no , doc_ye , doc_mo , doc_da , client_id , discount , descr 
                    FROM SHOP_doc 
                    WHERE is_deleted = 0 
                    ORDER BY doc_ye , doc_mo , doc_da 
                """
            )
            while (rs.next()) {
                var p = 1
                alDD.add(
                    DocumentData(
                        id = rs.getInt(p++),
                        type = rs.getInt(p++),
                        whSour = rs.getInt(p++),
                        whDest = rs.getInt(p++),
                        docNo = rs.getString(p++),
                        arrDT = arrayOf(rs.getInt(p++), rs.getInt(p++), rs.getInt(p++), 0, 0, 0),
                        clientID = rs.getInt(p++),
                        discount = rs.getDouble(p++),
                        descr = rs.getString(p++)
                    )
                )
            }

            for (dd in alDD) {
                dd.alDCD = DocumentContentData.loadDocumentContentData(stm, dd.id)
            }

            return alDD
        }
    }

}
