package foatto.ts.spring.controllers

import foatto.core.link.GetReplicationRequest
import foatto.core.link.GetReplicationResponse
import foatto.core.link.PutReplicationRequest
import foatto.core.link.PutReplicationResponse
import foatto.spring.controllers.CoreReplicationController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TSReplicationController : CoreReplicationController() {

    @PostMapping("/api/get_replication")
    override fun getReplication(
        @RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        return super.getReplication(getReplicationRequest)
    }

    @PostMapping("/api/put_replication")
    override fun putReplication(
        @RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        return super.putReplication(putReplicationRequest)
    }

}