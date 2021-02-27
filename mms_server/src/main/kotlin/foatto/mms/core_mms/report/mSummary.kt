package foatto.mms.core_mms.report

import foatto.core_server.app.server.UserConfig

class mSummary : mUODGP() {

    lateinit var sros: SummaryReportOptionSelector
        private set

    lateinit var sos: SumOptionSelector
        private set

    override fun addOptionsColumns(userConfig: UserConfig) {
        super.addOptionsColumns(userConfig)

        sros = SummaryReportOptionSelector()
        sros.fillColumns(userConfig, tableName, alFormColumn)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, tableName, alFormColumn)
    }
}
