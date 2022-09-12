package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.sql.CoreAdvancedConnection

class mZone : mAbstractUserSelector() {

    lateinit var columnZoneName: ColumnString
        private set
    lateinit var columnZoneDescr: ColumnString
        private set

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_zone"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        columnZoneName = ColumnString(modelTableName, "name", "Наименование геозоны", STRING_COLUMN_WIDTH)
        columnZoneName.isRequired = true
        //columnZoneName.setUnique( true ); !!! у разных корпоративных клиентов могут быть совпадающие значения
        columnZoneDescr = ColumnString(modelTableName, "descr", "Описание геозоны", STRING_COLUMN_WIDTH)
        val columnZoneOuterID = ColumnString(modelTableName, "outer_id", "Внешний идентификатор", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnZoneName)
        addTableColumn(columnZoneDescr)
        addTableColumn(columnZoneOuterID)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnUserName)
        alFormColumn.add(columnZoneName)
        alFormColumn.add(columnZoneDescr)
        alFormColumn.add(columnZoneOuterID)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnZoneName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_show_zone", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("mms_user_zone", columnId))
        alChildData.add(ChildData("mms_object_zone", columnId))
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = false, newGroup = true)
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = false)
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_user_zone", "zone_id"))
        alDependData.add(DependData("MMS_object_zone", "zone_id"))
        //alDependData.add( new DependData( "MMS_route_zone", "zone_id" ) );
    }

}
