//package foatto.shop.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_cash")
//open class CashEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "warehouse_id", nullable = false)
//    var warehouse: WarehouseEntity,
//
//    var date: DateTimeField,
//
//    @Basic
//    @Column(name = "cash_put", nullable = false)
//    var cashPut: Double,
//
//    @Basic
//    @Column(name = "cash_used", nullable = false)
//    var cashUsed: Double,
//
//    @Basic
//    @Column(name = "debt_out", nullable = false)
//    var debtOut: Double,
//
//    @Basic
//    @Column(name = "debt_in", nullable = false)
//    var debtIn: Double,
//
//    @Basic
//    @Column(name = "cash_rest", nullable = false)
//    var cashRest: Double,
//
//    @Basic
//    @Column(name = "descr", nullable = false, length = -1)
//    var descr: String
//
//)
