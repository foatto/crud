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
import foatto.mms.core_mms.sensor.config.SensorConfigEnergoAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigEnergoSummary
import foatto.mms.iMMSApplication
import foatto.spring.CoreSpringApp

class cShowCompositeEnergo : cAbstractForm() {

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

        val blocks = mutableListOf<CompositeBlockDTO>()

        //!!! пока только первый попавшийся датчик из всей толпы
        (oc.hmSensorConfig[SensorConfig.SENSOR_ENERGO_COUNT_AD]?.firstNotNullOf { it }?.value as? SensorConfigEnergoSummary)?.let { sces ->
            blocks += CompositeBlockDTO(x = 0, y = 0, w = 1, h = 1, xyResponse = getEnergoCountXyResponse(sces, hmOut))
        }
        (oc.hmSensorConfig[SensorConfig.SENSOR_ENERGO_COUNT_AR]?.firstNotNullOf { it }?.value as? SensorConfigEnergoSummary)?.let { sces ->
            blocks += CompositeBlockDTO(x = 0, y = 1, w = 1, h = 1, xyResponse = getEnergoCountXyResponse(sces, hmOut))
        }
        (oc.hmSensorConfig[SensorConfig.SENSOR_ENERGO_COUNT_RD]?.firstNotNullOf { it }?.value as? SensorConfigEnergoSummary)?.let { sces ->
            blocks += CompositeBlockDTO(x = 0, y = 2, w = 1, h = 1, xyResponse = getEnergoCountXyResponse(sces, hmOut))
        }
        (oc.hmSensorConfig[SensorConfig.SENSOR_ENERGO_COUNT_RR]?.firstNotNullOf { it }?.value as? SensorConfigEnergoSummary)?.let { sces ->
            blocks += CompositeBlockDTO(x = 0, y = 3, w = 1, h = 1, xyResponse = getEnergoCountXyResponse(sces, hmOut))
        }

        listOf(
            SensorConfig.SENSOR_ENERGO_VOLTAGE,
            SensorConfig.SENSOR_ENERGO_CURRENT,
            SensorConfig.SENSOR_ENERGO_POWER_KOEF,
            SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
            SensorConfig.SENSOR_ENERGO_POWER_FULL,
        ).forEachIndexed { index, sensorType ->
//            for (sc in oc.hmSensorConfig[sensorType]?.values ?: emptyList()) {
            //!!! пока только первый попавшийся датчик из всей толпы
            oc.hmSensorConfig[sensorType]?.values?.firstOrNull()?.let { sc ->
                if (sc is SensorConfigEnergoAnalogue) {
                    blocks += CompositeBlockDTO(x = index + 1, y = sc.phase, w = 1, h = 1, xyResponse = getEnergoAnalogueXyResponse(sc, hmOut))
                }
            }
        }

        val compositeResponseId = getRandomInt()
        hmOut[AppParameter.COMPOSITE_RESPONSE_ID + compositeResponseId] = CompositeResponse(
            title = title,
            tooltip = tooltip,
            alServerActionButton = emptyList(),
            cols = 7,
            rows = 4,
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

    private fun getEnergoCountXyResponse(
        sces: SensorConfigEnergoSummary,
        hmOut: MutableMap<String, Any>,
    ) = XyResponse(
        documentConfig = CoreSpringApp.hmXyDocumentConfig["mms_scheme_text"]!!,
        startParamId = XyStartData(
            alStartObjectData = mutableListOf(
                XyStartObjectData(
                    objectId = sces.id,
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

    private fun getEnergoAnalogueXyResponse(
        scea: SensorConfigEnergoAnalogue,
        hmOut: MutableMap<String, Any>,
    ) = XyResponse(
        documentConfig = CoreSpringApp.hmXyDocumentConfig["mms_scheme_analogue_indicator"]!!,
        startParamId = XyStartData(
            alStartObjectData = mutableListOf(
                XyStartObjectData(
                    objectId = scea.id,
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
