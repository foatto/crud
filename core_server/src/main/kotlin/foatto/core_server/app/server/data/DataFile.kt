package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.link.TableCellType
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet

class DataFile(
    val application: iApplication,
    aColumn: iColumn
) : DataAbstract(aColumn) {

    private var fileId = 0

    private lateinit var hmFileAdd: Map<Int, String>
    private lateinit var alFileRemovedIds: List<Int>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        fileId = rs.getInt(posRS++)
        return posRS
    }

    override fun loadFromDefault() {}

    override fun loadFromForm(conn: CoreAdvancedConnection, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        fileId = formData.fileId!!
        hmFileAdd = formData.hmFileAdd!!.mapKeys { it.key.toInt() }
        alFileRemovedIds = formData.alFileRemovedId!!

        return true
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, dataRowNo: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        if (isShowEmptyTableCell) {
            return TableCell(row, col, column.rowSpan, column.colSpan, dataRowNo)
        }

        val alFileStoreData = application.getFileList(conn, fileId)

        if (alFileStoreData.isEmpty()) {
            return TableCell(row, col)
        }

        val tc = TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aDataRow = dataRowNo,

            aAlign = column.tableAlign,
            aMinWidth = 0,
            aTooltip = column.caption,
            aCellType = TableCellType.BUTTON,
        )

        for (fsd in alFileStoreData) {
            tc.addButtonCellData(
                aText = fsd.second.substringAfterLast('/'),
                aUrl = fsd.second,
                aInNewWindow = true
            )
        }

        return tc
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val fci = FormCell(FormCellType.FILE)
        fci.fileName = getFieldCellName(0)
        fci.fileID = fileId

        val alFileStoreData = application.getFileList(conn, fileId)
        fci.alFile = alFileStoreData.map { (first, second) ->
            Triple(first, second, second.substringAfterLast('/'))
        }
        return fci
    }

    override fun getFieldSQLValue(index: Int): String = "$fileId"

    override fun preSave(rootDirName: String, conn: CoreAdvancedConnection) {
        //--- по каждому добавляемому файлу
        if (hmFileAdd.isNotEmpty()) {
            //--- при создании записи установим значение fileId
            if (fileId == 0) {
                fileId = conn.getNextIntId("SYSTEM_file_store", "file_id")
            }
            hmFileAdd.forEach { (fromClientId, fileName) ->
                application.saveFile(conn, fileId, fromClientId, fileName)
            }
        }
        //--- по каждому удаляемому файлу
        for (idForDelete in alFileRemovedIds) {
            application.deleteFile(conn, fileId, idForDelete)
        }
    }

    override fun preDelete(rootDirName: String, conn: CoreAdvancedConnection) {
        application.deleteFile(conn, fileId)
    }

    override fun setData(data: iData) {
        fileId = (data as DataFile).fileId
    }

}
