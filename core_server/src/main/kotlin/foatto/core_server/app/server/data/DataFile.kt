package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getFreeDir
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import java.io.File

private class FileStoreData(val id: Int, val name: String, val dir: String)

class DataFile(
    val application: iApplication,
    aColumn: iColumn
) : DataAbstract(aColumn) {

    private val FILE_BASE = "files"

    private var fileID = 0
//        private set

    private val hmFileAdd = mutableMapOf<Int, String>()
    private val alFileRemovedID = mutableListOf<Int>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        fileID = rs.getInt(posRS++)
        return posRS
    }

    override fun loadFromDefault() {}

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String?, id: Int): Boolean {
        //--- на всякий случай, не тестировал
        hmFileAdd.clear()
        alFileRemovedID.clear()

        fileID = formData.fileID!!
        hmFileAdd.putAll(formData.hmFileAdd!!.mapKeys { it.key.toInt() })
        alFileRemovedID.addAll(formData.alFileRemovedID!!)

        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        if (isShowEmptyTableCell) {
            return TableCell(row, col, column.rowSpan, column.colSpan)
        }

        val alFileStoreData = getList(stm, fileID)

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
            val url = "/$FILE_BASE/${fsd.dir}/${fsd.name}"
            tc.addCellData(
                aText = url.substringAfterLast('/'),
                aUrl = url,
                aInNewWindow = true
            )
        }

        return tc
    }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val fci = FormCell(FormCellType.FILE)
        fci.fileName = getFieldCellName(0)
        fci.fileID = fileID

        val alFileStoreData = getList(stm, fileID)
        fci.alFile = alFileStoreData.map { fsd ->
            val url = "/$FILE_BASE/${fsd.dir}/${fsd.name}"
            Triple(fsd.id, url, url.substringAfterLast('/'))
        }.toTypedArray()
        return fci
    }

    override fun getFieldSQLValue(index: Int): String = "$fileID"

    override fun preSave(rootDirName: String, stm: CoreAdvancedStatement) {
        //--- по каждому добавляемому файлу
        if (hmFileAdd.isNotEmpty()) {
            //--- при создании записи установим значение fileID - только при реальной необходимости
            if (fileID == 0) {
                fileID = stm.getNextID("SYSTEM_file_store", "file_id")
            }
            hmFileAdd.forEach { (id, fileName) ->
                save(stm, rootDirName, fileID, id, fileName)
            }
        }
        //--- по каждому удаляемому файлу
        for (deleteID in alFileRemovedID) {
            delete(stm, rootDirName, fileID, deleteID)
        }
    }

    override fun preDelete(rootDirName: String, stm: CoreAdvancedStatement) {
        delete(stm, rootDirName, fileID, 0)
    }

    override fun setData(data: iData) {
        fileID = (data as DataFile).fileID
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    override fun hashCode() = fileID
//
//    override fun equals(other: Any?): Boolean {
//        if(super.equals(other)) return true  // if( this == obj ) return true;
//        if(other == null) return false
//        if(other !is DataFile) return false
//        return fileID == other.fileID
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getList(stm: CoreAdvancedStatement, aFileID: Int): List<FileStoreData> {
        val alFileStoreData = mutableListOf<FileStoreData>()

        val rs = stm.executeQuery(" SELECT id , name , dir FROM SYSTEM_file_store WHERE file_id = $aFileID ORDER BY name ")
        while (rs.next()) {
            alFileStoreData.add(FileStoreData(rs.getInt(1), rs.getString(2), rs.getString(3)))
        }
        rs.close()

        return alFileStoreData
    }

    private fun save(stm: CoreAdvancedStatement, rootDirName: String, aFileID: Int, id: Int, fileName: String) {
        //--- найти для него новое местоположение
        val newDirName = getFreeDir("$rootDirName/$FILE_BASE", arrayOf(""), fileName)
        val newFile = File("$rootDirName/$FILE_BASE/$newDirName/$fileName")
        //--- перенести файл в отведённое место
        File(application.tempDirName, id.toString()).renameTo(newFile)
        //--- сохранить запись о файле
        stm.executeUpdate(
            " INSERT INTO SYSTEM_file_store ( id , file_id , name , dir ) VALUES ( ${stm.getNextID("SYSTEM_file_store", "id")} , " + "$aFileID , '$fileName' , '$newDirName' ) "
        )
    }

    private fun delete(stm: CoreAdvancedStatement, rootDirName: String, aFileID: Int, aID: Int) {
        val sbSQLDiff = if (aID == 0) {
            ""
        } else {
            " AND id = $aID "
        }
        val sbSQL = " SELECT name , dir FROM SYSTEM_file_store WHERE file_id = $aFileID $sbSQLDiff "

        val rs = stm.executeQuery(sbSQL)
        while (rs.next()) {
            val recName = rs.getString(1)
            val recDir = rs.getString(2)
            deleteFile(rootDirName, recDir, recName)
        }
        rs.close()
        stm.executeUpdate(" DELETE FROM SYSTEM_file_store WHERE file_id = $aFileID $sbSQLDiff ")
    }

    private fun deleteFile(rootDirName: String, dirName: String, fileName: String) {
        val delFile = File(rootDirName, "$FILE_BASE/$dirName/$fileName")
        if (delFile.exists()) {
            delFile.delete()
        }
    }
}
