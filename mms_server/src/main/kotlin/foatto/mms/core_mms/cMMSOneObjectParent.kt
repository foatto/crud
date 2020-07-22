package foatto.mms.core_mms

import foatto.core.link.FormResponse
import foatto.core.link.TableResponse
import foatto.core_server.app.server.cStandart

open class cMMSOneObjectParent : cStandart() {

    protected var oc: ObjectConfig? = null

    override fun getTable( hmOut: MutableMap<String, Any> ): TableResponse {
        //--- может быть null при вызове из "Модули системы"
        val objectID = hmParentData[ "mms_object" ]
        if( objectID != null ) oc = ObjectConfig.getObjectConfig( stm, userConfig, objectID )

        return super.getTable( hmOut )
    }

    override fun getForm( hmOut: MutableMap<String, Any> ): FormResponse {
        //--- может быть null при вызове из "Модули системы"
        val objectID = hmParentData[ "mms_object" ]
        //!!! непонятно накой нужно???
        //        if( objectID == null ) objectID = 0;
        if( objectID != null ) oc = ObjectConfig.getObjectConfig( stm, userConfig, objectID )

        return super.getForm( hmOut )
    }

    override fun fillHeader( selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String,String>>, hmOut: MutableMap<String, Any> ) {
        var s = aliasConfig.descr
        if( oc != null ) {
            var sGD = ""
            if( !oc!!.groupName.isEmpty() ) sGD += oc!!.groupName
            if( !oc!!.departmentName.isEmpty() ) sGD += ( if( sGD.isEmpty() ) "" else ", " ) + oc!!.departmentName

            s += ": ${oc!!.name}"
            if( !oc!!.model.isEmpty() ) s += ", ${oc!!.model}"
            if( !sGD.isEmpty() ) s += " ($sGD)"
        }

        alPath.add( Pair( "", s ) )
    }
}
