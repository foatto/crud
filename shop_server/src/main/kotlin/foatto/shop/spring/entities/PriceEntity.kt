//package foatto.shop.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_price")
//open class PriceEntity(
//
//    @Id
//    @Column(name = "id", unique = true)
//    var id: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "catalog_id")
//    var catalog: CatalogEntity,
//
//    @Basic
//    @Column(name = "price_type")
//    var type: Int,
//
//    var date: DateTimeField,
//
//    @Basic
//    @Column(name = "price_value")
//    var value: Double,
//
//    @Basic
//    @Column(name = "price_note", length = 250)
//    var note: String
//
//)
//
