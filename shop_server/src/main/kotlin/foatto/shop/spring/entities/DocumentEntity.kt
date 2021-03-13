package foatto.shop.spring.entities

import foatto.spring.entities.DateEntity
import foatto.spring.entities.DateTimeEntity
import javax.persistence.*

@Entity
@Table(name = "SHOP_doc")
class DocumentEntity(

    @Id
    val id: Int,

    @Column(name = "create_time")
    val createTime: Int,

    @Column(name = "edit_time")
    val editTime: Int,

    @Column(name = "content_edit_time")
    val contentEditTime: Int,

    @Column(name = "is_deleted")
    val isDeleted: Int,

    @Column(name = "doc_type")
    val type: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sour_id")
    val sourWarehouse: WarehouseEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dest_id")
    val destWarehouse: WarehouseEntity,

    @Column(name = "doc_no")
    val no: String,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "doc_ye")),
        AttributeOverride(name = "mo", column = Column(name = "doc_mo")),
        AttributeOverride(name = "da", column = Column(name = "doc_da")),
    )
    val date: DateEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    val client: ClientEntity,

    val descr: String,

    val discount: Double,

    @Column(name = "is_fiscaled")
    val isFiscaled: Int?,

    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentEntity) return false

        if (isDeleted != other.isDeleted) return false
        if (type != other.type) return false
        if (sourWarehouse != other.sourWarehouse) return false
        if (destWarehouse != other.destWarehouse) return false
        if (no != other.no) return false
        if (date != other.date) return false
        if (client != other.client) return false
        if (descr != other.descr) return false
        if (discount != other.discount) return false
        if (isFiscaled != other.isFiscaled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isDeleted
        result = 31 * result + type
        result = 31 * result + sourWarehouse.hashCode()
        result = 31 * result + destWarehouse.hashCode()
        result = 31 * result + no.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + client.hashCode()
        result = 31 * result + descr.hashCode()
        result = 31 * result + discount.hashCode()
        result = 31 * result + (isFiscaled ?: 0)
        return result
    }

}
