//package foatto.core.app.video
//
//import foatto.core.util.AdvancedByteBuffer
//import java.io.InputStream
//import java.net.HttpURLConnection
//import java.net.URL
//import java.util.*
//
//class CameraDef {
//
//    //--- общие/одинаковые для всех определения (удобно задать их здесь для каждой камеры)
//    var dirVideoRoot: String
//    var objectId = 0
//
//    //--- общее для всех режимов, но для каждой камеры своё
//    var descr: String
//
//    //--- только для онлайн-режима
//    var imageURL: String
//    var login: String
//    var password: String
//
//    //--- только для показа архива
//    var alStream: ArrayList<StreamDef>
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    constructor( aDirVideoRoot: String, aobjectId: Int, aDescr: String, aImageURL: String, aLogin: String, aPassword: String ) {
//        dirVideoRoot = aDirVideoRoot
//        objectId = aobjectId
//
//        descr = aDescr
//
//        imageURL = aImageURL
//        login = aLogin
//        password = aPassword
//
//        alStream = ArrayList( 0 )
//    }
//
//    constructor( aDirVideoRoot: String, aobjectId: Int, aDescr: String, streamCount: Int ) {
//        dirVideoRoot = aDirVideoRoot
//        objectId = aobjectId
//
//        descr = aDescr
//
//        imageURL = ""
//        login = ""
//        password = ""
//
//        alStream = ArrayList( streamCount )
//    }
//
//    constructor( bbIn: AdvancedByteBuffer ) {
//        dirVideoRoot = bbIn.getShortString()
//        objectId = bbIn.getInt()
//
//        descr = bbIn.getShortString()
//
//        imageURL = bbIn.getShortString()
//        login = bbIn.getShortString()
//        password = bbIn.getShortString()
//
//        val count = bbIn.getInt()
//        alStream = ArrayList(count)
//        for( i in 0 until count ) alStream.add( StreamDef( bbIn ) )
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun write( bbOut: AdvancedByteBuffer ) {
//        bbOut.putShortString( dirVideoRoot )
//        bbOut.putInt( objectId )
//
//        bbOut.putShortString( descr )
//
//        bbOut.putShortString( imageURL )
//        bbOut.putShortString( login )
//        bbOut.putShortString( password )
//
//        bbOut.putInt( alStream.size )
//        for( sd in alStream ) sd.write( bbOut )
//    }
//
//    fun getCameraImageInputStream() = getCameraImageInputStream( imageURL, login, password )
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    companion object {
//
//        fun getCameraImageInputStream( urlImage: String, cameraLogin: String, cameraPassword: String ): InputStream {
//            val urlConn = URL( urlImage ).openConnection() as HttpURLConnection
//            //--- настройка подключения
//            urlConn.requestMethod = "GET"
//            urlConn.allowUserInteraction = false
//            urlConn.connectTimeout = 1000
//            urlConn.doOutput = false
//            urlConn.doInput = true
//            urlConn.useCaches = false
//            //urlConn.setRequestProperty( "Content-type", "application/octet-stream" );
//            if( !cameraLogin.isEmpty() && !cameraPassword.isEmpty() )
//                urlConn.setRequestProperty( "Authorization",
//                    StringBuilder( "Basic " ).append( Base64.getEncoder().encodeToString( "$cameraLogin:$cameraPassword".toByteArray() ) ).toString() )
//            //--- так и осталось неясным - вызывать ли явно метод connect или он где-то автоматом вызывается:
//            //--- часть примеров с ним, часть без него.
//            //--- но сейчас работает и без его вызова.
//            //urlConn.connect();
//
//            return urlConn.inputStream
//        }
//    }
//}
