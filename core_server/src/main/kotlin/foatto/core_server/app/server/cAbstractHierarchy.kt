package foatto.core_server.app.server

import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.link.TablePopupData
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedStatement

open class cAbstractHierarchy : cStandart() {

    override fun isAliasesEquals(alias1: String, alias2: String): Boolean {
        val mah = model as mAbstractHierarchy

        return super.isAliasesEquals(alias1, alias2) ||
            (alias1 == mah.commonAliasName || alias1 == mah.folderAliasName || alias1 == mah.itemAliasName) &&
            (alias2 == mah.commonAliasName || alias2 == mah.folderAliasName || alias2 == mah.itemAliasName)
    }

    override fun getParentID(alias: String?): Int? {
        val pID = super.getParentID(alias)
        if(pID != null) return pID

        val mah = model as mAbstractHierarchy

        if(alias == mah.commonAliasName || alias == mah.folderAliasName || alias == mah.itemAliasName) {
            return hmParentData[mah.commonAliasName] ?: hmParentData[mah.folderAliasName] ?: hmParentData[mah.itemAliasName]
        }
        return null
    }

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        return super.addSQLWhere(hsTableRenameList) + " AND ${renameTableName(hsTableRenameList, model.tableName)}.${model.columnID!!.getFieldName()} > 0 "
    }

    override fun isOpenFormURLInNewWindow(): Boolean = false

    override fun doExpand(pid: Int) = expandCatalog(stm, model.tableName, pid, false)

    override fun getTableRowSelectButton(row: Int, col: Int, selectorParam: SelectorParameter, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): TableCell {
        val m = model as mAbstractHierarchy

        val dataRecordType = hmColumnData[m.columnRecordType] as DataComboBox

        return if(
            dataRecordType.value == mAbstractHierarchy.RECORD_TYPE_FOLDER && m.isSelectableFolder ||
            dataRecordType.value != mAbstractHierarchy.RECORD_TYPE_FOLDER && m.isSelectableItem
        ) {
            super.getTableRowSelectButton(row, col, selectorParam, hmColumnData, hmOut)
        } else {
            TableCell(row, col)
        }
    }

    override fun getTableRowGoto(selectorID: String?, hmColumnData: Map<iColumn, iData>, indexChild: Int, alPopupData: MutableList<TablePopupData>): String? {
        val m = model as mAbstractHierarchy
        val childAlias = model.alChildData[indexChild].alias as String
        if(childAlias == aliasConfig.alias) {
            val dataRecordType = hmColumnData[m.columnRecordType] as DataComboBox
            return if(dataRecordType.value == mAbstractHierarchy.RECORD_TYPE_FOLDER) {
                super.getTableRowGoto(selectorID, hmColumnData, indexChild, alPopupData)
            } else {
                null
            }
        } else return super.getTableRowGoto(selectorID, hmColumnData, indexChild, alPopupData)
    }

    //--- для наследников - можно изменить реакцию на действие по умолчанию (двойной клик по строке таблицы)
    override fun newTableRowDefaultOperation(selectorParam: SelectorParameter?, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val m = model as mAbstractHierarchy
        val dataRecordType = hmColumnData[m.columnRecordType] as DataComboBox

        //--- в режиме селектора double-click делает возврат строки аналогично нажатию соответствующей кнопки, кроме случаев:
        //--- double-click по групповой записи, который ВСЕГДА даёт вход в группу

        //--- double-click делает действие по умолчанию (обычно - вход в форму) в следующих случаях:
        //--- 1. это не селектор (т.е. не надо возвращать значений из текущей строки)
        //--- 2. это селектор, но это строка с папкой (в этом случае производится вход в папку, а не возврат значений из текущей строки)
        //--- 3. это селектор и это строка-элемент (не папка), но текущий алиас == выбор папки, поэтому не нужен значений из текущей строки
        return if(selectorParam == null || dataRecordType.value == mAbstractHierarchy.RECORD_TYPE_FOLDER || aliasConfig.alias == m.folderAliasName) {
            null
        } else {
            //--- в остальных случаях перекрываем на стандартный возврат значения из селектора
            super.newTableRowDefaultOperation(selectorParam, hmColumnData, hmOut)
        }
    }

    //--- для наследников - можно добавлять дополнительные паренты для child'ов
    override fun putTableRowGotoNewParentData(hmColumnData: Map<iColumn, iData>, indexChild: Int, hmNewParentData: MutableMap<String, Int>) {
        super.putTableRowGotoNewParentData(hmColumnData, indexChild, hmNewParentData)

        val mah = model as mAbstractHierarchy

        val id = (hmColumnData[model.alChildData[indexChild].column] as DataInt).value
        hmNewParentData[mah.commonAliasName] = id
        hmNewParentData[mah.folderAliasName] = id
        hmNewParentData[mah.itemAliasName] = id
    }

    //--- перекрыто для проверки и недопущения совпадения (с последующим зацикливанием) id == parent_id
    override fun getFormValues(id: Int, alFormData: List<FormData>, alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>): Boolean {
        val isValid = super.getFormValues(id, alFormData, alColumnList, hmColumnData)
        //--- совпасть может только при редактировании (при добавлении записи ещё нет)
        if(id != 0) {
            val mah = model as mAbstractHierarchy

            val dataParentID = hmColumnData[mah.columnParent] as DataInt
            if(dataParentID.value == id) {
                val dataParentFullName = hmColumnData[mah.columnParentFullName] as DataString
                dataParentFullName.errorValue = dataParentFullName.text
                dataParentFullName.errorText = "Значение совпадает с ${mah.columnParentFullName.caption} ! "
                return false
            }
        }
        return isValid
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- дополнительно в иерархических структурах
        if(model.columnActive != null && model.columnArchive != null) {
            setActiveAndArchive(
                action,
                id,
                model.tableName,
                model.columnID!!.getFieldName(),
                model.columnActive!!.getFieldName(),
                model.columnArchive!!.getFieldName(),
                (model as mAbstractHierarchy).columnParent.getFieldName(),
                (hmColumnData[(model as mAbstractHierarchy).columnRecordType] as DataComboBox).value == mAbstractHierarchy.RECORD_TYPE_FOLDER,
                stm
            )
        }

        return postURL
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        //--- загрузка полной структуры каталога: key = parent_id, value = list of ids
        fun getCatalogParent(stm: CoreAdvancedStatement, tableName: String): Map<Int, List<Int>> {
            val hmCatalogParent = mutableMapOf<Int, MutableList<Int>>()
            val rs = stm.executeQuery( " SELECT parent_id , id FROM $tableName WHERE id <> 0 ORDER BY parent_id , id " )
            while( rs.next() ) {
                val parentID = rs.getInt( 1 )
                val alID = hmCatalogParent.getOrPut( parentID ) { mutableListOf() }
                alID.add( rs.getInt( 2 ) )
            }
            rs.close()

            return hmCatalogParent
        }

        //--- выгрузка всех подэлементов заданного узла в линейный список
        fun expandCatalog(stm: CoreAdvancedStatement, tableName: String, pid: Int, isItemsOnly: Boolean ): Set<Int> {
            val hmCatalogParent = getCatalogParent( stm, tableName )

            //--- генерируем полный список подузлов
            val alId = mutableListOf<Int>()
            alId.add( pid )
            for( i in alId.indices ) {  // именно через i и indices, т.к. в процессе перебора идёт дозаполнение списка
                val alItem = hmCatalogParent[ alId[ i ] ]
                if( alItem != null ) alId.addAll( alItem )
            }
            //--- начальный pid убираем из списка
            alId.removeAt( 0 )

            //--- убираем папки, если не нужны
            //--- (на случай оптимизаторского зуда: фигурные скобки обязательны, чтобы внутренний if не спутался со внешним else)
            val hsResult = if( isItemsOnly ) {
                alId.filterNot { hmCatalogParent.containsKey( it ) }.toMutableSet()
            }
            else {
                alId.toMutableSet()
            }

            //--- если список пустой, то одно из двух - или pid не узел или в нём нет вложенных элементов.
            //--- тогда надо вернуть хотя бы исходный pid
            if( hsResult.isEmpty() ) hsResult.add( pid )

            return hsResult
        }

        fun setActiveAndArchive(
            action: String,
            id: Int,
            tableName: String,
            idFieldName: String,
            activeFieldName: String,
            archiveFieldName: String,
            parentFieldName: String,
            isNode: Boolean,
            stm: CoreAdvancedStatement
        ) {
            //--- устанавливаем только один соответствующий флаг в вышестоящих узлах
            //--- (т.е. вышестоящие узлы становятся "также и в архиве")
            var curId = id
            while(true) {
                val rs = stm.executeQuery(" SELECT $parentFieldName FROM $tableName WHERE $idFieldName = $curId ")
                val parentId = if(rs.next()) rs.getInt(1) else 0
                rs.close()

                if(parentId == 0) break

                stm.executeUpdate(
                    " UPDATE $tableName " +
                    " SET ${if(action == AppAction.ARCHIVE) archiveFieldName else activeFieldName} = 1 " +
                    " WHERE $idFieldName = $parentId "
                )
                curId = parentId
            }

            if(isNode) {
                //--- если элемент - узел, то устанавливаем оба флага в нижележащих узлах и элементах
                //--- (т.е. все нижележащие узлы и элементы становятся "только/полностью в архиве)
                val alId = mutableListOf<Int>()

                var alCurId = mutableListOf(id)
                while(true) {
                    val alNewId = mutableListOf<Int>()

                    alCurId.forEach {
                        val rs = stm.executeQuery(" SELECT $idFieldName FROM $tableName WHERE $parentFieldName = $it ")
                        while(rs.next()) alNewId.add(rs.getInt(1))
                        rs.close()
                    }

                    if(alNewId.isEmpty()) break

                    alId.addAll(alNewId)
                    alCurId = alNewId
                }

                alId.forEach {
                    stm.executeUpdate(
                        " UPDATE $tableName " +
                        " SET ${if(action == AppAction.ARCHIVE) archiveFieldName else activeFieldName} = 1 " +
                        "  ,  ${if(action == AppAction.ARCHIVE) activeFieldName else archiveFieldName} = 0 " +
                        " WHERE $idFieldName = $it "
                    )
                }
            }
        }

    }
}
