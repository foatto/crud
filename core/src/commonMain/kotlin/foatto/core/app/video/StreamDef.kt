//@file:JvmName("StreamDef")
//package foatto.core.app.video
//
//import foatto.core.util.AdvancedByteBuffer
//import java.util.ArrayList
//
//class StreamDef {
//
//    @JvmField var alFileBeg: ArrayList<Long>
//    @JvmField var alFileEnd: ArrayList<Long>
//    @JvmField var alFileName: ArrayList<String>
//
//    constructor() {
//        //--- типовой интервал - сутки, итого файлов с типовой 5-минутной нарезкой = 288 шт.
//        alFileBeg = ArrayList( 300 )
//        alFileEnd = ArrayList( 300 )
//        alFileName = ArrayList( 300 )
//    }
//
//    constructor( bbIn: AdvancedByteBuffer ) {
//        val count = bbIn.getInt()
//
//        alFileBeg = ArrayList( count )
//        alFileEnd = ArrayList( count )
//        alFileName = ArrayList( count )
//
//        for( i in 0 until count ) {
//            alFileBeg.add( bbIn.getInt() * 1000L )
//            alFileEnd.add( bbIn.getInt() * 1000L )
//            alFileName.add( bbIn.getShortString() )
//        }
//    }
//
//    fun write( bbOut: AdvancedByteBuffer ) {
//        bbOut.putInt( alFileName.size )
//        for( i in alFileName.indices ) {
//            bbOut.putInt( ( alFileBeg[ i ] / 1000 ).toInt() )
//            bbOut.putInt( ( alFileEnd[ i ] / 1000 ).toInt() )
//            bbOut.putShortString( alFileName[ i ] )
//        }
//    }
//
//}
