package foatto.shop.report

import foatto.sql.CoreAdvancedStatement
import java.util.ArrayList

class DocumentData( val id: Int, val type: Int, val whSour: Int, val whDest: Int, val docNo: String,
                    val arrDT: IntArray, val clientID: Int, val discount: Double, val descr: String ) {

//  create_time       INT,
//  edit_time         INT,
//  content_edit_time INT,
//  is_deleted        INT, - учитывается при загрузке

    lateinit var alDCD: ArrayList<DocumentContentData>

    companion object {
        fun loadDocumentData( stm: CoreAdvancedStatement): ArrayList<DocumentData> {
            val alDD = ArrayList<DocumentData>()

            val rs = stm.executeQuery(
                " SELECT id , doc_type , sour_id , dest_id , doc_no , doc_ye , doc_mo , doc_da , client_id , discount , descr " +
                " FROM SHOP_doc WHERE is_deleted = 0 ORDER BY doc_ye , doc_mo , doc_da " )
            while( rs.next() ) {
                var p = 1
                alDD.add( DocumentData( rs.getInt( p++ ), rs.getInt( p++ ), rs.getInt( p++ ), rs.getInt( p++ ), rs.getString( p++ ),
                                        intArrayOf( rs.getInt( p++ ), rs.getInt( p++ ), rs.getInt( p++ ), 0, 0, 0 ),
                                        rs.getInt( p++ ), rs.getDouble( p++ ), rs.getString( p++ ) ) )
            }

            for( dd in alDD ) dd.alDCD = DocumentContentData.loadDocumentContentData( stm, dd.id )

            return alDD
        }
    }

}
