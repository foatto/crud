//package foatto.core_server.service
//
//import foatto.core.app.iCoreAppContainer
//import foatto.core.app.xy.config.XyBitmapType
//import foatto.core.link.AppLink
//import foatto.core.link.GetFileRequest
//import foatto.core.link.PutFileRequest
//import foatto.core.util.AdvancedByteBuffer
//import foatto.core.util.AdvancedLogger
//import foatto.core.util.getRandomInt
//import foatto.core_server.app.xy.server.document.sdcXyMap
//import kotlinx.coroutines.runBlocking
//import java.io.*
//import java.net.HttpURLConnection
//import java.net.InetSocketAddress
//import java.net.Proxy
//import java.net.URL
//import java.util.*
//
//abstract class CoreBitmapLoader(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {
//
//    companion object {
//
//        private val CONFIG_SERVER_IP = "bitmap_server_ip"
//        private val CONFIG_SERVER_PORT = "bitmap_server_port"
//
//        private val CONFIG_PROXY_ADDR = "proxy_addr"
//        private val CONFIG_PROXY_PORT = "proxy_port"
//        private val CONFIG_PROXY_USER = "proxy_user"
//        private val CONFIG_PROXY_PASS = "proxy_pass"
//
//        private val CONFIG_MAP_NAME_ = "bitmap_name_"
//        private val CONFIG_MAP_URL_ = "bitmap_url_"
//        private val CONFIG_MAP_REF_ = "bitmap_ref_"
//
//        private val CONFIG_USER_AGENT_ = "bitmap_user_agent_"
//
//        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//        val hsBlockedFileSize = mutableSetOf( 2046L ) // картинка "Server is busy" от MapSurfer
//    }
//
//    //--- параметры http-прокси при необходимости
//    protected var proxyAddr: String = ""
//    protected var proxyPort: String = ""
//    protected var proxyUser: String = ""
//    protected var proxyPass: String = ""
//
//    //--- центральный сервер битмапов (т.е. мы), с которым можно и нужно обмениваться картинками
//    protected var appLink: AppLink? = null
//
//    //--- префиксы запросов к серверам
//    private val hmMapServerURLPrefix = mutableMapOf<String, String>()
//    //--- referer запросов к серверам
//    private val hmMapServerReferer = mutableMapOf<String, String>()
//    //--- список различных вариантов значений User-Agent
//    private val alUserAgent = mutableListOf<String>()
//
//    override fun loadConfig() {
//        super.loadConfig()
//
//        //--- общие настройки http-прокси - для загрузки с картографических серверов и для связи с главным пульсар-сервером
//        proxyAddr = hmConfig[CONFIG_PROXY_ADDR] ?: ""
//        proxyPort = hmConfig[CONFIG_PROXY_PORT] ?: ""
//        proxyUser = hmConfig[CONFIG_PROXY_USER] ?: ""
//        proxyPass = hmConfig[CONFIG_PROXY_PASS] ?: ""
//
//        //--- загрузка/выгрузка картинок - специфическая функция, и чтобы не засорять хранилище сессий,
//        //--- установим sessionId в "как бы служебное" значение 0
//        if(hmConfig[CONFIG_SERVER_IP] != null) {
//            appLink = AppLink( 0 )
//            appLink!!.addServer( hmConfig[ CONFIG_SERVER_IP ]!!,
//                                 hmConfig[ CONFIG_SERVER_PORT ]!!.toInt() )
//            appLink!!.setProxy( proxyAddr, proxyPort, proxyUser, proxyPass )
//        }
//        var gBitmapTypeCount = 0
//        while(true) {
//            val bitmapTypeName = hmConfig[CONFIG_MAP_NAME_ + gBitmapTypeCount] ?: break
//
//            hmMapServerURLPrefix[bitmapTypeName] = hmConfig[CONFIG_MAP_URL_ + gBitmapTypeCount]!!
//            hmMapServerReferer[bitmapTypeName] = hmConfig[CONFIG_MAP_REF_ + gBitmapTypeCount]!!
//
//            gBitmapTypeCount++
//        }
//
//        var userAgentCount = 0
//        while(true) {
//            val userAgent = hmConfig[CONFIG_USER_AGENT_ + userAgentCount] ?: break
//
//            alUserAgent.add(userAgent)
//
//            userAgentCount++
//        }
//    }
//
//    override fun cycle() {
//        val dir = File(rootDirName, sdcXyMap.BITMAP_LOADER_JOB_DIR)
//        if(!dir.exists()) return
//
//        //--- сначала ищем самое свежее из срочных заданий
//        val newestFile: File? = dir.listFiles { _, name -> name.startsWith("0_") }.filter{ it.isFile }.maxBy { it.lastModified() } ?:
//        //--- если таковых не обнаружилось - берём ОДНО любое (несрочное) задание
//                                dir.listFiles().first { it.isFile } ?:
//        //--- папка с заданиями пуста
//                                return
//
//        val st = StringTokenizer(newestFile!!.name, "_")
//
//        st.nextToken() // пропускаем ненужный здесь priority
//        val bmTypeName = st.nextToken()
//
//        val bbData = AdvancedByteBuffer(newestFile.length().toInt())
//        val fileChannel = FileInputStream(newestFile).channel
//        fileChannel.read(bbData.buffer)
//        fileChannel.close()
//
//        bbData.flip()
//
//        while(bbData.hasRemaining()) loadBitmap(bmTypeName, bbData.getInt(), bbData.getInt(), bbData.getInt())
//
//        newestFile.delete()
//    }
//
//    protected fun loadBitmap(bmTypeName: String, bmZoomLevel: Int, bmBlockX: Int, bmBlockY: Int) {
//        //--- сразу пропускаем неизвестные или неподдерживаемые типы карт
//        if(!hmMapServerURLPrefix.containsKey(bmTypeName)) return
//
//        val bmPath = File( "$rootDirName${XyBitmapType.BITMAP_DIR}$bmTypeName/$bmZoomLevel/$bmBlockY" )
//        val bmFile = File( bmPath, "$bmBlockX.${XyBitmapType.BITMAP_EXT}" )
//
//        //--- если файл уже существует и он не очень старый, то пропускаем его генерацию
//        //--- (несмотря на кэширование имен обработанных битмапов - ситуация частая, т.к. сервер мог быть перезагружен,
//        //--- а при укладке заданий в очередь для экономии времени существование файлов не проверяется)
//        if(bmFile.exists() && System.currentTimeMillis() - bmFile.lastModified() < iCoreAppContainer.MAP_REFRESH_PERIOD) return
//
//        bmPath.mkdirs()
//        //--- пригодится для общения с сервером
//        val bitmapName = "${XyBitmapType.BITMAP_DIR}$bmTypeName/$bmZoomLevel/$bmBlockY/$bmBlockX.${XyBitmapType.BITMAP_EXT}"
//        //--- если центральный сервер задан, попробуем взять готовый битмап оттуда
//        var isSuccess = false
//        if(appLink != null) {
//            runBlocking {
//                try {
//                    val response = appLink!!.invokeGetFile( GetFileRequest( null, bitmapName ) )
//                    if( response.fileData != null ) {
//                        //--- во избежание сохранения частично принятых файлов сначала грузим их во временный файл
//                        val fos = FileOutputStream( bmFile )
//                        fos.write( response.fileData )
//                        fos.close()
//
//                        AdvancedLogger.debug("BITMAP: $bitmapName loaded from root server.")
//                        isSuccess = true
//                    }
//                    else AdvancedLogger.debug("BITMAP: $bitmapName not found on root server.")
//                }
//                catch(t: Throwable) {
//                    //t.printStackTrace();
//                    AdvancedLogger.debug("BITMAP: $bitmapName error at loading from root server.")
//                }
//            }
//        }
//        if( isSuccess ) return
//
//        //--- получение имени/урла исходного файла-блока
//        val sbMapURL = StringBuilder(hmMapServerURLPrefix[bmTypeName])
//        val sbMapBlockURL = StringBuilder()
//        when(bmTypeName) {
//            //--- убираем нафиг работу со всеми юридически нечистыми битмап-серверами
//            //        case XyBitmapType.GS:
//            //            //--- специфично для гугл-сат
//            //            sbMapURL.insert( 11, Integer.toString( CommonFunction.getRandomInt() % 4 ) );
//            //
//            //            sbMapBlockURL.append( "&x=" ).append( bmBlockX ).append( "&y=" ).append( bmBlockY )
//            //                       .append( "&z=" ).append( bmZoomLevel )
//            //                       .append( "&s=" ).append( "Galileo".substring( 0, ( bmBlockX * 3 + bmBlockY ) % 8 ) );
//            //            //--- в новой версии можно .append( "Galileo".substring( 0, random( 8 ) ); - хотя результат тот же
//            //            break;
//            //--- убираем нафиг работу со всеми юридически нечистыми битмап-серверами
//            //        case XyBitmapType.GM:
//            //            //--- специфично для гугл-мап
//            //            sbMapURL.insert( 9, Integer.toString( CommonFunction.getRandomInt() % 4 ) );
//            //
//            //            sbMapBlockURL.append( "&x=" ).append( bmBlockX ).append( "&y=" ).append( bmBlockY )
//            //                       .append( "&zoom=" ).append( bmZoomLevel )
//            //                       .append( "&s=" ).append( "Galileo".substring( 0, ( bmBlockX * 3 + bmBlockY ) % 8 ) );
//            //            //--- в новой версии можно .append( "Galileo".substring( 0, random( 8 ) ); - хотя результат тот же
//            //            break;
//            XyBitmapType.MN -> {
//                //--- специфично для MAPNIK
//                val arrPrefixOSM = charArrayOf('a', 'b', 'c')
//                sbMapURL.setCharAt(7, arrPrefixOSM[getRandomInt() % 3])
//
//                sbMapBlockURL.append('/').append(bmZoomLevel).append('/').append(bmBlockX).append('/').append(bmBlockY).append(".png")
//            }
//            XyBitmapType.MS ->
//                //--- специфично для MapSurfer.net
//                sbMapBlockURL.append("x=").append(bmBlockX).append("&y=").append(bmBlockY).append("&z=").append(bmZoomLevel)
//            //--- убираем нафиг работу со всеми юридически нечистыми битмап-серверами
//            //        case XyBitmapType.WM:
//            //            //--- специфично для викимапии
//            //            sbMapURL.insert( 8, Integer.toString( bmBlockX % 4 + ( bmBlockY % 4 ) * 4 ) );
//            //
//            //            sbMapBlockURL.append( "&x=" ).append( bmBlockX ).append( "&y=" ).append( bmBlockY ).append( "&zoom=" ).append( bmZoomLevel );
//            //            break;
//            //--- дополнительная проверка на неизвестные/неподдерживаемые типа карт
//            else -> return
//        }
//        sbMapURL.append(sbMapBlockURL)
//
//        try {
//            val url = URL(sbMapURL.toString())
//            val urlConn: HttpURLConnection
//
//            if(proxyAddr.isEmpty()) urlConn = url.openConnection() as HttpURLConnection
//            else {
//                urlConn = url.openConnection(
//                    Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyAddr, Integer.parseInt(proxyPort)))
//                ) as HttpURLConnection
//                if( proxyUser.isNotEmpty() && proxyPass.isNotEmpty())
//                    urlConn.setRequestProperty(
//                        "Proxy-Authorization", StringBuilder("Basic ").append(
//                            Base64.getEncoder().encodeToString(
//                                StringBuilder(proxyUser).append(':').append(proxyPass).toString().toByteArray()
//                            )
//                        ).toString()
//                    )
//            }
//
//            //--- настройка подключения
//            urlConn.requestMethod = "GET"
//            urlConn.allowUserInteraction = false
//            urlConn.connectTimeout = 60000
//            urlConn.doOutput = false
//            urlConn.doInput = true
//            urlConn.useCaches = false
//            urlConn.setRequestProperty("Content-Type", "application/octet-stream")
//            urlConn.setRequestProperty("Referer", hmMapServerReferer[bmTypeName])
//            urlConn.setRequestProperty("User-Agent", alUserAgent[getRandomInt() % alUserAgent.size])
//            //--- так и осталось неясным - вызывать ли явно метод connect или он где-то автоматом вызывается:
//            //--- часть примеров с ним, часть без него.
//            //--- но сейчас работает и без его вызова.
//            //urlConn.connect();
//            val gis = urlConn.inputStream
//
//            //--- зависит от платформы
//            workBitmap(gis, bmFile, sbMapURL, bitmapName)
//        }
//        catch(ioe: IOException) {
//            AdvancedLogger.debug("BITMAP: $sbMapURL can't load from geoserver.")
//            AdvancedLogger.debug(ioe)
//        }
//        //--- здесь проверяем именно IOException, т.е. это общее для:
//        //--- FileNotFoundException от моего открытия урла и
//        //--- IIOException от ImageIO.read
//    }
//
//    protected abstract fun workBitmap(gis: InputStream, bmFile: File, sbMapURL: StringBuilder, bitmapName: String)
//
//    protected fun sendBitmap(bitmapName: String, bmFile: File) {
//        runBlocking {
//            try {
//                appLink!!.invokePutFile( PutFileRequest( bitmapName, FileInputStream( bmFile ).readAllBytes() ) )
//                AdvancedLogger.debug( "BITMAP: $bitmapName sended to root server." )
//            }
//            catch(t: Throwable) {
//                //t.printStackTrace();
//            AdvancedLogger.debug( "BITMAP: $bitmapName can't send to root server." )
//            }
//        }
//    }
//
//}
