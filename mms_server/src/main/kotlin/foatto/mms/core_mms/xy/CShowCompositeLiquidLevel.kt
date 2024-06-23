package foatto.mms.core_mms.xy

import foatto.core.app.ICON_NAME_STATE
import foatto.core.app.composite.CompositeBlockDTO
import foatto.core.app.composite.CompositeResponse
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.link.XyResponse
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.iMMSApplication
import foatto.spring.CoreSpringApp
import kotlin.math.ceil
import kotlin.math.sqrt

class cShowCompositeLiquidLevel : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_STATE

    override fun isFormAutoClick() = if (getParentId("mms_object") != null) {
        true
    } else {
        super.isFormAutoClick()
    }

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val mss = model as mShowState

        //--- выборка данных параметров для отчета
        val selectObjectId = (hmColumnData[mss.columnObject] as DataInt).intValue

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, selectObjectId)

//        val csd = CompositeStartData()
//        csd.objectId = selectObjectId
//        csd.rangeType = 86_400  // на случай графиков - показываем последние сутки

        var title = aliasConfig.descr + "\n${oc.name}"
        if (oc.model.isNotEmpty()) {
            title += ", ${oc.model}"
        }

        var tooltip = oc.name
        if (oc.model.isNotEmpty()) {
            tooltip += "\nМодель: ${oc.model}"
        }

        //--- compose part

        val sensorCount = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.size ?: 0
        //--- в соотношении 2:1
        val blockRows = ceil(sqrt(sensorCount / 2.0)).toInt()
        val blockOnRow = blockRows * 2

        val blocks = mutableListOf<CompositeBlockDTO>()
        var row = 0
        var col = 0

        oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
            val scll = sc as SensorConfigLiquidLevel
            blocks += CompositeBlockDTO(x = col, y = row, w = 1, h = 1, xyResponse = getLiquidLevelAnalogueXyResponse(scll, hmOut))

            col++
            if (col >= blockOnRow) {
                row++
                col = 0
            }
        }

        val compositeResponseId = getRandomInt()
        hmOut[AppParameter.COMPOSITE_RESPONSE_ID + compositeResponseId] = CompositeResponse(
            title = title,
            tooltip = tooltip,
            alServerActionButton = emptyList(),
            cols = blockOnRow,
            rows = blockRows,
            blocks = blocks,
        )

        return getParamURL(
            aAlias = aliasConfig.name,  // не имеет значения
            aAction = AppAction.COMPOSITE,
            aRefererId = null,
            aId = null,
            aParentData = null,
            aParentUserId = null,
            aAltParams = "&${AppParameter.COMPOSITE_RESPONSE_ID}=$compositeResponseId"
        )
    }

    private fun getLiquidLevelAnalogueXyResponse(
        scll: SensorConfigLiquidLevel,
        hmOut: MutableMap<String, Any>,
    ) = XyResponse(
        documentConfig = CoreSpringApp.hmXyDocumentConfig["mms_scheme_analogue_indicator"]!!,
        startParamId = XyStartData(
            alStartObjectData = mutableListOf(
                XyStartObjectData(
                    objectId = scll.id,
                    typeName = "mms_sensor",
                    isStart = true,
                    isTimed = false,
                    isReadOnly = true
                )
            ),
        ).let { xyStartData ->
            val xyParamId = getRandomInt().toString()
            hmOut[AppParameter.XY_START_DATA + xyParamId] = xyStartData
            xyParamId
        }
    )
}
