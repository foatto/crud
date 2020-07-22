//package foatto.shop.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_doc")
//open class DocumentEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @Basic
//    @Column(name = "create_time", nullable = false)
//    var createTime: Int,
//
//    @Basic
//    @Column(name = "edit_time", nullable = false)
//    var editTime: Int,
//
//    @Basic
//    @Column(name = "content_edit_time", nullable = false)
//    var contentEditTime: Int,
//
//    @Basic
//    @Column(name = "is_deleted", nullable = false)
//    var isDeleted: Boolean,
//
//    @Basic
//    @Column(name = "doc_type", nullable = false)
//    var type: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "sour_id", nullable = false)
//    var sourWarehouse: WarehouseEntity,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dest_id", nullable = false)
//    var destWarehouse: WarehouseEntity,
//
//    @Basic
//    @Column(name = "doc_no", nullable = false, length = 250)
//    var no: String,
//
//    @AttributeOverrides(
//        AttributeOverride(name = "ye", column = Column(name = "doc_ye")),
//        AttributeOverride(name = "mo", column = Column(name = "doc_mo")),
//        AttributeOverride(name = "da", column = Column(name = "doc_da"))
//    )
//    var date: DateTimeField,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "client_id", nullable = false)
//    var client: ClientEntity,
//
//    @Basic
//    @Column(name = "descr", nullable = false, length = 250)
//    var descr: String,
//
//    @Basic
//    @Column(name = "discount", nullable = false)
//    var discount: Double
//
//)
//
///*
//
//        columnDocumentRowCount = ColumnInt( tableName, "_row_count", "Кол-во наименований", 10 )
//            columnDocumentRowCount.isVirtual = true
//            columnDocumentRowCount.tableAlign = TableCellAlign.CENTER
//        columnDocumentCostOut = ColumnDouble( tableName, "_doc_cost_out", "Сумма [руб.]", 10, 2 )
//            columnDocumentCostOut.isVirtual = true
//            columnDocumentCostOut.tableAlign = TableCellAlign.RIGHT
//
// */
