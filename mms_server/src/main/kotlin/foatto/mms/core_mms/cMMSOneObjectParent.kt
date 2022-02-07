package foatto.mms.core_mms

import foatto.core.link.FormResponse
import foatto.core.link.TableResponse
import foatto.core_server.app.server.cStandart
import foatto.mms.iMMSApplication

open class cMMSOneObjectParent : cStandart() {

    protected var oc: ObjectConfig? = null

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        getParentId("mms_object")?.let { objectId ->
            oc = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
        }
        return super.getTable(hmOut)
    }

    override fun getForm(hmOut: MutableMap<String, Any>): FormResponse {
        getParentId("mms_object")?.let { objectId ->
            oc = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
        }
        return super.getForm(hmOut)
    }

    override fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        var s = aliasConfig.descr
        oc?.let { oc ->
            var sGD = ""
            if (oc.groupName.isNotEmpty()) sGD += oc.groupName
            if (oc.departmentName.isNotEmpty()) sGD += (if (sGD.isEmpty()) "" else ", ") + oc.departmentName

            s += ": ${oc.name}"
            if (oc.model.isNotEmpty()) s += ", ${oc.model}"
            if (sGD.isNotEmpty()) s += " ($sGD)"
        }

        alPath.add(Pair("", s))
    }
}
