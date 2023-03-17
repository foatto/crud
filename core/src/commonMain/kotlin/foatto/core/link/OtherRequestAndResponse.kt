package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class GetFileRequest(val altServerDirName: String?, val fullFileName: String)
@Serializable
class GetFileResponse(val fileData: ByteArray?)

@Serializable
class PutFileRequest(val fullFileName: String, val fileData: ByteArray)
@Serializable
class PutFileResponse

//--- used in replication utilities (not JS), so returning mutableListOf is not dangerous
@Serializable
class GetReplicationRequest(val destName: String, val prevTimeKey: Long)
@Serializable
class GetReplicationResponse(val dialect: String) {
    var timeKey: Long = -1
    val alSQL = mutableListOf<String>()
}

@Serializable
class PutReplicationRequest(
    val destName: String,
    val sourName: String,
    val sourDialect: String,
    val timeKey: Long,
    val alSQL: List<String>
)

@Serializable
class PutReplicationResponse(val timeKey: Long)

@Serializable
class SaveUserPropertyRequest(val name: String, val value: String) {
    var sessionId: Long = 0
}

@Serializable
class SaveUserPropertyResponse

@Serializable
class ChangePasswordRequest(val password: String) {
    var sessionId: Long = 0
}

@Serializable
class ChangePasswordResponse

@Serializable
class LogoffRequest {
    var sessionId: Long = 0
}

@Serializable
class LogoffResponse

@Serializable
class FormFileUploadResponse

@Serializable
class CustomRequest(
    val command: String,
    val hmData: Map<String, String> = emptyMap()
)

@Serializable
class CustomResponse