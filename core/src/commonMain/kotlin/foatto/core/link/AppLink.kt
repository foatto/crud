//package foatto.core.link
//
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.cio.CIO
//import io.ktor.client.features.json.JacksonSerializer
//import io.ktor.client.features.json.JsonFeature
//import io.ktor.client.request.post
//import io.ktor.client.request.url
//import io.ktor.http.ContentType
//import io.ktor.http.contentType
//
//import java.net.URL
//
//class AppLink( private val sessionID: Long, hmSystemConfig: Map<String, String> = emptyMap(), hmLocalConfig: Map<String, String> = emptyMap() ) {
//
//    companion object {
//        const val CONFIG_SERVER_IP_ = "server_ip_"
//        const val CONFIG_SERVER_PORT_ = "server_port_"
//
//        const val CONFIG_PROXY_ADDR = "proxy_addr"
//        const val CONFIG_PROXY_PORT = "proxy_port"
//        const val CONFIG_PROXY_USER = "proxy_user"
//        const val CONFIG_PROXY_PASS = "proxy_pass"
//    }
//
//    private val alServer = mutableListOf<Pair<String,Int>>()
//
//    private var proxyAddr = ""
//    private var proxyPort = ""
//    private var proxyUser = ""
//    private var proxyPass = ""
//
//    init {
//        var index = 0
//        while( true ) {
//            val sServerIP = hmSystemConfig[ CONFIG_SERVER_IP_ + index ] ?: break
//            val sServerPort = hmSystemConfig[ CONFIG_SERVER_PORT_ + index ] ?: break
//            addServer(sServerIP, sServerPort.toInt() )
//
//            index++
//        }
//        setProxy( hmLocalConfig[ CONFIG_PROXY_ADDR ] ?: "",
//                  hmLocalConfig[ CONFIG_PROXY_PORT ] ?: "",
//                  hmLocalConfig[ CONFIG_PROXY_USER ] ?: "",
//                  hmLocalConfig[ CONFIG_PROXY_PASS ] ?: "" )
//    }
//
//    private var serverNo = 0                    // номер текущего сервера
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun addServer( ip: String, port: Int ) {
//        alServer.add( Pair( ip, port ) )
//    }
//
//    fun setProxy( addr: String, port: String, user: String, pass: String ) {
//        proxyAddr = addr
//        proxyPort = port
//        proxyUser = user
//        proxyPass = pass
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
////    suspend fun invokeApp( appRequest: AppRequest ): AppResponse {
////
////        appRequest.sessionID = sessionID
////
////
////
//////        val appRequest2 = AppRequest2( appRequest.action )
//////        appRequest2.action = appRequest.action
//////        appRequest2.find = appRequest.find
//////        appRequest2.sessionID = appRequest.sessionID
////
////
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<AppResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/app" ) )
////            contentType( ContentType.Application.Json )
////            body = appRequest
//////            body = appRequest2
////        }
////        client.close()
////
////        return response
////    }
////
////    suspend fun invokeGraphic( graphicActionRequest: GraphicActionRequest ): GraphicActionResponse {
////
////        graphicActionRequest.sessionID = sessionID
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<GraphicActionResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/graphic" ) )
////            contentType( ContentType.Application.Json )
////            body = graphicActionRequest
////        }
////        client.close()
////
////        return response
////    }
////
////    suspend fun invokeXy( xyActionRequest: XyActionRequest ): XyActionResponse {
////
////        xyActionRequest.sessionID = sessionID
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<XyActionResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/xy" ) )
////            contentType( ContentType.Application.Json )
////            body = xyActionRequest
////        }
////        client.close()
////
////        return response
////    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
////    suspend fun invokeUpdate( updateRequest: UpdateRequest ): UpdateResponse {
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<UpdateResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/update" ) )
////            contentType( ContentType.Application.Json )
////            body = updateRequest
////        }
////        client.close()
////
////        return response
////    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    suspend fun invokeGetFile( getFileRequest: GetFileRequest ): GetFileResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<GetFileResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/get_file" ) )
//            contentType( ContentType.Application.Json )
//            body = getFileRequest
//        }
//        client.close()
//
//        return response
//    }
//
//    suspend fun invokePutFile( putFileRequest: PutFileRequest ): PutFileResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<PutFileResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/put_file" ) )
//            contentType( ContentType.Application.Json )
//            body = putFileRequest
//        }
//        client.close()
//
//        return response
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    suspend fun invokeGetReplication( getReplicationRequest: GetReplicationRequest ): GetReplicationResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<GetReplicationResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/get_replication" ) )
//            contentType( ContentType.Application.Json )
//            body = getReplicationRequest
//        }
//        client.close()
//
//        return response
//    }
//
//    suspend fun invokePutReplication( putReplicationRequest: PutReplicationRequest ): PutReplicationResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<PutReplicationResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/put_replication" ) )
//            contentType( ContentType.Application.Json )
//            body = putReplicationRequest
//        }
//        client.close()
//
//        return response
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
////    suspend fun invokeSaveUserProperty( saveUserPropertyRequest: SaveUserPropertyRequest ): SaveUserPropertyResponse {
////
////        saveUserPropertyRequest.sessionID = sessionID
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<SaveUserPropertyResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/save_user_property" ) )
////            contentType( ContentType.Application.Json )
////            body = saveUserPropertyRequest
////        }
////        client.close()
////
////        return response
////    }
////
////    suspend fun invokeChangePassword( changePasswordRequest: ChangePasswordRequest ): ChangePasswordResponse {
////
////        changePasswordRequest.sessionID = sessionID
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<ChangePasswordResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/change_password" ) )
////            contentType( ContentType.Application.Json )
////            body = changePasswordRequest
////        }
////        client.close()
////
////        return response
////    }
////
////    suspend fun invokeLogoff( logoffRequest: LogoffRequest ): LogoffResponse {
////
////        logoffRequest.sessionID = sessionID
////
////        val client = HttpClient( CIO ) {
////            install( JsonFeature ) {
////                serializer = JacksonSerializer()
//////                serializer = KotlinxSerializer()
////            }
////        }
////
////        val ( serverIP, serverPort ) = alServer[ serverNo ]
////        val response = client.post<LogoffResponse> {
////            url( URL( "http://$serverIP:$serverPort/api/logoff" ) )
////            contentType( ContentType.Application.Json )
////            body = logoffRequest
////        }
////        client.close()
////
////        return response
////    }
//
//}