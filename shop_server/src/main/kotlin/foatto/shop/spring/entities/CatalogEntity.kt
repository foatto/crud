//package foatto.shop.entity
//
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_catalog")
//open class CatalogEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "parent_id", nullable = false)
//    var parentCatalog: CatalogEntity,
//
//    @Basic
//    @Column(name = "record_type", nullable = false)
//    var recordType: Int,
//
//    @Basic
//    @Column(name = "name", nullable = false, length = 250)
//    var fullName: String,
//
//    @Basic
//    @Column(name = "is_production", nullable = false)
//    var isProduction: Boolean,
//
//    @Basic
//    @Column(name = "profit_add", nullable = false)
//    var profitAdd: Int
//
//)
///*
//        columnCatalogPriceDate = ColumnDate( tableName, "_price_ye" , "_price_mo" , "_price_da", "Дата установки цены", 2010, 2100, timeZone )
//            columnCatalogPriceDate.isVirtual = true
//            columnCatalogPriceDate.tableCaption = "Дата устан. цены"
//            columnCatalogPriceDate.addFormVisible( FormColumnVisibleData( columnRecordType, true, intArrayOf( RECORD_TYPE_ITEM ) ) )
//            columnCatalogPriceDate.setDefaultDate( gc.get( GregorianCalendar.YEAR ), gc.get( GregorianCalendar.MONTH ) + 1, gc.get( GregorianCalendar.DAY_OF_MONTH ) )
//
//        columnCatalogPriceIn = ColumnDouble( tableName, "_price_in", "Закупочная цена", 10, 2 )
//            columnCatalogPriceIn.isVirtual = true
//            columnCatalogPriceIn.addFormVisible( FormColumnVisibleData( columnRecordType, true, intArrayOf( RECORD_TYPE_ITEM ) ) )
//            columnCatalogPriceIn.tableCaption = "Закуп. цена"
//            columnCatalogPriceIn.tableAlign = TableCellAlign.RIGHT
//
//        columnCatalogPriceOut = ColumnDouble( tableName, "_price_out", "Розничная цена", 10, 2 )
//            columnCatalogPriceOut.isVirtual = true
//            columnCatalogPriceOut.addFormVisible( FormColumnVisibleData( columnRecordType, true, intArrayOf( RECORD_TYPE_ITEM ) ) )
//            columnCatalogPriceOut.tableCaption = "Розн. цена"
//            columnCatalogPriceOut.tableAlign = TableCellAlign.RIGHT
//
//        columnCatalogRowCount = ColumnString( tableName, "_row_count", "Кол-во наименований", 10 )
//            columnCatalogRowCount.isVirtual = true
//            columnCatalogRowCount.emptyValueString = ""
//            columnCatalogRowCount.tableCaption = "Кол-во наим."
//            columnCatalogRowCount.tableAlign = TableCellAlign.RIGHT
//
//        alColumnCatalogCount = ArrayList( alWarehouseName.size )
//        for( i in 0 until alWarehouseName.size ) {
//            val cd = ColumnDouble( tableName, "_$i", alWarehouseName[ i ], 10, -1 )
//                cd.isVirtual = true
//                cd.tableAlign = TableCellAlign.CENTER
//            alColumnCatalogCount.add( cd )
//        }
//        columnCatalogAllCount = ColumnDouble( tableName, "_all_count", "ВСЕГО", 10, -1 )
//            columnCatalogAllCount.isVirtual = true
//            columnCatalogAllCount.tableAlign = TableCellAlign.CENTER
//
//        columnToArchive = ColumnBoolean( tableName, "_to_archive", "Перенести товар в архив", false )
//            columnToArchive.isVirtual = true
//
// */
