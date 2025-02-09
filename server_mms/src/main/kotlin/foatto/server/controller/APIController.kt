package foatto.server.controller

import foatto.server.model.ObjectDataRequest
import foatto.server.model.ObjectDataResponse
import foatto.server.service.APIService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class APIController(
    private val apiService: APIService,
) {

    companion object {
        private const val URL_API_BASE = "ext"
        private const val URL_API_VERSION = "v1"

        //        private const val URL_DEVICES = "devices"
//        private const val URL_DEVICES_DETAIL = "devices_detail"
        private const val URL_OBJECT_DATA = "object_data"
    }

    @PostMapping(value = ["/$URL_API_BASE/$URL_OBJECT_DATA/$URL_API_VERSION"])
    @Transactional
    fun getObjectData(
        @RequestBody
        request: ObjectDataRequest,
    ): ObjectDataResponse = apiService.getObjectData(
        token = request.token,
        objectName = request.name,
        start = request.start,
        duration = request.duration,
    )
}
