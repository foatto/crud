package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig

class mSummaryBNGRE : mUODGP() {

    lateinit var sos: SumOptionSelector
        private set

    override fun addOptionsColumns(userConfig: UserConfig) {
        super.addOptionsColumns(userConfig)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, modelTableName, alFormColumn)
    }
}
