package foatto.ts.core_ts

import foatto.core.link.FormResponse
import foatto.core.link.TableResponse
import foatto.core_server.app.server.cStandart
import foatto.ts.iTSApplication

open class cTSOneObjectParent : cStandart() {

    protected var oc: ObjectConfig? = null

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        hmParentData["ts_object"]?.let { objectId ->
            oc = (application as iTSApplication).getObjectConfig(userConfig, objectId)
        }
        return super.getTable(hmOut)
    }

    override fun getForm(hmOut: MutableMap<String, Any>): FormResponse {
        hmParentData["ts_object"]?.let { objectId ->
            oc = (application as iTSApplication).getObjectConfig(userConfig, objectId)
        }
        return super.getForm(hmOut)
    }

    override fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        var s = aliasConfig.descr
        oc?.let { oc ->
            s += ": ${oc.name}"
            if (oc.model.isNotEmpty()) s += ", ${oc.model}"
        }

        alPath.add(Pair("", s))
    }
}
