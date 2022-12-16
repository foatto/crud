package foatto.core.link

import kotlinx.serialization.Serializable

class GetFileRequest(val altServerDirName: String?, val fullFileName: String)
class GetFileResponse(val fileData: ByteArray?)

class PutFileRequest(val fullFileName: String, val fileData: ByteArray)
class PutFileResponse

//--- used in replication utilities (not JS), so returning mutableListOf is not dangerous
class GetReplicationRequest(val destName: String, val prevTimeKey: Long)
class GetReplicationResponse(val dialect: String) {
    var timeKey: Long = -1
    val alSQL = mutableListOf<String>()
}

class PutReplicationRequest(
    val destName: String,
    val sourName: String,
    val sourDialect: String,
    val timeKey: Long,
    val alSQL: List<String>
)

class PutReplicationResponse(val timeKey: Long)

class SaveUserPropertyRequest(val name: String, val value: String) {
    var sessionId: Long = 0
}

@Serializable
class SaveUserPropertyResponse

class ChangePasswordRequest(val password: String) {
    var sessionId: Long = 0
}

@Serializable
class ChangePasswordResponse

class LogoffRequest {
    var sessionId: Long = 0
}

@Serializable
class LogoffResponse

@Serializable
class FormFileUploadResponse

class CustomRequest(
    val command: String,
    val hmData: Map<String, String> = emptyMap()
)

@Serializable
class CustomResponse