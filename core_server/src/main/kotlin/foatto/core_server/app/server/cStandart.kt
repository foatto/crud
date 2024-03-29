package foatto.core_server.app.server

import foatto.core.app.*
import foatto.core.link.*
import foatto.core.util.BusinessException
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.*
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

open class cStandart {

    companion object {

        const val TABLE_CELL_FORE_COLOR_DISABLED = 0xFF_D0_D0_D0.toInt()
        const val TABLE_CELL_FORE_COLOR_CRITICAL = 0xFF_80_00_00.toInt()
        const val TABLE_CELL_FORE_COLOR_WARNING = 0xFF_80_80_00.toInt()
        const val TABLE_CELL_FORE_COLOR_NORMAL = 0xFF_00_80_00.toInt()

        const val TABLE_CELL_BACK_COLOR_DISABLED = 0xFF_EE_EE_EE.toInt()
        const val TABLE_CELL_BACK_COLOR_CRITICAL = 0xFF_FF_EE_EE.toInt()
        const val TABLE_CELL_BACK_COLOR_WARNING = 0xFF_FF_FF_EE.toInt()
        const val TABLE_CELL_BACK_COLOR_NORMAL = 0xFF_EE_FF_EE.toInt()

        const val PERM_ACCESS = "access"
        const val PERM_TABLE = "table"
        const val PERM_FORM = "form"
        const val PERM_ADD = "add"
        const val PERM_EDIT = "edit"
        const val PERM_DELETE = "delete"
        const val PERM_ARCHIVE = "archive"
        const val PERM_UNARCHIVE = "unarchive"

        val SELF_LINK_TABLE_PREFIX = "SELF_LINK_"

        protected val arrPageArrowLeft = arrayOf("<", "<<", "<<<")
        protected val arrPageArrowRight = arrayOf(">", ">>", ">>>")

        private val locale = Locale("ru")

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        fun checkPerm(aUserConfig: UserConfig, aHsPermission: Set<String>, permName: String, recordUserId: Int): Boolean {
            for ((relName, _) in UserRelation.arrNameDescr) {
                if (aUserConfig.getUserIDList(relName).contains(recordUserId)) {
                    return aHsPermission.contains("${permName}_$relName")
                }
            }
            //--- если userID таки не найден во всех списках пользователей
            //--- (хотя такого быть не должно, но может возникнуть при удалении пользователя, на которого еще есть записи,
            //--- да и для повышения общей ошибкоустойчивости допустим),
            //--- то относимся к нему как ко всем остальным
            return aHsPermission.contains("${permName}_${UserRelation.OTHER}")
        }

        fun getParamURL(
            aAlias: String,
            aAction: String,
            aRefererId: String?,
            aId: Int?,
            aParentData: Map<String, Int>?,
            aParentUserId: Int?,
            aAltParams: String?,
        ): String {

            var sResult = "${AppParameter.ALIAS}=$aAlias&${AppParameter.ACTION}=$aAction"
            aRefererId?.let {
                sResult += "&${AppParameter.REFERER}=$aRefererId"
            }
            aId?.let {
                sResult += "&${AppParameter.ID}=$aId"
            }

            if (!aParentData.isNullOrEmpty()) {
                var sAlias = ""
                var sID = ""
                for ((a, id) in aParentData) {
                    sAlias += "${if (sAlias.isEmpty()) "" else ","}$a"
                    sID += "${if (sID.isEmpty()) "" else ","}$id"
                }
                sResult += "&${AppParameter.PARENT_ALIAS}=$sAlias&${AppParameter.PARENT_ID}=$sID"
            }
            aParentUserId?.let {
                sResult += "&${AppParameter.PARENT_USER_ID}=$aParentUserId"
            }
            aAltParams?.let {
                sResult += aAltParams
            }

            return sResult
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- common part ---

    protected lateinit var application: iApplication
    protected lateinit var conn: CoreAdvancedConnection
    protected lateinit var chmSession: ConcurrentHashMap<String, Any>
    protected lateinit var hmParam: Map<String, String>
    protected lateinit var hmAliasConfig: Map<String, AliasConfig> // конфигурационные данные по всем алиасам
    protected lateinit var aliasConfig: AliasConfig // клонируется от переданного (на случай временного внутреннего изменения свойств)
    protected lateinit var hmXyDocumentConfig: Map<String, XyDocumentConfig>
    protected lateinit var userConfig: UserConfig // клонируется от переданного (на случай временного внутреннего изменения свойств)

    protected lateinit var model: mAbstract

    protected lateinit var zoneId: ZoneId

    //--- permission definition part ---

    val alPermission = mutableListOf<Pair<String, String>>()

    //--- private part ---

    //--- данные по parent-параметрам
    protected var hmParentData = mutableMapOf<String, Int>()  // key = parent alias, value = parent id
    protected var parentUserId: Int = 0

    protected lateinit var hsPermission: Set<String>

    protected var optimizedTableUserID = 0

    //--- временные возможно переименованные имена полей, используемые для создания запроса с самосвязанными таблицами
    protected lateinit var alRenamedSelectedFields: MutableList<String>

    //--- внутренний список для сохранения старых значений группировочных полей между вызовами getTableRowOld
    private var arrCurGroupValue: Array<String?>? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun init(
        aApplication: iApplication,
        aConn: CoreAdvancedConnection,
        aChmSession: ConcurrentHashMap<String, Any>,
        aHmParam: Map<String, String>,
        aHmAliasConfig: Map<String, AliasConfig>,
        aAliasConfig: AliasConfig,
        aHmXyDocumentConfig: Map<String, XyDocumentConfig>,
        aUserConfig: UserConfig
    ) {

        application = aApplication
        conn = aConn
        chmSession = aChmSession
        hmParam = aHmParam

        //--- получить конфигурацию всех алиасов
        hmAliasConfig = aHmAliasConfig
        //--- получить конфигурацию класса
        aliasConfig = aAliasConfig
        //--- получить xy-конфигурацию
        hmXyDocumentConfig = aHmXyDocumentConfig
        //--- получить конфигурацию пользователя
        userConfig = aUserConfig

        zoneId = userConfig.upZoneId

        //--- получение/инициализация parent-данных
        initParent()

        //--- получить данные по правам доступа (в случае, если никаких прав доступа данного пользователя к данному алиасу не прописано,
        //--- то список прав доступа == null, что чревато ошибками, поэтому установим пустой список прав доступа)
        hsPermission = userConfig.userPermission[aliasConfig.name] ?: emptySet()
        //--- инициализация модели
        initModel()
        //--- определение набора прав доступа ( зависит от модели, поэтому после нее )
        definePermission()
    }

    protected fun initParent() {
        //--- пред-обработка parent-параметра ( см. также postInit )
        try {
            val stParentAlias = StringTokenizer(hmParam[AppParameter.PARENT_ALIAS], ",")
            val stParentID = StringTokenizer(hmParam[AppParameter.PARENT_ID], ",")
            while (stParentAlias.hasMoreTokens()) {
                hmParentData[stParentAlias.nextToken()] = stParentID.nextToken().toInt()
            }
        } catch (t: Throwable) {
        }

        //--- если никакие паренты не заданы и если при отсутствии прочих парентов должен быть парент-пользователь,
        //--- то считаем, что просто работаем со своими записями
        if (hmParentData.isEmpty() && aliasConfig.isDefaultParentUser) {
            hmParentData["system_user"] = userConfig.userId
        }

        //--- попытка загрузки parentUserID
        parentUserId = hmParam[AppParameter.PARENT_USER_ID]?.toIntOrNull() ?: 0
    }

    protected fun initModel() {
        model = Class.forName(aliasConfig.modelClassName).getConstructor().newInstance() as mAbstract
        //--- если для иерархической таблицы парент от себя не задан, то самостоятельно задаем 0-й уровень структуры иерархической таблицы
        if (model.isExpandable() && getParentId(aliasConfig.name) == null) {
            hmParentData[aliasConfig.name] = 0
        }
        model.init(application, conn, aliasConfig, userConfig, hmParam, hmParentData, getIdFromParam())
    }

//--- permission part -----------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun definePermission() {
        //--- добавление базовых прав доступа
        alPermission.add(Pair(PERM_ACCESS, "01 Access"))

        if (!model.isUseParentUserId() && model.columnUser == null) {
            alPermission.add(Pair(PERM_TABLE, "02 Table"))
        } else {
            addPermDef(PERM_TABLE, "02", "Table")
        }

        if (!model.isUseParentUserId() && model.columnUser == null) {
            alPermission.add(Pair(PERM_FORM, "03 Form"))
        } else {
            addPermDef(PERM_FORM, "03", "Form")
        }

        if (model.isUseParentUserId()) {
            addPermDef(PERM_ADD, "04", "Add")
        } else {
            alPermission.add(Pair(PERM_ADD, "04 Add"))
        }

        if (!model.isUseParentUserId() && model.columnUser == null) {
            alPermission.add(Pair(PERM_EDIT, "05 Edit"))
        } else {
            addPermDef(PERM_EDIT, "05", "Edit")
        }

        if (!model.isUseParentUserId() && model.columnUser == null) {
            alPermission.add(Pair(PERM_DELETE, "06 Delete"))
        } else {
            addPermDef(PERM_DELETE, "06", "Delete")
        }

        if (model.columnActive != null && model.columnArchive != null) {
            if (!model.isUseParentUserId() && model.columnUser == null) {
                alPermission.add(Pair(PERM_ARCHIVE, "07 Archive"))
            } else {
                addPermDef(PERM_ARCHIVE, "07", "Archive")
            }

            if (!model.isUseParentUserId() && model.columnUser == null) {
                alPermission.add(Pair(PERM_UNARCHIVE, "08 Unarchive"))
            } else {
                addPermDef(PERM_UNARCHIVE, "08", "Unarchive")
            }
        }
    }

    protected fun addPermDef(permName: String, permDescrNum: String, permDescr: String) {
        for ((i, nd) in UserRelation.arrNameDescr.withIndex()) {
            alPermission.add(Pair("${permName}_${nd.first}", "$permDescrNum.$i $permDescr: ${nd.second}"))
        }
    }

    protected open fun isAddEnabled(): Boolean = if (model.isUseParentUserId()) {
        checkPerm(PERM_ADD, parentUserId)
    } else {
        hsPermission.contains(PERM_ADD)
    }

    protected open fun isEditEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean =
        if (id == 0) {
            isAddEnabled()
        } else {
            checkPerm(PERM_EDIT, getRecordUserId(hmColumnData))
        }

    protected open fun isArchiveEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean =
        id != 0 && !model.isArchiveAlias && checkPerm(PERM_ARCHIVE, getRecordUserId(hmColumnData))

    protected open fun isUnarchiveEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean =
        id != 0 && model.isArchiveAlias && checkPerm(PERM_UNARCHIVE, getRecordUserId(hmColumnData))

    protected open fun isDeleteEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean =
        id != 0 && checkPerm(PERM_DELETE, getRecordUserId(hmColumnData)) && !isExistDepencies(id)

    protected fun checkPerm(permName: String, recordUserID: Int): Boolean =
        if (!model.isUseParentUserId() && model.columnUser == null) {
            hsPermission.contains(permName)
        } else {
            checkPerm(userConfig, hsPermission, permName, if (model.isUseParentUserId()) parentUserId else recordUserID)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- в некоторых ситуациях (см. класс cUser и cUser.getParentID)
    //--- различные алиасы одного класса равны между собой в смысле parent-отношений
    protected open fun isAliasesEquals(alias1: String, alias2: String): Boolean = alias1 == alias2

    protected fun getIdFromParam(): Int? = hmParam[AppParameter.ID]?.toIntOrNull()

    protected open fun getParentId(alias: String?): Int? = hmParentData[alias]

    protected fun renameTableName(hsTableRenameList: Set<String>, tableName: String): String {
        return "${if (hsTableRenameList.contains(tableName)) SELF_LINK_TABLE_PREFIX else ""}$tableName"
    }

    protected fun getSQLString(
        isForTable: Boolean, id: Int, alColumnList: List<iColumn>,
        alSortColumn: List<iColumn>?, alSortDirect: List<String>?, alFindWord: List<String>?
    ): String {
        val hsSelectTables = mutableSetOf<String>()
        var wherePart = ""             // список условий для связывания таблиц
        var sortPart = ""

        //--- заранее соберем список подстановки наименований самосвязаных таблиц для их временного переименования
        val hsTableRenameList = mutableSetOf<String>()
        alColumnList.forEach { column ->
            column.selfLinkTableName?.let { selfLinkTableName ->
                hsTableRenameList.add(selfLinkTableName)
            }
            column.linkColumn?.selfLinkTableName?.let { selfLinkTableName ->
                hsTableRenameList.add(selfLinkTableName)
            }
        }
        val idFieldName = " ${renameTableName(hsTableRenameList, model.modelTableName)}.${model.columnId.getFieldName()} "

        alRenamedSelectedFields = mutableListOf()
        hsSelectTables.add(
            StringBuilder(model.modelTableName).append(
                if (hsTableRenameList.contains(model.modelTableName)) {
                    StringBuilder().append(' ').append(renameTableName(hsTableRenameList, model.modelTableName))
                } else {
                    ""
                }
            ).toString()
        )

        if (isForTable) {
            //--- пропускаем служебные записи ( если id-поле вообще задано )
            wherePart += " $idFieldName <> 0 "

            if (optimizedTableUserID != 0) {
                val userIDFieldName = "${renameTableName(hsTableRenameList, model.columnUser!!.columnTableName)}.${model.columnUser!!.getFieldName()}"
                wherePart += (if (wherePart.isEmpty()) "" else " AND ") + " $userIDFieldName = $optimizedTableUserID "
            }

            //--- игнорируем self-parent в иерархических таблицах, если заданы опция глубокого поиска и поисковые слова
            val isIgnoreSelfParent =
                try { //--- пока сделаем весь поиск в иерархических таблицах глубоким
                    //Boolean.parseBoolean(  userConfig.getUserProperty(  iAppContainer.DEEP_SEARCH  )  ) &&
                    alFindWord != null && alFindWord.size > 1
                } catch (t: Throwable) {
                    false
                }

            //--- добавить ограничения по parent-данным
            for (pa in hmParentData.keys) {
                if (isAliasesEquals(aliasConfig.name, pa) && isIgnoreSelfParent) {
                    continue
                }
                val pid = getParentId(pa)!!
                //--- если parent является иерархической структурой, то вполне вероятно,
                //--- что данный pid узла развернется в список вложенных pid'ов
                val hsID = expandParent(pa, pid)
                val pIDList = hsID.joinToString(",")
                val pc = model.hmParentColumn[pa]
                if (pc != null) {
                    wherePart += (if (wherePart.isEmpty()) "" else " AND ") + " ${renameTableName(hsTableRenameList, pc.columnTableName)}.${pc.getFieldName(0)} "
                    if (hsID.size == 1) {
                        wherePart += " = $pIDList "
                    } else {
                        wherePart += " IN ( $pIDList ) "
                    }
                }
            }
            //--- добавить ограничения по архивному полю
            if (model.columnActive != null && model.columnArchive != null) {
                wherePart +=
                    " AND ${renameTableName(hsTableRenameList, model.modelTableName)}." +
                        "${(if (model.isArchiveAlias) model.columnArchive!! else model.columnActive!!).getFieldName()} <> 0 "
            }
            //--- добавить свои ограничители
            wherePart += addSQLWhere(hsTableRenameList)
            //--- список полей для сортировки (для табличной части)
            for (i in alSortColumn!!.indices) {
                val column = alSortColumn[i]
                for (j in 0 until column.getFieldCount()) {
                    val sf = column.getSortFieldName(j)
                    //--- возможны разные порядки сортировки
                    sortPart += (if (sortPart.isEmpty()) "" else " , ") + " ${renameTableName(hsTableRenameList, column.columnTableName)}.$sf ${alSortDirect!![i]} "
                }
            }
        }
        //--- для загрузки одной записи в форму
        else {
            wherePart += " $idFieldName = $id "
        }

        //--- проход по полям
        for (column in alColumnList) {
            //--- виртуальные поля в SQL-запрос попасть не должны
            if (column.isVirtual) {
                continue
            }

            for (j in 0 until column.getFieldCount()) {
                alRenamedSelectedFields.add("${renameTableName(hsTableRenameList, column.columnTableName)}.${column.getFieldName(j)}")
            }

            val linkColumn = column.linkColumn
            if (linkColumn != null) {
                //--- добавить таблицу в селект
                val sb = StringBuilder()
                if (linkColumn.selfLinkTableName != null) {
                    sb.append(linkColumn.selfLinkTableName).append(' ')
                }
                sb.append(linkColumn.columnTableName)
                hsSelectTables.add(sb.toString())
                //--- если я все сделал правильно, лишнее переименование не должно помешать :)
                for (j in 0 until column.getFieldCount()) {
                    wherePart += (if (wherePart.isEmpty()) "" else " AND ") +
                        " ${renameTableName(hsTableRenameList, column.columnTableName)}.${column.getFieldName(j)} = " +
                        " ${renameTableName(hsTableRenameList, linkColumn.columnTableName)}.${linkColumn.getFieldName(j)} "
                }
            }
        }
        //--- ручное добавление дополнительных таблиц
        hsSelectTables.addAll(model.getAdditionalTables(hsTableRenameList))
        //--- список полей для селекта
        val selectFields = StringBuilder()
        for (i in alRenamedSelectedFields.indices) {
            selectFields.append(if (i == 0) "" else " , ").append(alRenamedSelectedFields[i])
        }
        //--- список таблиц для селекта
        val selectTables = StringBuilder()
        for (tableName in hsSelectTables) {
            selectTables.append(if (selectTables.isEmpty()) "" else " , ").append(tableName)
        }
        if (wherePart.isNotBlank()) {
            wherePart = " WHERE $wherePart "
        }
        if (sortPart.isNotBlank()) {
            sortPart = " ORDER BY $sortPart "
        }

        return " SELECT $selectFields FROM $selectTables $wherePart $sortPart "
    }

    protected fun expandParent(pAlias: String, pId: Int): Set<Int> {
        //--- pid своего алиаса не распахиваем, иначе никакого дерева не получится
        if (!isAliasesEquals(aliasConfig.name, pAlias)) {
            //--- загрузить данные по только что сохраненному классу
            //--- если реальный модуль для такого парент-алиаса не найден (ac == null),
            //--- возможно это псевдо-парент (для задания различных фильтров)
            application.getAliasConfig(conn = conn, aliasName = pAlias)[pAlias]?.let { ac ->
                val m = Class.forName(ac.modelClassName).getConstructor().newInstance() as mAbstract
                if (m.isExpandable()) {
                    //!!! можно ли как-то обойтись без создания объекта?
                    val page = Class.forName(ac.controlClassName).getConstructor().newInstance() as cStandart
                    page.init(application, conn, chmSession, emptyMap(), hmAliasConfig, ac, hmXyDocumentConfig, userConfig)
                    return page.doExpand(pId)
                }
            }
        }
        return setOf(pId)
    }

    protected open fun addSQLWhere(hsTableRenameList: Set<String>): String = ""

    protected open fun getColumnData(rs: CoreAdvancedResultSet?, isForTable: Boolean, alColumnList: List<iColumn>): MutableMap<iColumn, iData>? {
        //--- предварительная проверка на права доступа
        val permName: String = if (isForTable) {
            PERM_TABLE
        } else {
            PERM_FORM
        }
        //--- перегон объектов из Map в типизированное Data Object Storage
        val hmColumnData = mutableMapOf<iColumn, iData>()
        var posRS = 1  // нумерация полей в CoreAdvancedResultSet начинается с 1
        for (column in alColumnList) {
            val data = column.getData()
            if (column.isVirtual) {
                data.loadFromDefault()
            } else {
                posRS = data.loadFromDB(rs!!, posRS)
            }
            hmColumnData[column] = data
        }
        val recordUserID = getRecordUserId(hmColumnData)
        return if (checkPerm(permName, recordUserID)) {
            hmColumnData
        } else {
            null
        }
    }

    protected fun getSelfParentId(id: Int): Int {
        val sql = " SELECT ${model.hmParentColumn[aliasConfig.name]!!.getFieldName(0)} FROM ${model.modelTableName} WHERE ${model.columnId.getFieldName()} = $id"
        //System.out.println(  sb.toString()  );
        val rs = conn.executeQuery(sql)
        val result = if (rs.next()) {
            rs.getInt(1)
        } else {
            0
        }
        rs.close()

        return result
    }

    protected open fun isExistDepencies(id: Int): Boolean {
        //--- проверка в бизнес-таблицах
        for (dependData in model.alDependData) {
            if (dependData.type == DependData.CHECK && checkActiveOrExisting(dependData.destTableName, dependData.destFieldName, id)) {
                return true
            }
        }
        return false
    }

    //--- Отдельная проверка на наличие зависимой таблицы проводится потому, что в модулях, общих для всех проектов ( например, mUser ),
    //--- могут быть ссылки на depend-таблицы, отсутствующие в некоторых проектах.
    //--- Поэтому такие таблицы будем отлавливать заранее и при их отсутствии зависимость на них учитывать не будем.
    protected fun checkActiveOrExisting(aTableName: String, aFieldID: String, value: Int): Boolean {
        var returnValue: Boolean
        val rs: CoreAdvancedResultSet
        try {
            rs = conn.executeQuery(" SELECT id FROM $aTableName WHERE id <> 0 AND $aFieldID = $value ")
            returnValue = rs.next()
            rs.close()
        } catch (t: Throwable) {
            returnValue = false
        }
        return returnValue
    }

    private fun getRecordUserId(hmColumnData: Map<iColumn, iData>) =
        model.columnUser?.let { columnUser ->
            (hmColumnData[columnUser] as DataAbstractIntValue).intValue
        } ?: 0

    protected fun getRecordUserName(recordUserID: Int): String {
        var recordUserName: String?
        if (recordUserID == 0) {
            recordUserName = "(общее)"
        } else if (recordUserID == userConfig.userId) {
            recordUserName = ""
        } else {
            recordUserName = application.hmUserShortNames[recordUserID]
            if (recordUserName.isNullOrEmpty()) {
                recordUserName = application.hmUserFullNames[recordUserID]
            }
            if (recordUserName == null) {
                recordUserName = "(неизвестно)"
            }
        }
        return recordUserName
    }

//--- expand part ( для иерархических структур ) --------------------------------------------------------------------------------------------------------------------------------------

    protected open fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        //--- иерархические таблицы всегда имеют в заголовке описание предыдущего пути
        if (model.isExpandable()) {
            getExpandPath(selectorID, withAnchors, alPath)
        } else {
            alPath.add(Pair("", aliasConfig.descr))
        }
    }

    //--- в базовой реализации возвращает исходный pid
    open fun doExpand(pid: Int): Set<Int> = setOf(pid)

    protected fun getExpandPath(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>) {

        val pID = getParentId(aliasConfig.name)
        val originalParentID = pID ?: 0
        var curParentID = originalParentID
        while (true) {
            var (newPID, pName) = getExpandPathItem(curParentID)
            if (pName.isEmpty()) {
                pName = aliasConfig.descr
            }

            //--- пустые строки вместо null - чтобы не делать лишних проверок при writeUTF
            var tmpURL = ""
            val tmpText = if (curParentID == originalParentID) {
                pName
            } else {
                val hmP = mutableMapOf<String, Int>()
                hmP[aliasConfig.name] = curParentID
                tmpURL = getParamURL(aliasConfig.name, AppAction.TABLE, null, null, hmP, null, if (selectorID == null) "" else "&${AppParameter.SELECTOR}=$selectorID")
                pName
            }

            alPath.add(0, Pair(if (withAnchors) tmpURL else "", tmpText))
            if (curParentID != 0) {
                curParentID = newPID
            } else {
                break
            }
        }
    }

    protected fun getExpandPathItem(curParentID: Int): Pair<Int, String> {
        val sqlStr = " SELECT ${model.expandParentIDColumn!!.getFieldName()} , ${model.expandParentNameColumn!!.getFieldName()} " +
            " FROM ${model.modelTableName} WHERE ${model.columnId.getFieldName()} = $curParentID "
        val rs = conn.executeQuery(sqlStr)
        rs.next()
        val pID = rs.getInt(1)
        val pName = rs.getString(2)
        rs.close()

        return Pair(pID, pName)
    }

//--- table part ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alTableHiddenColumn)
        alColumnList.addAll(model.alTableGroupColumn)
        model.fillTableColumn(alColumnList)

        optimizedTableUserID = 0
        //--- для модулей, использующих parentUserID, оптимизация userID недопустима, т.к. userID у них не существует
        if (model.columnUser != null) {
            val hsTableUserID = mutableSetOf<Int>()
            for ((relName, _) in UserRelation.arrNameDescr) {
                if (hsPermission.contains("${PERM_TABLE}_$relName")) {
                    hsTableUserID.addAll(userConfig.getUserIDList(relName))
                }
            }
            if (hsTableUserID.size == 1) {
                optimizedTableUserID = userConfig.userId
            }
        }

        //--- готовятся отдельно, т.к. в разных ситуациях вызова getSQLString разные требования по сортировке
        val alTableSortColumn = mutableListOf<iColumn>()
        val alTableSortDirect = mutableListOf<String>()
        //--- заданная пользователем сортировка
        //--- если == 0, то не задана ( начальная ситуация ),
        //--- иначе sort == 1..кол-во табличных столбцов первой строки,
        //--- а знак величины sort обозначает порядок сортировки
        val sort = try {
            hmParam[AppParameter.SORT]!!.toInt()
        } catch (t: Throwable) {
            0
        }

        val columnSort = model.getTableColumn(0, abs(sort) - 1)
        if (sort != 0 && abs(sort) <= model.getTableColumnColCount() && columnSort != null && columnSort.isSortable()) {
            alTableSortColumn.add(columnSort)
            alTableSortDirect.add(if (sort > 0) "ASC" else "DESC")
        }
        //--- программно заданную сортировку добавляем, только если она не противоречит заданной пользователем
        model.alTableSort.forEach { tsd ->
            if (!alTableSortColumn.contains(tsd.column) && tsd.column.isSortable()) {
                alTableSortColumn.add(tsd.column)
                alTableSortDirect.add(if (tsd.direct) "ASC" else "DESC")
            }
        }
        if (model.alTableGroupColumn.size > 0) {
            arrCurGroupValue = arrayOfNulls(model.alTableGroupColumn.size)
        }

        //--- appParam придётся генерировать заново/обратно, поскольку в явном виде он отсутствует
        val urlReferer = AppParameter.collectParam(hmParam)
        val refererID = getRandomInt().toString()
        hmOut[AppParameter.REFERER + refererID] = urlReferer

        val finderID = hmParam[AppParameter.FINDER]
        val alFindWord = if (finderID == null) {
            null
        } else {
            chmSession[AppParameter.FINDER + finderID] as? List<String>?
        }

        val pageNo = if (aliasConfig.pageSize > 0) {
            try {
                hmParam[AppParameter.PAGE]!!.toInt()
            } catch (t: Throwable) {
                0
            }
        } else {
            0
        }

        //--- if this table called for select from edit form
        val selectorID = hmParam[AppParameter.SELECTOR]
        val selectorParam = chmSession[AppParameter.SELECTOR + selectorID] as? SelectorParameter
        if (selectorParam != null) {
            setSelectorParent(selectorParam)
        }

        //--- заголовок таблицы
        val alHeader = mutableListOf<Pair<String, String>>()
        fillHeader(selectorID, true, alHeader, hmOut)

        //--- заголовки столбцов
        val alCaption = mutableListOf<Pair<String, String>>()
        getTableCaption(selectorParam != null, urlReferer, sort, alCaption)

        //--- используем отдельный Statement для открытия табличного ResultSet'a,
        //--- т.к. в процессе загрузки могут открываться другие ResultSet'ы от стандартного Statement'a
        var rsTable: CoreAdvancedResultSet? = null
        if (model.modelTableName != mAbstract.FAKE_TABLE_NAME) {
            val sqlStr = getSQLString(
                isForTable = true,
                id = 0,
                alColumnList = alColumnList,
                alSortColumn = alTableSortColumn,
                alSortDirect = alTableSortDirect,
                alFindWord = alFindWord
            )
//AdvancedLogger.error("sqlStr = $sqlStr")
            rsTable = conn.executeQuery(sqlStr)
        }

        var dataRowCount = 0        // счетчик логических строк на текущей странице (без учета отображения группировок)
        var globalRowCount = 0      // общий счётчик логических строк (для нумерации строк) (без учета отображения группировок)
        var tableRowCount = 0       // общий счетчик физических строк (с учетом отображения группировок)
        var nextPageExist = false
        val alTableCell = mutableListOf<TableCell>()
        val alTableRowData = mutableListOf<TableRowData>()

        val currentRowID = getIdFromParam()    // id текущей строки ( если вообще задан )
        var currentRowNo = -1                  // номер текущей строки
        while (isNextDataInTable(rsTable)) {
            val hmColumnData = getColumnData(rsTable, true, alColumnList) ?: continue
            //--- строка не проходит по правам доступа
            //--- возможная догенерация данных перед фильтрами поиска и страничной разбивки
            generateColumnDataBeforeFilter(hmColumnData)
            //--- поиск по строке
            if (alFindWord != null && alFindWord.size > 1 && !findInTableRow(hmColumnData, alFindWord)) {
                continue
            }
            //--- если строчка по поиску проходит ( т.е. засчитывается в состав искомых строк ),
            //--- но пропускается как "строка из предыдущих страниц", то возвращаем пустую строку
            val rowCount: Int
            if (aliasConfig.pageSize > 0 && globalRowCount < pageNo * aliasConfig.pageSize) {
                rowCount = 0
            } else {
                //--- возможная догенерация данных после фильтров поиска и страничной разбивки
                generateTableColumnDataAfterFilter(hmColumnData)
                rowCount = getTableRow(
                    hmColumnData = hmColumnData,
                    dataRowNo = dataRowCount,
                    globalRowNo = globalRowCount,
                    tableRowStart = tableRowCount,
                    selectorID = selectorID,
                    selectorParam = selectorParam,
                    refererID = refererID,
                    alTableCell = alTableCell,
                    alTableRowData = alTableRowData,
                    hmOut = hmOut
                )
                dataRowCount++
            }
            //--- вынесено сюда, чтобы нумерация строк работала независимо от наличия постраничной разбивки
            globalRowCount++
            tableRowCount += rowCount
            if ((hmColumnData[model.columnId] as DataInt).intValue == currentRowID) {
                currentRowNo = dataRowCount - 1
            }
            if (aliasConfig.pageSize > 0 && globalRowCount >= (pageNo + 1) * aliasConfig.pageSize) {
                nextPageExist = true
                break
            }
        }
        rsTable?.close()

        //--- кнопка отмены выбора
        val selectorCancelURL = if (selectorParam != null) {
            val formDataID = getRandomInt().toString()
            hmOut[AppParameter.FORM_DATA + formDataID] = selectorParam.hmColumnData
            getParamURL(
                aAlias = selectorParam.formAlias,
                aAction = AppAction.FORM,
                aRefererId = selectorParam.refererId,
                aId = selectorParam.recordId,
                aParentData = selectorParam.hmParentData,
                aParentUserId = selectorParam.parentUserId,
                aAltParams = "&${AppParameter.FORM_DATA}=$formDataID"
            )
        } else {
            ""
        }

        //--- панель поиска
        val findData = getFindForm(refererID, finderID)

        //--- панель перехода по страницам (делается ПОСЛЕ вывода таблицы, т.к. только тогда становится известно наличие след./пред. страниц)
        val alPageButton = mutableListOf<Pair<String, String>>()
        if (aliasConfig.pageSize > 0) {
            //--- предыдущие страницы
            if (pageNo > 0) {
                alPageButton.add(Pair(AppParameter.setParam(urlReferer, AppParameter.PAGE, "0"), "1"))  // для людей нумерация с "1"
                var pageStep = 1
                for (pageArrow in arrPageArrowLeft) {
                    val prevPageNo = pageNo - pageStep
                    if (prevPageNo <= 0) break
                    alPageButton.add(1, Pair(AppParameter.setParam(urlReferer, AppParameter.PAGE, prevPageNo.toString()), pageArrow))
                    pageStep *= 3
                }
            }
            //--- текущая страница
            alPageButton.add(Pair("", (pageNo + 1).toString()))
            //--- следущие страницы
            if (nextPageExist) {
                var pageStep = 1
                for (pageArrow in arrPageArrowRight) {
                    val nextPageNo = pageNo + pageStep
                    alPageButton.add(Pair(AppParameter.setParam(urlReferer, AppParameter.PAGE, nextPageNo.toString()), pageArrow))
                    pageStep *= 3
                }
            }
        }

        return TableResponse(
            tab = aliasConfig.descr,
            alHeader = alHeader,
            selectorCancelURL = selectorCancelURL,
            findURL = findData.first,
            findText = findData.second,
            alAddActionButton = (if (isAddEnabled()) getAddButtonURL(refererID, hmOut) else emptyList()),
            alServerActionButton = getServerAction(),
            alClientActionButton = getClientAction(),
            alColumnCaption = alCaption,
            alTableCell = alTableCell,
            alTableRowData = alTableRowData,
            selectedRow = currentRowNo,
            alPageButton = alPageButton
        )
    }

    protected open fun setSelectorParent(selectorParam: SelectorParameter) {}

    protected fun getTableCaption(forSelect: Boolean, urlReferer: String, sort: Int, alCaption: MutableList<Pair<String, String>>) {
        //--- пустой заголовок для нумератора строк
        if (aliasConfig.isShowRowNo) {
            alCaption.add(Pair("", "№"))
        }
        //--- пустой заголовок для кнопки возврата в вызывающую форму
        if (forSelect) {
            alCaption.add(Pair("", ""))
        }
        //--- пустые заголовки для групповых полей
        for (i in 0 until model.alTableGroupColumn.size) {
            alCaption.add(Pair("", ""))
        }
        //--- пустой заголовок для указателя имени пользователя записи при работе в режиме всех пользователей
        if (model.columnUser != null && aliasConfig.isShowUserColumn) {
            alCaption.add(Pair("", ""))
        }

        //--- заголовки табличных полей - только если в одном столбце только одно поле
        val tableColCount = model.getTableColumnColCount()
        for (col in 0 until tableColCount) {
            val column = model.getTableColumn(0, col)
            val isMultiColColumn = model.getTableColumnColCount(col) > 1
            alCaption.add(
                Pair(
                    if (column == null || isMultiColColumn || !column.isSortable()) {
                        ""
                    } else {
                        AppParameter.setParam(urlReferer, AppParameter.SORT, (if (abs(sort) - 1 == col) -sort else col + 1).toString())
                    },
                    if (column == null) {
                        ""
                    } else if (isMultiColColumn) {
                        //--- составляем неповторяющийся список заголовков с сохранением порядка полей
                        val captions = mutableListOf<String>()
                        for (row in 0 until model.getTableColumnColCount(col)) {
                            model.getTableColumn(row, col)?.let { c ->
                                val caption = if (c.tableCaption.isNotBlank()) {
                                    c.tableCaption
                                } else {
                                    c.caption
                                }
                                if (caption.isNotBlank() && !captions.contains(caption)) {
                                    captions += caption
                                }
                            }
                        }
                        captions.joinToString()
                    } else if (column.tableCaption.isNotBlank()) {
                        column.tableCaption
                    } else {
                        column.caption
                    }
                )
            )
        }
    }

    protected open fun isNextDataInTable(rs: CoreAdvancedResultSet?): Boolean = rs != null && rs.next()

    protected fun findInTableRow(hmColumnData: Map<iColumn, iData>, alFindWord: List<String>): Boolean {
        val recordUserID = getRecordUserId(hmColumnData)
        val recordUserName = getRecordUserName(recordUserID)

        val hmColumnCell = hmColumnData.mapValues { entry ->
            entry.value.getTableCell(
                rootDirName = application.rootDirName,
                conn = conn,
                row = -1,
                col = -1,
                dataRowNo = -1,
                isUseThousandsDivider = userConfig.upIsUseThousandsDivider,
                decimalDivider = userConfig.upDecimalDivider
            )
        }

        var sFind = "$recordUserName "

        model.alTableGroupColumn.filter { it.isSearchable }.forEach { column ->
            hmColumnCell[column]?.let { tc ->
                sFind += getCellTextForFind(column, tc)
            }
        }

        //--- сами поля данных
        val tableRowCount = model.getTableColumnRowCount()
        val tableColCount = model.getTableColumnColCount()
        for (row in 0 until tableRowCount) {
            for (col in 0 until tableColCount) {
                val column = model.getTableColumn(row, col)
                if (column == null || !column.isSearchable) {
                    continue
                }
                hmColumnCell[column]?.let { tc ->
                    sFind += getCellTextForFind(column, tc)
                }
            }
        }
        //--- переводим все в нижний регистр
        val strLowCaseFind = sFind.lowercase(locale)
        //--- 0-й индекс в alFindWord - полная строка поиска
        for (i in 1 until alFindWord.size) {
            //--- при первой же неудаче поиска выходим
            val findStr = alFindWord[i]
            //--- если искомое слово имеет префикс отрицания (одиночный символ "!" рассматривается как обычная поисковая строка)
            if (findStr[0] == '!' && findStr.length > 1) {
                if (strLowCaseFind.contains(findStr.substring(1))) {
                    return false
                }
            } else if (!strLowCaseFind.contains(findStr)) {
                return false
            }
        }
        return true
    }

    private fun getCellTextForFind(column: iColumn, tc: TableCell): String {
        var cellText = ""
        when (tc.cellType) {
            TableCellType.CHECKBOX -> {}
            TableCellType.TEXT -> {
                cellText +=
                    if (column is ColumnInt || column is ColumnDouble) {
                        tc.textCellData.text.replace(" ", "")
                    } else {
                        tc.textCellData.text
                    } + ' '
            }

            TableCellType.BUTTON -> {
                tc.alButtonCellData.forEach { tbcd ->
                    cellText += tbcd.text + ' '
                }
            }

            TableCellType.GRID -> {
                tc.alGridCellData.forEach { gridRow ->
                    gridRow.forEach { tgcd ->
                        cellText += tgcd.text + ' '
                    }
                }
            }
        }
        return cellText
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ДО и ПОСЛЕ фильтров поиска и страничной разбивки
    protected open fun generateColumnDataBeforeFilter(hmColumnData: MutableMap<iColumn, iData>) {}

    protected open fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {}

    protected fun getTableRow(
        hmColumnData: Map<iColumn, iData>,
        dataRowNo: Int,
        globalRowNo: Int,
        tableRowStart: Int,
        selectorID: String?,
        selectorParam: SelectorParameter?,
        refererID: String,
        alTableCell: MutableList<TableCell>,
        alTableRowData: MutableList<TableRowData>,
        hmOut: MutableMap<String, Any>
    ): Int {

        //--- вычислим и запомним кол-во один раз
        val tableColCount = model.getTableColumnColCount()

        val recordUserID = getRecordUserId(hmColumnData)
        val recordUserName = getRecordUserName(recordUserID)

        //--- для внешнего суммирования реального кол-ва выведенных строк
        var rowCount = 0

        val valueID = (hmColumnData[model.columnId] as DataInt).intValue

        //--- обработка для выделения isNew строки, если есть
        val isNewRow = aliasConfig.isNewable && userConfig.userId != UserConfig.USER_GUEST && !getTableRowIsReaded(valueID)

        //--- обработка группировочных полей
        //--- (подразумевается, что группировочными могут быть только ячейки типа TEXT (CHECKBOX, BUTTON и GRID в данной роли бессмысленны))
        for (gi in 0 until model.alTableGroupColumn.size) {
            val curGroupColumn = model.alTableGroupColumn[gi]
            val curValue = hmColumnData[curGroupColumn]!!.getTableCell(
                rootDirName = application.rootDirName,
                conn = conn,
                row = -1,
                col = -1,
                dataRowNo = -1,
                isUseThousandsDivider = userConfig.upIsUseThousandsDivider,
                decimalDivider = userConfig.upDecimalDivider
            ).textCellData.text
            if (curValue != arrCurGroupValue!![gi]) {
                val row = tableRowStart + rowCount
                var col = 0
                //--- пустая ячейка вместо номера строки
                if (aliasConfig.isShowRowNo) {
                    alTableCell.add(TableCell(row, col++))
                }
                //--- пустая ячейка вместо кнопки селектора
                if (selectorParam != null) {
                    alTableCell.add(TableCell(row, col++))
                }
                //--- пустые ячейки в строке группы (по предыдущим уровням) до наименования группы
                //--- ( передаем именно свежесозданный CoreTableCellInfo, т.к. у него будут меняться настройки стиля )
                for (i in 0 until gi) {
                    alTableCell.add(setTableGroupColumnStyle(hmColumnData, model.alTableGroupColumn[i], TableCell(row, col++)))
                }
                //--- посчитаем насколько там надо заспанить (прим.: без общих скобок компилятор Котлина не воспринимает это как общее выражение!)
                val spanCellCount = (1   // собственная ячейка
                    + model.alTableGroupColumn.size
                    + tableColCount
                    - gi  // ячейки до наименования группы
                    - 1   // ячейка с наименованием группы
                    + (if (model.columnUser != null && aliasConfig.isShowUserColumn) 1 else 0))

                //--- наименование группы
                alTableCell.add(
                    setTableGroupColumnStyle(
                        hmColumnData = hmColumnData,
                        column = curGroupColumn,
                        tci = TableCell(
                            aRow = row,
                            aCol = col,
                            aRowSpan = 1,
                            aColSpan = spanCellCount,
                            aDataRow = -1,
                            aAlign = TableCellAlign.LEFT,
                            aMinWidth = 0,
                            aIsWordWrap = false,
                            aTooltip = curGroupColumn.caption,
                            aText = curValue
                        )
                    )
                )

                //--- эта строка таблицы не содержит tooltip-ов, popup-menu и ссылок
                rowCount++

                //--- установить новое значение группировочного поля
                arrCurGroupValue!![gi] = curValue
                //--- сбросить текущие значения для более младших группировочных полей
                //--- ( т.е. перегруппировать младшие поля )
                for (j in gi + 1 until model.alTableGroupColumn.size) {
                    arrCurGroupValue!![j] = null
                }
            }
        }
        //--- разрешено ли открытие формы для данной строки таблицы?
        val formURL = if (checkPerm(PERM_FORM, recordUserID)) {
            getParamURL(
                aAlias = aliasConfig.name,
                aAction = AppAction.FORM,
                aRefererId = refererID,
                aId = valueID,
                aParentData = hmParentData,
                aParentUserId = parentUserId,
                aAltParams = null
            )
        } else {
            ""
        }
        //--- по умолчанию дабл-клик == открытию формы в том же окне
        var rowURL = formURL
        var isRowURLInNewWindow = false
        //--- отдельная кнопка перехода (goto) для тех, у кого не срабатывает дабл-клик или дабл-тач (iOS)
        var gotoURL = ""
        var isGotoURLInNewWindow = false
        //--- popup-menu на правую кнопку мыши по данной строке ( меню ссылок перехода на другие таблицы )
        val alPopupData = mutableListOf<TablePopupData>()
        for (i in 0 until model.alChildData.size) {
            val defaultOperationURL = getTableRowGoto(selectorID, hmColumnData, i, alPopupData)
            if (defaultOperationURL != null) {
                rowURL = defaultOperationURL
                //--- для иерархических таблиц операция по умолчанию - проход вглубь по иерархии, а она делается внутри одного/текущего окна
                isRowURLInNewWindow = isOpenFormURLInNewWindow()
                //--- специально для кнопки перехода
                gotoURL = rowURL
                isGotoURLInNewWindow = isRowURLInNewWindow
            }
        }
        //--- в некоторых случаях клик по строке может обрабатываться особенным образом,
        //--- например, в режиме селектора двойной клик делает выбор/возврат строки
        val defaultOperationURL = newTableRowDefaultOperation(selectorParam, hmColumnData, hmOut)
        if (defaultOperationURL != null) {
            rowURL = defaultOperationURL
            isRowURLInNewWindow = false
        }
        //--- основные строки таблицы
        val tableRowCount = model.getTableColumnRowCount()
        for (rowIndex in 0 until tableRowCount) {
            val row = tableRowStart + rowCount
            var col = 0
            //--- номер строки
            if (aliasConfig.isShowRowNo) {
                if (rowIndex == 0)
                    alTableCell.add(
                        TableCell(
                            aRow = row,
                            aCol = col,
                            aRowSpan = tableRowCount,
                            aColSpan = 1,
                            aDataRow = dataRowNo,
                            aAlign = TableCellAlign.CENTER,
                            aMinWidth = 0,
                            aIsWordWrap = false,
                            aTooltip = "Номер строки",
                            aText = (globalRowNo + 1).toString()
                        )
                    )
                col++
            }

            //--- подготовка функции возврата значения из таблицы в вызывающую форму
            if (selectorParam != null) {
                if (rowIndex == 0) {
                    val tci = getTableRowSelectButton(row, col, dataRowNo, selectorParam, hmColumnData, hmOut)
                    //--- сигнатуру метода getTableRowSelectButton не меняем,
                    //--- т.к. он и сложно перекрывается наследниками и применяется и в старых методах, а просто вручную правим rowSpan
                    tci.rowSpan = tableRowCount
                    alTableCell.add(tci)
                }
                col++
            }
            //--- линии уровня от группировочных полей
            for (column in model.alTableGroupColumn) {
                if (rowIndex == 0) {
                    val tci = setTableGroupColumnStyle(hmColumnData, column, TableCell(row, col))
                    //--- сигнатуру метода setTableGroupColumnStyle не меняем,
                    //--- т.к. он и сложно перекрывается наследниками и применяется и в старых методах, а просто вручную правим rowSpan
                    tci.rowSpan = tableRowCount
                    alTableCell.add(tci)
                }
                col++
            }
            //--- указатель имени пользователя записи при смешанном режиме просмотра пользователей
            if (model.columnUser != null && aliasConfig.isShowUserColumn) {
                if (rowIndex == 0)
                    alTableCell.add(
                        TableCell(
                            aRow = row,
                            aCol = col,
                            aRowSpan = tableRowCount,
                            aColSpan = 1,
                            aDataRow = dataRowNo,
                            aAlign = TableCellAlign.LEFT,
                            aMinWidth = 0,
                            aIsWordWrap = true,
                            aTooltip = "Пользователь",
                            aText = recordUserName
                        )
                    )
                col++
            }

            //--- сами поля данных
            for (colIndex in 0 until tableColCount) {
                val column = model.getTableColumn(rowIndex, colIndex) ?: continue

                //--- по умолчанию ячейка видима, если хотя бы один раз не доказано обратное
                var cellIsVisible = true
                for (i in 0 until column.getFormVisibleCount()) {
                    val fcvd = column.getFormVisible(i)
                    //--- определение контрольного значения
                    var controlValue = 0
                    val data = hmColumnData[fcvd.columnMaster]
                    if (data is DataBoolean) {
                        controlValue = if (data.value) {
                            1
                        } else {
                            0
                        }
                    } else if (data is DataAbstractSelector) {
                        controlValue = data.intValue
                    }
                    cellIsVisible = cellIsVisible and (fcvd.state == fcvd.values.contains(controlValue))
                    //--- одного доказательства невидимости достаточно
                    if (!cellIsVisible) {
                        break
                    }
                }
                //--- передаем именно свежесозданный CoreTableCellInfo, т.к. у него будут меняться настройки стиля
                val tci = if (cellIsVisible) {
                    hmColumnData[column]!!.getTableCell(
                        rootDirName = application.rootDirName,
                        conn = conn,
                        row = row,
                        col = col + colIndex,
                        dataRowNo = dataRowNo,
                        isUseThousandsDivider = userConfig.upIsUseThousandsDivider,
                        decimalDivider = userConfig.upDecimalDivider
                    )
                } else {
                    TableCell(row, col + colIndex)
                }
                //--- стиль столбца ( возможно перекрытие из описания самого класса )
                getTableColumnStyle(isNewRow, hmColumnData, column, tci)
                alTableCell.add(tci)
            }
            rowCount++
        }
        alTableRowData.add(
            TableRowData(
                formURL = formURL,
                rowURL = rowURL,
                isRowURLInNewWindow = isRowURLInNewWindow,
                gotoURL = gotoURL,
                isGotoURLInNewWindow = isGotoURLInNewWindow,
                alPopupData = alPopupData
            )
        )

        //--- необходимо "прочитывать" строки уже сразу после их вывода в таблице, без обязательного входа в форму
        if (aliasConfig.isNewAutoRead && isNewRow) {
            addIsReaded(valueID)
        }

        return rowCount
    }

    protected open fun getTableRowIsReaded(valueID: Int?): Boolean {
        if (valueID == null) return true

        val rs = conn.executeQuery(
            " SELECT ${conn.getPreLimit(1)} row_id FROM SYSTEM_new " +
                " WHERE table_name = '${model.modelTableName}' AND row_id = $valueID AND user_id = ${userConfig.userId} " +
                " ${conn.getMidLimit(1)} ${conn.getPostLimit(1)} "
        )
        val isReaded = rs.next()
        rs.close()

        return isReaded
    }

    //--- hmColumnData может пригодиться в наследниках для дополнительного выделения группы
    //--- ( например, дополнительно выделить красным текстом просроченные задания )
    protected open fun setTableGroupColumnStyle(hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell): TableCell {
        val index = model.alTableGroupColumn.indexOf(column)

        //--- если не указать чёрный цвет явно - будет белый шрифт при выделении
        tci.foreColorType = TableCellForeColorType.DEFINED
        tci.foreColor = 0xFF_00_00_00.toInt()
        tci.backColorType = if (index % 2 == 0) TableCellBackColorType.GROUP_0 else TableCellBackColorType.GROUP_1
        tci.fontStyle = 1

        return tci
    }

    //--- hmColumnData может пригодиться в наследниках для дополнительного выделения группы
    //--- ( например, дополнительно выделить красным текстом просроченные задания )
    protected open fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        tci.fontStyle = if (isNewRow) 1 else 0
    }

    protected open fun getTableRowSelectButton(
        row: Int,
        col: Int,
        dataRowNo: Int,
        selectorParam: SelectorParameter,
        hmColumnData: Map<iColumn, iData>,
        hmOut: MutableMap<String, Any>
    ): TableCell {
        //--- глубокая копия column-data с данными селекта
        val hmNewColumnData = selectorParam.hmColumnData.map { (column, data) ->
            column to data.clone() as iData
        }.toMap().toMutableMap()

        for (i in selectorParam.alColumnTo.indices) {
            val columnFrom = selectorParam.alColumnFrom[i]
            //--- запомнить прежнее (возможно, подстановочное) имя таблицы
            val oldTableName = columnFrom.columnTableName
            //--- если есть реальное имя таблицы (т.е. нынешнее значение - подстановочное),
            //--- то на время поиска поля в выборке заменим подстановочное имя таблицы на реальное
            if (columnFrom.selfLinkTableName != null) {
                columnFrom.columnTableName = columnFrom.selfLinkTableName!!
            }
            //AdvancedLogger.error(  "columnFrom = " + columnFrom.getTableName() + "." + columnFrom.getFieldName(  0  )  );
            val dataFrom = hmColumnData[columnFrom]
            //--- вернём прежнее имя таблицы ПОСЛЕ получения dataFrom
            columnFrom.columnTableName = oldTableName

            val columnTo = selectorParam.alColumnTo[i]
            //AdvancedLogger.error(  "columnTo = " + columnTo.getTableName() + "." + columnTo.getFieldName(  0  )  );
            dataFrom?.let {
                hmNewColumnData[columnTo]?.setData(it)
            }
        }

        val formDataID = getRandomInt().toString()
        hmOut[AppParameter.FORM_DATA + formDataID] = hmNewColumnData

        return TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = 1,
            aColSpan = 1,
            aDataRow = dataRowNo,

            aAlign = TableCellAlign.LEFT,
            aMinWidth = 0,
            aTooltip = "Выбрать эту строку",

            aIcon = ICON_NAME_SELECT,
            aText = "",
            aUrl = getParamURL(
                aAlias = selectorParam.formAlias,
                aAction = AppAction.FORM,
                aRefererId = selectorParam.refererId,
                aId = selectorParam.recordId,
                aParentData = selectorParam.hmParentData,
                aParentUserId = selectorParam.parentUserId,
                aAltParams = "&${AppParameter.FORM_DATA}=$formDataID"
            ),
            aInNewWindow = false
        )
    }

    protected open fun getTableRowGoto(selectorID: String?, hmColumnData: Map<iColumn, iData>, indexChild: Int, alPopupData: MutableList<TablePopupData>): String? {

        val childData = model.alChildData[indexChild]
        val obj = childData.alias
        var childAlias: String
        //--- если это ссылка на поле, содержащее алиас
        if (obj is ColumnString) {
            val ds = hmColumnData[obj] as DataString
            childAlias = ds.text
            if (childAlias.isBlank()) {
                childAlias = aliasConfig.name
            }
        } else {
            childAlias = obj as String
        }

        //--- если таблица вызвана для селекта, то разрешаем переходы только на таблицы с совпадающим алиасом
        //--- ( для реализации иерархических таблиц )
        if (selectorID != null && childAlias != aliasConfig.name) {
            return null
        }
        //--- проверка прав доступа на child-классы
        val hsPerm = userConfig.userPermission[childAlias]
        if (hsPerm == null || !hsPerm.contains(PERM_ACCESS)) {
            return null
        }
        val hmNewParentData = mutableMapOf<String, Int>()
        putTableRowGotoNewParentData(hmColumnData, indexChild, hmNewParentData)
        val acChild = hmAliasConfig[childAlias]!!

        //--- добавим разделитель в меню, если надо
        if (childData.isNewGroup && alPopupData.isNotEmpty()) {
            alPopupData.add(TablePopupData("", "", "", false))
        }

        //--- если есть свой columnUser, то передаем его значение, иначе передаем дальше родительский userID
        var newParentUserID = getRecordUserId(hmColumnData)
        if (newParentUserID == 0) {
            newParentUserID = parentUserId
        }

        val popupURL = getParamURL(
            childAlias, childData.action, null, if (childData.action == AppAction.FORM) 0 else null, hmNewParentData, newParentUserID,
            if (selectorID == null) {
                ""
            } else {
                "&${AppParameter.SELECTOR}=$selectorID"
            }
        )

        alPopupData.add(TablePopupData(childData.group, popupURL, acChild.descr, childAlias != aliasConfig.name))

        return if (childData.isDefaultOperation) {
            popupURL
        } else {
            null
        }
    }

    //--- для наследников - можно добавлять дополнительные паренты для child'ов
    protected open fun putTableRowGotoNewParentData(hmColumnData: Map<iColumn, iData>, indexChild: Int, hmNewParentData: MutableMap<String, Int>) {
        hmNewParentData[aliasConfig.name] = (hmColumnData[model.alChildData[indexChild].column] as DataInt).intValue
    }

    //--- для наследников - можно изменить реакцию на действие по умолчанию (двойной клик по строке таблицы)
    protected open fun newTableRowDefaultOperation(selectorParam: SelectorParameter?, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? =
        //--- в режиме селектора double-click делает возврат строки аналогично нажатию соответствующей кнопки
        selectorParam?.let {
            getTableRowSelectButton(-1, -1, -1, selectorParam, hmColumnData, hmOut).alButtonCellData.first().url
        }

    protected open fun isOpenFormURLInNewWindow(): Boolean = true

    protected fun getFindForm(refererID: String?, aFinderID: String?): Pair<String, String> {
        var finderID = aFinderID

        //--- если были заданы поисковые значения
        val findStr: String
        if (finderID == null) {
            finderID = getRandomInt().toString()
            findStr = ""
        } else {
            val alWord = chmSession[AppParameter.FINDER + finderID] as? List<String>
            findStr = if (alWord == null) "" else alWord[0]
        }

        val sbFindURL = StringBuilder(AppParameter.ALIAS).append('=').append(aliasConfig.name).append('&').append(AppParameter.ACTION).append('=').append(AppAction.FIND)
        if (refererID != null) sbFindURL.append('&').append(AppParameter.REFERER).append('=').append(refererID)
        sbFindURL.append('&').append(AppParameter.FINDER).append('=').append(finderID)

        return Pair(sbFindURL.toString(), findStr)
    }

    protected open fun getAddButtonURL(refererID: String, hmOut: MutableMap<String, Any>): MutableList<AddActionButton> {
        val alAddButtonList = mutableListOf<AddActionButton>()

        alAddButtonList.add(
            AddActionButton(
                caption = "Добавить",
                tooltip = "Добавить",
                icon = ICON_NAME_ADD_ITEM,
                url = getParamURL(aliasConfig.name, AppAction.FORM, refererID, 0, hmParentData, parentUserId, null)
            )
        )

        return alAddButtonList
    }

    protected open fun getServerAction(): MutableList<ServerActionButton> {
        return mutableListOf()
    }

    protected open fun getClientAction(): MutableList<ClientActionButton> {
        return mutableListOf()
    }

//--- form part -----------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun getForm(hmOut: MutableMap<String, Any>): FormResponse {
        val id = getIdFromParam()!!

        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alFormHiddenColumn)
        alColumnList.addAll(model.alFormColumn)

        val refererID = hmParam[AppParameter.REFERER]
        val refererURL = refererID?.let { chmSession[AppParameter.REFERER + it] as? String }

        //--- preparing a "clean" appParam for form buttons
        //--- (simple cloning of the original hmParam won't do here,
        //--- since a lot of associated garbage parameters will come, which can suddenly fire somewhere)
        val formParam = getFormParam()

        var hmColumnData: MutableMap<iColumn, iData>? = null
        //--- возврат с ошибками ввода в форме или после выбора из справочника
        val formDataID = hmParam[AppParameter.FORM_DATA]
        if (formDataID != null) {
            hmColumnData = chmSession[AppParameter.FORM_DATA + formDataID] as? MutableMap<iColumn, iData>?
            if (hmColumnData != null) {
                postProcessFormColumnDataFromFormData(id, hmColumnData)
            }
        }

        //--- это первый запуск формы
        if (hmColumnData == null) {
            //--- предварительное заполнение 0-й записи при добавлении
            if (id == 0) {
                //--- заполним значениями по умолчанию
                hmColumnData = getFormDefaultValues(alColumnList, true)
                //--- заполним авто-парентами
                for (column in alColumnList) {
                    if (column.selectorAlias == null || column.isNotUseParentId) {
                        continue
                    }
                    val pID = getParentId(column.selectorAlias)
                    if (pID != null && pID != 0) {
                        (hmColumnData[column.getSelectTo()[0]] as DataInt).intValue = pID
                    }
                }
                //--- для модулей с виртуальными таблицами служебную запись не делаем
                if (model.modelTableName != mAbstract.FAKE_TABLE_NAME) {
                    //--- заполним или создадим 0-ю служебную запись с этими данными
                    if (doUpdate(0, alColumnList, hmColumnData) == 0) {
                        doInsert(0, alColumnList, hmColumnData)
                    }
                }
            }
            //--- для модулей с виртуальными таблицами чтение не делаем
            if (model.modelTableName != mAbstract.FAKE_TABLE_NAME) {
                //--- теперь загрузим запись (в случае добавления загружаем предварительно заполненную 0-ю служебную запись -
                //--- все только для того, чтобы избежать автозаполнения через скрипты)
                val sqlStr = getSQLString(
                    isForTable = false,
                    id = id,
                    alColumnList = alColumnList,
                    alSortColumn = null,
                    alSortDirect = null,
                    alFindWord = null
                )
//println("--- getForm ------------------------------------------------")
//println("sqlStr = $sqlStr")
                val rs = conn.executeQuery(sqlStr)
                if (rs.next()) {
                    hmColumnData = getColumnData(rs, false, alColumnList)
                }
                rs.close()
                //--- добавить свою "прочитанность"
                addIsReaded(id)
            }
            //--- служебную запись вернем в максимально "нулевое" состояние
            if (id == 0 && model.modelTableName != mAbstract.FAKE_TABLE_NAME) {
                doUpdate(0, alColumnList, getFormDefaultValues(alColumnList, false))
            }
        }
        if (hmColumnData == null) {
            throw BusinessException("Просмотр данных в форме не разрешен.")
        }
        //--- генерация вычисляемых полей
        getCalculatedFormColumnData(id, hmColumnData)

        //--- заранее сохраним разрешение сохранения, чтобы использовать при выводе
        val isEditEnabled = isEditEnabled(hmColumnData, id)

        //--- заголовок формы
        val alHeader = mutableListOf<Pair<String, String>>()
        fillHeader(null, false, alHeader, hmOut)

        val alFormCell = mutableListOf<FormCell>()

        //--- невидимые поля
        for (column in model.alFormHiddenColumn) {
            if (column == model.columnId) continue // свое id-поле пропускаем, т.к. оно задается особым образом
            alFormCell.add(getFormCell(column, hmColumnData, false))
        }
        //--- основные поля
        for (column in model.alFormColumn) {
            val fci = getFormCell(column, hmColumnData, isEditEnabled)
            fci.caption = column.caption
            if (isEditEnabled) {
                //--- кнопки селектора ( если есть )
                fci.selectorSetURL = if (column.selectorAlias == null) {
                    ""
                } else {
                    getFormSelector(
                        formParam = formParam,
                        forClear = false,
                        id = id,
                        refererID = refererID,
                        column = column,
                        hmColumnData = hmColumnData,
                        hmOut = hmOut
                    )
                }
                fci.selectorClearURL = if (column.selectorAlias == null) {
                    ""
                } else {
                    getFormSelector(
                        formParam = formParam,
                        forClear = true,
                        id = id,
                        refererID = refererID,
                        column = column,
                        hmColumnData = hmColumnData,
                        hmOut = hmOut
                    )
                }
                //--- определяем автозапуск селектора
                fci.isAutoStartSelector = column.isAutoStartSelector
                //--- текст сообщения об ошибке ввода ( если есть )
                fci.errorMessage = hmColumnData[column]!!.getError() ?: ""
            }
            alFormCell.add(fci)
        }

        val alFormButton = mutableListOf<FormButton>()
        if (isEditEnabled) {
            alFormButton.add(
                FormButton(
                    url = getSaveButtonParams(formParam),
                    caption = model.getSaveButonCaption(aliasConfig),
                    iconName = getOkButtonIconName(),
                    withNewData = true,   // передавать ли новые данные (для SAVE) или передать старые данные (для DELETE и проч. комманд)
                    key = if (isFormAutoClick()) {
                        BUTTON_KEY_AUTOCLICK
                    } else {
                        BUTTON_KEY_SAVE
                    }
                )
            )
        }
        if (model.columnActive != null && model.columnArchive != null) {
            if (isArchiveEnabled(hmColumnData, id)) {
                alFormButton.add(
                    FormButton(
                        url = AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.ARCHIVE),
                        caption = "В архив",
                        iconName = ICON_NAME_ARCHIVE,
                        withNewData = true,
                        key = BUTTON_KEY_NONE
                    )
                )
            }
            if (isUnarchiveEnabled(hmColumnData, id)) {
                alFormButton.add(
                    FormButton(
                        url = AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.UNARCHIVE),
                        caption = "Из архива",
                        iconName = ICON_NAME_UNARCHIVE,
                        withNewData = true,
                        key = BUTTON_KEY_NONE
                    )
                )
            }
        }
        if (isDeleteEnabled(hmColumnData, id)) {
            alFormButton.add(
                FormButton(
                    url = AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.DELETE),
                    caption = "Удалить",
                    iconName = ICON_NAME_DELETE,
                    withNewData = false,
                    key = BUTTON_KEY_NONE,
                    question = model.deleteQuestion,
                )
            )
        }
        if (refererURL != null) {
            alFormButton.add(
                FormButton(
                    url = if (id != 0) {
                        AppParameter.setParam(refererURL, AppParameter.ID, id.toString())
                    } else {
                        refererURL
                    },
                    caption = "Выйти",
                    iconName = ICON_NAME_EXIT,
                    withNewData = false,
                    key = BUTTON_KEY_EXIT
                )
            )
        }

        return FormResponse(
            tab = aliasConfig.descr,
            alHeader = alHeader,
            columnCount = 0,    // 0 = это обычная ( т.е. не GRID ) форма
            alFormColumn = listOf(),
            alFormCell = alFormCell,
            alFormButton = alFormButton
        )
    }

    //--- подготовка "чистого" appParam для кнопок формы (простое клонирование исходного hmParam здесь не годится,
    //--- т.к. придёт много попутных мусорных параметров, которые могут внезапно выстрелить где-нибудь)
    protected fun getFormParam(): String {
        val arrFormParams = arrayOf(
            AppParameter.ALIAS,
            AppParameter.REFERER,
            AppParameter.PARENT_ALIAS,
            AppParameter.PARENT_ID,
            AppParameter.PARENT_USER_ID,
            AppParameter.ID
        )
        val hmFormParam = arrFormParams.map { it to (hmParam[it] ?: "") }.filterNot { it.second.isEmpty() }.toMap()
        return AppParameter.collectParam(hmFormParam)
    }

    protected open fun postProcessFormColumnDataFromFormData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {}

    protected fun getFormDefaultValues(alColumnList: List<iColumn>, forSetting: Boolean): MutableMap<iColumn, iData> {
        val hmColumnData = mutableMapOf<iColumn, iData>()

        for (column in alColumnList) {
            val data = column.getData()
            if (forSetting) data.loadFromDefault()
            hmColumnData[column] = data
        }
        return hmColumnData
    }

    protected open fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {}

    protected fun getFormCell(column: iColumn, hmColumnData: Map<iColumn, iData>, isEditable: Boolean): FormCell {
        val fci = hmColumnData[column]!!.getFormCell(
            rootDirName = application.rootDirName,
            conn = conn,
            isUseThousandsDivider = userConfig.upIsUseThousandsDivider,
            decimalDivider = userConfig.upDecimalDivider
        )
        fci.isEditable = isEditable && column.isEditable && column.columnTableName == model.modelTableName
        fci.formPinMode = column.formPinMode
        fci.isAutoFocus = column.isAutoFocus

        //--- эту чисто серверную часть нежелательно передавать в клиенто-ориентированный FormCellInfo
        fci.alVisible = fci.alVisible.toMutableList().apply {
            for (i in 0 until column.getFormVisibleCount()) {
                val fcvd = column.getFormVisible(i)
                //--- выжимка из getFieldCellName
                add(Triple("${fcvd.columnMaster.columnTableName}___${fcvd.columnMaster.getFieldName(0)}", fcvd.state, fcvd.values.toTypedArray()))
            }
        }
        fci.alCaption = fci.alCaption.toMutableList().apply {
            for (i in 0 until column.getFormCaptionCount()) {
                val fccd = column.getFormCaption(i)
                //--- выжимка из getFieldCellName
                add(Triple("${fccd.columnMaster.columnTableName}___${fccd.columnMaster.getFieldName(0)}", fccd.caption, fccd.values.toTypedArray()))
            }
        }
        return fci
    }

    protected fun getFormSelector(
        formParam: String,
        forClear: Boolean,
        id: Int,
        refererID: String?,
        column: iColumn,
        hmColumnData: Map<iColumn, iData>,
        hmOut: MutableMap<String, Any>,
    ): String {
        val selectorParam = SelectorParameter()
        selectorParam.forClear = forClear

        selectorParam.formAlias = aliasConfig.name
        selectorParam.recordId = id
        selectorParam.refererId = refererID
        selectorParam.hmParentData = hmParentData
        selectorParam.parentUserId = parentUserId

        selectorParam.selectorAlias = column.selectorAlias!!
        selectorParam.selectedId = (hmColumnData[column.getSelectTo()[0]] as DataInt).intValue
        selectorParam.selectedParentId = chmSession[AppParameter.SAVED_SELECTOR_PARENT + column.selectorAlias] as? Int ?: 0
        selectorParam.alColumnTo = column.getSelectTo()
        selectorParam.alColumnFrom = column.getSelectFrom()

        val selectorID = getRandomInt().toString()
        hmOut[AppParameter.SELECTOR + selectorID] = selectorParam

        return AppParameter.setParam(AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.SAVE), AppParameter.FORM_SELECTOR, selectorID)
    }

    protected open fun getSaveButtonParams(formParam: String): String = AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.SAVE)

    protected open fun getOkButtonIconName(): String = ICON_NAME_SAVE

    //--- используется ли автоклик/автовыход из формы эмуляцией нажатия кнопки OK/SAVE/SHOW
    protected open fun isFormAutoClick(): Boolean = false

//--- find part -----------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun doFind(findStr: String?, hmOut: MutableMap<String, Any>): String? {

        val finderID = hmParam[AppParameter.FINDER]
        val refererID = hmParam[AppParameter.REFERER]
        val refererURL = chmSession[AppParameter.REFERER + refererID] as? String

        //--- если сервер вдруг перезагрузили между отдельными командами поиска
        //--- ( такое бывает редко, только при обновлениях, но тем не менее пользователю обидно ), то сделаем вид что поиска не было :)
        if (findStr == null || finderID == null || refererID == null || refererURL == null) {
            return null
        }

        val alWord = mutableListOf<String>()    // набор готовых к поиску слов
        var sWord = ""                             // набор слов, разделенных пробелами

        alWord.add(findStr)  // нулевым элементом запишем саму поисковую строку

        //--- разбор строки поиска на предмет строк, ограниченных кавычками
        var startPos = 0
        while (true) {
            val begPos = findStr.indexOf('"', startPos)
            if (begPos == -1) {
                sWord += findStr.substring(startPos)
                break
            }
            sWord += findStr.substring(startPos, begPos)
            val endPos = findStr.indexOf('"', begPos + 1)
            if (endPos == -1) {
                sWord += findStr.substring(begPos)
                break
            }
            val tmpStr = findStr.substring(begPos + 1, endPos)
            if (tmpStr.isNotEmpty()) alWord.add(tmpStr.toLowerCase(locale))

            startPos = endPos + 1
        }
        //--- разбор строк, ограниченных пробелами
        val st = StringTokenizer(sWord, " ")
        while (st.hasMoreTokens())
            alWord.add(st.nextToken().toLowerCase(locale))

        hmOut[AppParameter.FINDER + finderID] = alWord
        return AppParameter.setParam(AppParameter.setParam(refererURL, AppParameter.FINDER, finderID), AppParameter.PAGE, "0")
    }

//--- save part -----------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alFormHiddenColumn)
        alColumnList.addAll(model.alFormColumn)

        val hmColumnData = mutableMapOf<iColumn, iData>()

        //--- исправление (возможно, неправильно заданных) настроек
        model.columnVersionNo?.apply {
            isRequired = true    // номер версии должен быть заполнен
        }

        var id = getIdFromParam()!!

        //--- ошибки ввода в форме
        val returnURL = checkInput(id, alFormData, alColumnList, hmColumnData, hmOut)
        //--- урло на возврат с ошибкой
        if (returnURL != null) {
            return returnURL
        }

        //--- определить изменения номера версии в версионном поле
        val isVersionChanged = if (model.columnVersionNo != null && id != 0) {
            var oldVersionNo = ""
            val rs = conn.executeQuery(" SELECT ${model.columnVersionNo!!.getFieldName()} FROM ${model.modelTableName} WHERE id = $id ")
            if (rs.next()) {
                oldVersionNo = rs.getString(1)
            }
            rs.close()

            val curVersionNo = (hmColumnData[model.columnVersionNo!!] as DataString).text

            oldVersionNo != curVersionNo
        } else {
            false
        }

        //--- предварительные действия перед сохранением
        preSave(id, hmColumnData)
        //--- пред-обработка ( сохранение файлов/картинок/проч. в серверных папках )
        for (column in alColumnList) {
            //--- своё id-поле пропускаем, т.к. не изменяется
            if (column == model.columnId) {
                continue
            }
            //--- виртуальным полям нельзя делать предзапись
            if (column.isVirtual) {
                continue
            }
            if (column.columnTableName == model.modelTableName) {
                hmColumnData[column]!!.preSave(application.rootDirName, conn)
            }
        }
        val postUrl: String?
        if (id == 0) {
            val (addId, addPostUrl) = doAdd(alColumnList, hmColumnData, hmOut)
            id = addId
            postUrl = addPostUrl
        }
        //--- изменение версии - как добавление, только versionId переопределять уже не надо
        else if (isVersionChanged) {
            id = getNextId(hmColumnData)
            doInsert(id, alColumnList, hmColumnData)
            addIsReaded(id)
            postUrl = postAdd(id, hmColumnData, hmOut)
            model.getAddAlertTag()?.let { alertTag ->
                doAlert(id, hmColumnData, alertTag)
            }
        }
        //--- обычное редактирование
        else {
            if (model.columnActive != null && model.columnArchive != null) {
                val dataActive = hmColumnData[model.columnActive!!] as DataBoolean
                val dataArchive = hmColumnData[model.columnArchive!!] as DataBoolean
                if (action == AppAction.ARCHIVE) {
                    dataActive.value = false
                    dataArchive.value = true
                } else if (action == AppAction.UNARCHIVE) {
                    dataActive.value = true
                    dataArchive.value = false
                }
            }
            doUpdate(id, alColumnList, hmColumnData)
            deleteIsReaded(id, false)
            postUrl = postEdit(action, id, hmColumnData, hmOut)
            model.getEditAlertTag()?.let { alertTag ->
                doAlert(id, hmColumnData, alertTag)
            }
        }

        return postUrl ?: AppParameter.setParam(chmSession[AppParameter.REFERER + hmParam[AppParameter.REFERER]] as String, AppParameter.ID, id.toString())
    }

    protected fun checkInput(id: Int, alFormData: List<FormData>, alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        var isValidFormData = getFormValues(id, alFormData, alColumnList, hmColumnData)

        //--- после успешных основных проверок дополнительно проверим на парную уникальность версионных полей (для режима редактирования записи)
        if (isValidFormData && model.columnVersionId != null && model.columnVersionNo != null && id != 0) {
            val dataVersionId = hmColumnData[model.columnVersionId!!] as DataInt
            val dataVersionNo = hmColumnData[model.columnVersionNo!!] as DataString
            val result = conn.checkExisting(
                model.modelTableName,
                listOf(
                    Pair(model.columnVersionId!!.getFieldName(), dataVersionId.intValue),
                    Pair(model.columnVersionNo!!.getFieldName(), dataVersionNo.text)
                ),
                model.columnId.getFieldName(),
                id
            )
            if (result) {
                dataVersionNo.setError(dataVersionNo.text, "Это значение уже существует")
                isValidFormData = false
            }
        }

        val formSelectorID = hmParam[AppParameter.FORM_SELECTOR]
        val selectorID = formSelectorID?.toIntOrNull() ?: 0
        //--- запуск селектора
        if (selectorID != 0) {
            val selectorParam = chmSession[AppParameter.SELECTOR + selectorID] as SelectorParameter
            //--- очистка выбора
            if (selectorParam.forClear) {
                for (i in selectorParam.alColumnTo.indices) {
                    val columnFrom = selectorParam.alColumnFrom[i]
                    val dataFrom = columnFrom.getData()

                    dataFrom.loadFromDefault()

                    val columnTo = selectorParam.alColumnTo[i]
                    val dataTo = hmColumnData[columnTo]!!
                    dataTo.setData(dataFrom)
                }
                val formDataID = getRandomInt().toString()
                hmOut[AppParameter.FORM_DATA + formDataID] = hmColumnData
                return getParamURL(
                    aAlias = selectorParam.formAlias,
                    aAction = AppAction.FORM,
                    aRefererId = selectorParam.refererId,
                    aId = selectorParam.recordId,
                    aParentData = selectorParam.hmParentData,
                    aParentUserId = selectorParam.parentUserId,
                    aAltParams = "&${AppParameter.FORM_DATA}=$formDataID"
                )
            } else {
                selectorParam.hmColumnData = hmColumnData
                return getParamURL(
                    aAlias = selectorParam.selectorAlias,
                    aAction = AppAction.TABLE,
                    aRefererId = null,
                    aId = null,
                    aParentData = null,
                    aParentUserId = null,
                    aAltParams = "&${AppParameter.SELECTOR}=$selectorID"
                )
            }
        } else if (!isValidFormData) {
            val formDataID = getRandomInt().toString()
            hmOut[AppParameter.FORM_DATA + formDataID] = hmColumnData
            return getInvalidFormDataUrl(id, formDataID)
        } else return null
    }

    protected open fun getFormValues(id: Int, alFormData: List<FormData>, alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>): Boolean {
        var isValid = true
        //--- перегон объектов из AdvancedByteBuffer в типизированное Data Object Storage
        var formDataIndex = 0
        for (column in alColumnList) {
            // свое id-поле пропускаем, т.к. оно задается особым образом
            if (column == model.columnId) {
                continue
            }
            val data = column.getData()
            isValid = isValid and data.loadFromForm(conn, alFormData[formDataIndex++], model.columnId.getFieldName(), id)
            hmColumnData[column] = data
        }
        //--- проверка на уникальность
        if (isValid) {
            model.alUniqueColumnData.forEach { alUniqueColumnData ->
                val alFilteredUniqueCheckData = alUniqueColumnData.filter { uniqueColumnData ->
                    //--- special case: getUniqueCheckValue(0) - "unique ignore data" not used for multi-column data
                    uniqueColumnData.ignore == null || uniqueColumnData.ignore != hmColumnData[uniqueColumnData.column]!!.getUniqueCheckValue(0)
                }
                if (alFilteredUniqueCheckData.isNotEmpty()) {
                    val alFieldCheck = mutableListOf<Pair<String, Any>>()
                    alFilteredUniqueCheckData.forEach { uniqueColumnData ->
                        for (ci in 0 until uniqueColumnData.column.getFieldCount()) {
                            alFieldCheck += Pair(uniqueColumnData.column.getFieldName(ci), hmColumnData[uniqueColumnData.column]!!.getUniqueCheckValue(ci))
                        }
                    }
                    val existingCheckResult = conn.checkExisting(
                        aTableName = model.modelTableName,
                        alFieldCheck = alFieldCheck,
                        aFieldID = model.columnId.getFieldName(),
                        id = id
                    )
                    if (existingCheckResult) {
                        alFilteredUniqueCheckData.forEach { uniqueColumnData ->
                            hmColumnData[uniqueColumnData.column]!!.setUniqueCheckingError("Это значение уже существует")
                        }
                    }
                    isValid = isValid and !existingCheckResult
                }
            }
        }
        return isValid
    }

    protected open fun getInvalidFormDataUrl(id: Int, formDataID: String): String =
        getParamURL(aliasConfig.name, AppAction.FORM, hmParam[AppParameter.REFERER], id, hmParentData, parentUserId, "&${AppParameter.FORM_DATA}=$formDataID")

    //--- для классов-наследников - пред-обработка сохранения
    protected open fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {}

    protected open fun getNextId(hmColumnData: Map<iColumn, iData>): Int {
        return conn.getNextIntId(model.modelTableName, model.columnId.getFieldName())
    }

    protected open fun doAdd(alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>, hmOut: MutableMap<String, Any>): Pair<Int, String?> {
        val id = getNextId(hmColumnData)
        model.columnVersionId?.let {
            val versionId = conn.getNextIntId(model.modelTableName, it.getFieldName())
            (hmColumnData[it] as DataInt).intValue = versionId
        }
        doInsert(id, alColumnList, hmColumnData)
        addIsReaded(id)
        val postUrl = postAdd(id, hmColumnData, hmOut)
        model.getAddAlertTag()?.let { alertTag ->
            doAlert(id, hmColumnData, alertTag)
        }
        return Pair(id, postUrl)
    }

    protected open fun doInsert(id: Int, alColumnList: List<iColumn>, hmColumnData: Map<iColumn, iData>): Int {
        var sFieldList = model.columnId.getFieldName()
        var sValueList = "$id"
        for (column in alColumnList) {
            //--- свое id-поле пропускаем, т.к. оно задается особым образом
            if (column == model.columnId) {
                continue
            }
            //--- сохраняем значение запоминаемого поля
            if (column.isSavedDefault) {
                column.saveDefault(application, conn, userConfig, hmColumnData)
            }
            //--- виртуальные поля не записываем
            if (column.isVirtual) {
                continue
            }

            //--- записываем поля только из "своей" таблицы
            if (column.columnTableName == model.modelTableName) {
                val data = hmColumnData[column]!!
                for (j in 0 until data.fieldSQLCount) {
                    sFieldList += (if (sFieldList.isEmpty()) "" else " , ") + column.getFieldName(j)
                    sValueList += (if (sValueList.isEmpty()) "" else " , ") + data.getFieldSQLValue(j)
                }
            }
        }

        return conn.executeUpdate(" INSERT INTO ${model.modelTableName}( $sFieldList ) VALUES ( $sValueList ) ")
    }

    protected fun doUpdate(id: Int, alColumnList: List<iColumn>, hmColumnData: Map<iColumn, iData>): Int {
        var sFieldList = ""
        for (column in alColumnList) {
            //--- своё id-поле пропускаем, т.к. оно задается особым образом
            if (column == model.columnId) {
                continue
            }
            //--- сохраняем значение запоминаемого поля
            if (column.isSavedDefault) {
                column.saveDefault(application, conn, userConfig, hmColumnData)
            }
            //--- виртуальные поля не записываем
            if (column.isVirtual) {
                continue
            }

            //--- записываем поля только из "своей" таблицы
            if (column.columnTableName == model.modelTableName) {
                val data = hmColumnData[column]!!
                for (j in 0 until data.fieldSQLCount) {
                    sFieldList += if (sFieldList.isEmpty()) {
                        ""
                    } else {
                        " , "
                    } + "${column.getFieldName(j)} = ${data.getFieldSQLValue(j)} "
                }
            }
        }

        return conn.executeUpdate(" UPDATE ${model.modelTableName} SET $sFieldList WHERE ${model.columnId.getFieldName()} = $id ")
    }

    //--- для классов-наследников - пост-обработка после добавления
    protected open fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? = null

    //--- для классов-наследников - пост-обработка после редактирования
    protected open fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? = null

    protected fun doAlert(id: Int, hmColumnData: Map<iColumn, iData>, alertTag: String) {
        //--- если оповещение должно быть уникальным ( не повторяться ),
        //--- то удалим предыдущую версию, если она ещё есть в очереди
        if (model.isUniqueAlertRowId()) {
            conn.executeUpdate(" DELETE FROM SYSTEM_alert WHERE tag = '$alertTag' AND row_id = $id ")
        }
        //--- если заданы поля, то приготовим дату/время для оповещения
        var alertTime = getCurrentTimeInt()
        val arrDateTimeColumn = model.getDateTimeColumns()
        if (arrDateTimeColumn != null) {
            val dcb = hmColumnData[arrDateTimeColumn[0]] as DataComboBox
            //--- возможное выключение оповещения для конкретной записи
            if (dcb.intValue >= 0) {
                //--- поле упреждения и одно поле ColumnDateTime
                if (arrDateTimeColumn.size == 2) {
                    val ddt = hmColumnData[arrDateTimeColumn[1]] as DataDateTimeInt
                    alertTime = ddt.zonedDateTime.toEpochSecond().toInt()
                } else {
                    val dd = hmColumnData[arrDateTimeColumn[1]] as DataDate3Int
                    val dt = hmColumnData[arrDateTimeColumn[2]] as DataTime3Int
                    alertTime = ZonedDateTime.of(dd.localDate, dt.localTime, zoneId).toEpochSecond().toInt()
                }//--- поле упреждения и пара полей ColumnDate и ColumnTime
                //--- сохраняем время в секундах с заданным упреждением
                alertTime -= dcb.intValue * 60
                conn.executeUpdate(
                    " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " + conn.getNextIntId("SYSTEM_alert", "id") +
                        " , $alertTime , '$alertTag' , $id  ) "
                )
            }
        } else {
            conn.executeUpdate(
                " INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( " + conn.getNextIntId("SYSTEM_alert", "id") +
                    " , $alertTime , '$alertTag' , $id ) "
            )
        }
    }

//--- delete part ---------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun doDelete(alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String {
        val id = getIdFromParam()!!

        val alColumnList = mutableListOf<iColumn>()
        alColumnList.addAll(model.alFormHiddenColumn)
        alColumnList.addAll(model.alFormColumn)

        val hmColumnData = mutableMapOf<iColumn, iData>()

        //--- при удалении проверять правильность ввода нет необходимости -
        //--- все равно удаляться будет по ранее загруженным данным
        checkInput(id, alFormData, alColumnList, hmColumnData, hmOut)

        //--- пред-обработка ( удаление файлов/картинок/проч. в серверных папках )
        preDelete(id)

        for (column in alColumnList) {
            if (column == model.columnId) continue // свое id-поле пропускаем, т.к. оно задается особым образом
            if (column.isVirtual) continue  // предочистка виртуальных полей не нужна
            if (column.columnTableName == model.modelTableName) {
                val data = hmColumnData[column]!!
                data.preDelete(application.rootDirName, conn)
            }
        }

        conn.executeUpdate(" DELETE FROM ${model.modelTableName} WHERE ${model.columnId.getFieldName()} = $id ")

        deleteIsReaded(id, true)
        postDelete(id, hmColumnData)

        return chmSession[AppParameter.REFERER + hmParam[AppParameter.REFERER]] as String
    }

    protected open fun preDelete(id: Int) {}

    protected open fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        //--- замены/удаления в бизнес-таблицах
        for (dd in model.alDependData) {
            when (dd.type) {
                DependData.DELETE -> conn.executeUpdate(" DELETE FROM ${dd.destTableName} WHERE ${dd.destFieldName} = $id ")
                DependData.SET -> conn.executeUpdate(" UPDATE ${dd.destTableName} SET ${dd.destFieldName} = ${dd.valueForSet} WHERE ${dd.destFieldName} = $id ")
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun addIsReaded(id: Int) {
        if (aliasConfig.isNewable) {
            conn.executeUpdate(" INSERT INTO SYSTEM_new ( table_name, row_id, user_id ) VALUES ( '${model.modelTableName}' , $id , ${userConfig.userId} ) ")
        }
    }

    protected fun deleteIsReaded(id: Int, deleteAll: Boolean) {
        if (aliasConfig.isNewable) {
            conn.executeUpdate(
                " DELETE FROM SYSTEM_new WHERE table_name = '${model.modelTableName}' AND row_id = $id " +
                    if (deleteAll) "" else " AND user_id <> ${userConfig.userId}"
            )
        }
    }

}
