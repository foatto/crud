package foatto.shop.spring.entities

import javax.persistence.*

@Entity
@Table(name = "SHOP_doc_content")
class DocumentContentEntity(

    @Id
    val id: Int,

    @Column(name = "create_time")
    val createTime: Int,

    @Column(name = "edit_time")
    val editTime: Int,

    @Column(name = "is_deleted")
    val isDeleted: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doc_id")
    val document: DocumentEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sour_id")
    val sourCatalog: CatalogEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dest_id")
    val destCatalog: CatalogEntity,

    @Column(name = "sour_num")
    val sourNum: Double,

    @Column(name = "dest_num")
    val destNum: Double,

    @Column(name = "mark_code")
    val markCode: String,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentContentEntity) return false

        if (document != other.document) return false
        if (sourCatalog != other.sourCatalog) return false
        if (destCatalog != other.destCatalog) return false
        if (sourNum != other.sourNum) return false
        if (destNum != other.destNum) return false
        if (markCode != other.markCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.hashCode()
        result = 31 * result + sourCatalog.hashCode()
        result = 31 * result + destCatalog.hashCode()
        result = 31 * result + sourNum.hashCode()
        result = 31 * result + destNum.hashCode()
        result = 31 * result + markCode.hashCode()
        return result
    }
}


