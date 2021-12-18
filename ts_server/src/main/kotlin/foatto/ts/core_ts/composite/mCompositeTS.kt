package foatto.ts.core_ts.composite

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.mShow

class mCompositeTS : mShow() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {
        useLastTimeOnly = true
        
        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)
    }
}