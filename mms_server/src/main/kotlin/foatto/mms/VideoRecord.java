//package foatto.mms;
//
//import foatto.core_server.app.video.server.CoreVideoRecord;
//
//public class VideoRecord extends CoreVideoRecord {
//
//    public static void main( String[] args ) {
//        new VideoRecord().run( args );
//    }
//
//    protected void extraWork() throws Throwable {
//        super.extraWork();
//
////            //--- возможная связь/привязка к внешней системе
////            if( ffmpegExtraCommand != null && ! ffmpegExtraCommand.isEmpty() ) {
////                bwText.write( new StringBuilder( "call " ).append( ffmpegExtraCommand )
////                                        .append( ' ' ).append( cameraDescr.isEmpty() ? '-' : cameraDescr )
////                                        .append( ' ' ).append( streamIndex )
////                                        .append( ' ' ).append( cameraDir ).toString() );
////                bwText.newLine();
////            }
//
////DEL_ID=10000
//
////source ./set_del_id.var
////
////DEL_ROOT=/home/vc/MMSServerVideoLinuxARM/http_root/del_root
////
////mkdir -p $DEL_ROOT/$DEL_ID/$1/$2/$beg_dir_name
////
////# mv $3/$2/$beg_dir_name/$beg_dir_name-$beg_file_name-$end_dir_name-$end_file_name.mp4 $DEL_ROOT/$DEL_ID/$1/$2/$beg_dir_name
////cp $3/$2/$beg_dir_name/$beg_dir_name-$beg_file_name-$end_dir_name-$end_file_name.mp4 $DEL_ROOT/$DEL_ID/$1/$2/$beg_dir_name/
//
//
//    }
//}
