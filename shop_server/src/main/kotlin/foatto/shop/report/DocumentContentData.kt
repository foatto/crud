package foatto.shop.report

import foatto.sql.CoreAdvancedConnection

class DocumentContentData(val id: Int, val sourID: Int, val destID: Int, val sourNum: Double, val destNum: Double) {
//  create_time       INT,
//  edit_time         INT,
//  doc_id            INT,            -- учитывается при загрузке

    companion object {
        fun loadDocumentContentData(conn: CoreAdvancedConnection, docID: Int): ArrayList<DocumentContentData> {
            val alDCD = ArrayList<DocumentContentData>()

            val rs = conn.executeQuery(" SELECT id , sour_id , dest_id , sour_num , dest_num FROM SHOP_doc_content WHERE doc_id = $docID AND is_deleted = 0 ORDER BY id ")
            while (rs.next()) {
                var p = 1
                alDCD.add(DocumentContentData(rs.getInt(p++), rs.getInt(p++), rs.getInt(p++), rs.getDouble(p++), rs.getDouble(p++)))
            }
            return alDCD
        }
    }
}
