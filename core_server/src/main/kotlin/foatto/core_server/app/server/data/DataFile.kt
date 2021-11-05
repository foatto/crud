package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

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

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String?, id: Int): Boolean {
        fileId = formData.fileId!!
        hmFileAdd = formData.hmFileAdd!!.mapKeys { it.key.toInt() }
        alFileRemovedIds = formData.alFileRemovedId!!

        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        if (isShowEmptyTableCell) {
            return TableCell(row, col, column.rowSpan, column.colSpan)
        }

        val alFileStoreData = application.getFileList(stm, fileId)

        if (alFileStoreData.isEmpty()) {
            return TableCell(row, col)
        }

        val tc = TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = 0,
            aTooltip = column.caption
        )

        for (fsd in alFileStoreData) {
            tc.addCellData(
                aText = fsd.second.substringAfterLast('/'),
                aUrl = fsd.second,
                aInNewWindow = true
            )
        }

        return tc
    }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val fci = FormCell(FormCellType.FILE)
        fci.fileName = getFieldCellName(0)
        fci.fileID = fileId

        val alFileStoreData = application.getFileList(stm, fileId)
        fci.alFile = alFileStoreData.map { fsd ->
            Triple(fsd.first, fsd.second, fsd.second.substringAfterLast('/'))
        }.toTypedArray()
        return fci
    }

    override fun getFieldSQLValue(index: Int): String = "$fileId"

    override fun preSave(rootDirName: String, stm: CoreAdvancedStatement) {
        //--- по каждому добавляемому файлу
        if (hmFileAdd.isNotEmpty()) {
            //--- при создании записи установим значение fileId
            if (fileId == 0) {
                fileId = stm.getNextIntId("SYSTEM_file_store", "file_id")
            }
            hmFileAdd.forEach { (fromClientId, fileName) ->
                application.saveFile(stm, fileId, fromClientId, fileName)
            }
        }
        //--- по каждому удаляемому файлу
        for (idForDelete in alFileRemovedIds) {
            application.deleteFile(stm, fileId, idForDelete)
        }
    }

    override fun preDelete(rootDirName: String, stm: CoreAdvancedStatement) {
        application.deleteFile(stm, fileId)
    }

    override fun setData(data: iData) {
        fileId = (data as DataFile).fileId
    }

}
