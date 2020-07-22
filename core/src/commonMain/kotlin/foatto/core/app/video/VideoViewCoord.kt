//@file:JvmName("VideoViewCoord")
//package foatto.core.app.video
//
//class VideoViewCoord( aT1: Long, aT2: Long ) {
//
//    @JvmField var t1: Long = 0
//    @JvmField var t2: Long = 0
//
//    //----------------------------------------------------------------------------------------------------------------------
//
//    val width: Long
//        get() = t2 - t1
//
//    init {
//        set( aT1, aT2 )
//    }
//
//    fun set( aX1: Long, aX2: Long ) {
//        t1 = aX1
//        t2 = aX2
//    }
//
//    fun moveRel( dx: Long ) {
//        t1 += dx
//        t2 += dx
//    }
//
//    //    public boolean isEquals( VideoViewCoord view ) {
//    //        return t1 == view.t1 && t2 == view.t2;
//    //    }
//
//}
