package foatto.server.controller

import foatto.server.ds.PulsarData
import foatto.server.service.MMSPulsarDataService
import foatto.server.util.AdvancedLogger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MMSPulsarDataController(
    private val mmsPulsarDataService: MMSPulsarDataService,
) {
    //--- /{fileName} - на время Юриной отладки
    @PostMapping(value = ["/data/pulsar/{fileName}"])
    @Transactional
    fun getPulsarData(
        @PathVariable("fileName")
        fileName: String,
        @RequestBody
        arrData: Array<PulsarData>,
        request: HttpServletRequest,
    ) {
        AdvancedLogger.debug(
            message = arrData.joinToString(separator = "\n") { (dateTime, deviceID, blockID, idx, vals) ->
                "dateTime = ${dateTime}\n" +
                    "deviceID = ${deviceID}\n" +
                    "blockID = ${blockID}\n" +
                    "idx = ${idx}\n" +
                    vals?.joinToString(separator = "\n") { valuesMap ->
                        valuesMap.entries.joinToString(separator = "\n")
                    }
            },
            subDir = arrData.firstOrNull()?.deviceID ?: "0"
        )

        mmsPulsarDataService.getPulsarData(
            request = request,
            arrData = arrData,
        )
    }

}
