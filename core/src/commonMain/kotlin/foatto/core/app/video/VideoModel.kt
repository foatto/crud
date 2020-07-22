//@file:JvmName("VideoModel")
//package foatto.core.app.video
//
//import foatto.core.app.iCoreAppContainer
//import foatto.core.link.AppAction
//import foatto.core.link.AppLinkOld
//import foatto.core.util.AdvancedByteBuffer
//import java.util.ArrayList
//import java.util.TreeSet
//
//class VideoModel {
//
//    //--- основные режимы работы
//    enum class WorkMode {
//        PAUSE, PLAY /*, PAN, ZOOM_BOX*/ /*, LOAD*/
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private lateinit var appContainer: iCoreAppContainer
//    lateinit var appLink: AppLinkOld
//
//    //--- параметры, генерируемые и/или загружаемые с сервера
//    lateinit var startParamID: String
//    lateinit var startTitle: String
//    @JvmField var isOnlineMode = false
//
//    @JvmField var scaleKoef = 1
//    @JvmField var isLocalServer = false
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    //--- текущий режим работы
//    @JvmField var curMode = WorkMode.PAUSE
//    @JvmField var vcViewCoord = VideoViewCoord( 0, 0 )
//    @Volatile
//    @JvmField var curTime: Long = 0
//
//    //--- редко бывает больше 16 камер одновременно
//    @JvmField var alCameraDef = ArrayList<CameraDef>( 16 )
//
//    //--- режим показа по каждой камере: индекс - режим показа, внутри элемента массива - список индексов камер.
//    //--- (используется именно ArrayList, т.к. важен порядок индексов)
//    @JvmField var tsShowModeHidden = TreeSet<Int>()
//    @JvmField var tsShowModePreview = TreeSet<Int>()
//    @JvmField var tsShowModeFull = TreeSet<Int>()
//
//    //--- экранная позиция курсора
//    @JvmField var cursorX = -1
//    //--- экранные X-позиции рамки выбора области
//    @JvmField var selectorX1 = -1
//    @JvmField var selectorX2 = -1
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun setContainerParam( aAppContainer: iCoreAppContainer, aAppLink: AppLinkOld ) {
//        appContainer = aAppContainer
//        appLink = aAppLink
//
//        scaleKoef = appContainer.scaleKoef
//        isLocalServer = appLink.isLocalServer
//    }
//
//    fun setVideoParam( aStartParamID: String, aPrintTitle: String, aIsOnlineMode: Boolean ) {
//        startParamID = aStartParamID
//        startTitle = aPrintTitle
//        isOnlineMode = aIsOnlineMode
//        //--- при изменении не забудь поправить соответствующий writeGraphicParam
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun getCoordsParam(): AdvancedByteBuffer {
//        val bbParam = AdvancedByteBuffer( 1024 )
//
//        bbParam.putShortString( AppAction.VIDEO_ACTION )
//        bbParam.putShortString( VideoParameter.ACTION_GET_COORDS )
//        bbParam.putShortString( startParamID )
//
//        return bbParam
//    }
//
//    fun getCoordsDone( bbIn: AdvancedByteBuffer ) {
//        vcViewCoord.set( bbIn.getInt() * 1000L, bbIn.getInt() * 1000L )
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    //--- установка параметров для загрузки списка файлов
//    fun getFileListParam() : AdvancedByteBuffer {
//        val bbParam = AdvancedByteBuffer( 1024 )
//
//        bbParam.putShortString( AppAction.VIDEO_ACTION )
//        bbParam.putShortString( VideoParameter.ACTION_GET_FILE_LIST )
//        bbParam.putShortString( startParamID )
//
//        if( !isOnlineMode ) {
//            bbParam.putInt( ( vcViewCoord.t1 / 1000 ).toInt() )
//            bbParam.putInt( ( vcViewCoord.t2 / 1000 ).toInt() )
//        }
//        return bbParam
//    }
//
//    fun getFileListDone( bbIn: AdvancedByteBuffer ) {
//        alCameraDef.clear()
//
//        val count = bbIn.getInt()
//        for( i in 0 until count ) alCameraDef.add( CameraDef( bbIn ) )
//
//        //--- если (новое) кол-во камер не совпадает со старым определением их размещения,
//        //--- то определим его заново
//        if( tsShowModeHidden.size + tsShowModePreview.size + tsShowModeFull.size != alCameraDef.size ) {
//
//            tsShowModeHidden.clear()
//            tsShowModePreview.clear()
//            tsShowModeFull.clear()
//
//            //--- если камера одна - то в full режиме
//            if( alCameraDef.size == 1 ) tsShowModeFull.add( 0 )
//            //--- иначе в сетку квадратора
//            else for( i in alCameraDef.indices ) tsShowModePreview.add( i )
//        }
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    //--- установка параметров для загрузки списка файлов
//    fun getImageLoadParam(): AdvancedByteBuffer {
//        val bbParam = AdvancedByteBuffer( 1024 )
//
//        bbParam.putShortString( AppAction.VIDEO_ACTION )
//        bbParam.putShortString( VideoParameter.ACTION_LOAD_IMAGE )
//        bbParam.putShortString( startParamID )
//
//        return bbParam
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun isPlayEnabled(): Boolean {
//        var isFound = false
//
//        for( cIndex in alCameraDef.indices ) {
//            val cd = alCameraDef[ cIndex ]
//
//            if( tsShowModeHidden.contains( cIndex ) ) continue
//
//            val sIndex = getStreamIndex( cIndex )
//            val alFileEnd = cd.alStream.get( sIndex ).alFileEnd
//            if( !alFileEnd.isEmpty() && curTime < alFileEnd[ alFileEnd.size - 1 ] ) {
//                isFound = true
//                break
//            }
//        }
//        return isFound
//    }
//
//    fun getPlayDef(): PlayDef {
//        val pd = PlayDef( alCameraDef.size )
//
//        var isFound = false
//        //--- ищем видео на текущее время
//        for( cIndex in alCameraDef.indices ) {
//            //--- камеры, скрытые для показа, пропускаем
//            if( tsShowModeHidden.contains( cIndex ) ) continue
//
//            val cd = alCameraDef[ cIndex ]
//            //--- для режима мелкого просмотра используем второй поток видео, если он есть и не пустой
//            val sIndex = getStreamIndex( cIndex )
//
//            pd.arrStreamIndex[ cIndex ] = sIndex
//
//            val alFileBeg = cd.alStream[ sIndex ].alFileBeg
//            val alFileEnd = cd.alStream[ sIndex ].alFileEnd
//            for( i in alFileBeg.indices )
//                //--- если есть файл, попадающий в заданное время
//                if( curTime >= alFileBeg[ i ] && curTime < alFileEnd[ i ] ) {
//                    pd.arrCurFileIndex[ cIndex ] = i
//                    //--- если текущий файл не последний - сразу запомним следующий файл для проигрывания
//                    if( i + 1 < alFileBeg.size ) {
//                        pd.arrNextFileIndex[ cIndex ] = i + 1
//                        pd.minNextTime = Math.min( pd.minNextTime, alFileBeg[ i + 1 ] )
//                    }
//                    isFound = true
//                    break
//                }
//                //--- иначе как следующий - берём первый файл, находящийся за заданным временем
//                else if( alFileBeg[ i ] > curTime ) {
//                    pd.arrNextFileIndex[ cIndex ] = i
//                    pd.minNextTime = Math.min( pd.minNextTime, alFileBeg[ i ] )
//                    break
//                }
//        }
//        if( isFound ) return pd
//        //--- если ни одного файла для заданного времени не найдено
//        else {
//            //--- переводим время к ближайшему следующему файлу
//            //--- (переводить sldTime не будем, т.к. все равно он будет перещёлкиваться в процессе воспроизведения)
//            curTime = pd.minNextTime
//            //--- и вызываем себя ещё раз, но уже к времени, в котором точно будут текущие файлы
//            return getPlayDef()
//        }
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private fun getStreamIndex( cameraIndex: Int ): Int {
//        val cd = alCameraDef[ cameraIndex ]
//        //--- для режима мелкого просмотра используем второй поток видео, если он есть и не пустой
//        return if( tsShowModePreview.contains( cameraIndex ) && cd.alStream.size > 1 && !cd.alStream[ 1 ].alFileName.isEmpty() ) 1 else 0
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    fun writeGraphicParam( bbOut: AdvancedByteBuffer ) {
//        bbOut.putShortString( startParamID )
//        bbOut.putShortString( startTitle )
//        bbOut.putBoolean( isOnlineMode )
//    }
//
//    fun writeCoords( bbOut: AdvancedByteBuffer ) {
//        bbOut.putInt( ( vcViewCoord.t1 / 1000 ).toInt() )
//        bbOut.putInt( ( vcViewCoord.t2 / 1000 ).toInt() )
//    }
//
//    //    public void write( AdvancedByteBuffer bbOut ) throws Throwable {
//    //        bbOut.putInt( alFileTime.size() );
//    //        for( int i = 0; i < alFileTime.size(); i++ ) {
//    //            bbOut.putInt( (int) ( alFileTime.get( i ) / 1000 ) );
//    //            bbOut.putShortString( alDirName.get( i ) );
//    //            bbOut.putShortString( alFileName.get( i ) );
//    //        }
//    //    }
//}
