//package foatto.shop.entity
//
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_doc_content")
//open class DocumentContentEntity(
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
//    @Column(name = "is_deleted", nullable = false)
//    var isDeleted: Boolean,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "doc_id", nullable = false)
//    var doc: DocumentEntity,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "sour_id", nullable = false)
//    var sourCatalog: CatalogEntity,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "dest_id", nullable = false)
//    var destCatalog: CatalogEntity,
//
//    @Basic
//    @Column(name = "sour_num", nullable = false)
//    var sourNum: Double,
//
//    @Basic
//    @Column(name = "dest_num", nullable = false)
//    var destNum: Double
//
//)
//
///*
//        columnDestCatalogPriceOut = ColumnDouble( tableName, "_price_out_dest", "Цена", 10, 2 )
//            columnDestCatalogPriceOut.isVirtual = true
//            columnDestCatalogPriceOut.tableAlign = TableCellAlign.RIGHT
//            columnDestCatalogPriceOut.isEditable = false
//
//        columnCostOut = ColumnDouble( tableName, "_doc_cost_out", "Сумма [руб.]", 10, 2 )
//            columnCostOut.isVirtual = true
//            columnCostOut.tableAlign = TableCellAlign.RIGHT
//
//        columnToArchive = ColumnBoolean( tableName, "_to_archive", "Перенести исх. товар в архив", false )
//            columnToArchive.isVirtual = true
////            columnToArchive.setSavedDefault( userConfig )
//
//        columnResort2Reprice = ColumnBoolean( tableName, "_resort_2_reprice", "Пересортицу в переоценку", false )
//            columnResort2Reprice.isVirtual = true
// */
//
//
