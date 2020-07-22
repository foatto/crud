package foatto.core.app.xy.config

class XyBitmapType( val name: String, val descr: String ) {

    companion object {

        //    public static final String GM = "gm";   // Google Map
        //    public static final String GS = "gs";   // Google Sat
        const val MN = "mn"   // MAPNIK
        const val MS = "ms"   // MapSurfer
        //    public static final String WM = "wm";   // WikiMapia

        //--------------------------------------------------------------------------------------------------------------

        //    public static final HashMap<String,String> hmAllNameDescr = new HashMap<>();
        val hmEnabledNameDescr = hashMapOf(
            //        hmAllNameDescr.put( GS, "Google Sat" );
            //        hmAllNameDescr.put( GM, "Google Map" );
            //        hmAllNameDescr.put( WM, "WikiMapia" );

            MN to  "MAPNIK (OpenStreetMap)",
            MS to "MapSurfer (OpenStreetMap)" )

        //--------------------------------------------------------------------------------------------------------------

        const val BITMAP_DIR = "/map/image/"
        const val BITMAP_EXT = "png"
        const val BLOCK_SIZE = 256

        //--------------------------------------------------------------------------------------------------------------

        //--- сейчас максимальный уровень в картах - 18, но запас не помешает
        //private static final int MAX_MAP_LEVEL = 22;
        //--- список level/zoom для каждого масштаба
        val hmTypeScaleZ = mutableMapOf<String, MutableMap<Int, Int>>()

        init {
            // gs = Google Sat
            // mn = MAPNIK
            // wm = WikiMapia
            // ms = MapSurfer
            val hmScaleZ = mutableMapOf<Int, Int>()
            hmScaleZ.put( 16, 18 )
            hmScaleZ.put( 32, 17 )
            hmScaleZ.put( 64, 16 )
            hmScaleZ.put( 128, 15 )
            hmScaleZ.put( 256, 14 )
            hmScaleZ.put( 512, 13 )
            hmScaleZ.put( 1024, 12 )
            hmScaleZ.put( 2 * 1024, 11 )
            hmScaleZ.put( 4 * 1024, 10 )
            hmScaleZ.put( 8 * 1024, 9 )
            hmScaleZ.put( 16 * 1024, 8 )
            hmScaleZ.put( 32 * 1024, 7 )
            hmScaleZ.put( 64 * 1024, 6 )
            hmScaleZ.put( 128 * 1024, 5 )
            hmScaleZ.put( 256 * 1024, 4 )
            hmScaleZ.put( 512 * 1024, 3 )

            //        hmTypeScaleZ.put( GS, hmScaleZ );
            hmTypeScaleZ.put( MN, hmScaleZ )
            hmTypeScaleZ.put( MS, hmScaleZ )
            //        hmTypeScaleZ.put( WM, hmScaleZ );

            //        // gm = Google Map
            //        hmScaleZ = new HashMap<>();
            //        hmScaleZ.put(         16, -1 );
            //        hmScaleZ.put(         32,  0 );
            //        hmScaleZ.put(         64,  1 );
            //        hmScaleZ.put(        128,  2 );
            //        hmScaleZ.put(        256,  3 );
            //        hmScaleZ.put(        512,  4 );
            //        hmScaleZ.put(       1024,  5 );
            //        hmScaleZ.put(   2 * 1024,  6 );
            //        hmScaleZ.put(   4 * 1024,  7 );
            //        hmScaleZ.put(   8 * 1024,  8 );
            //        hmScaleZ.put(  16 * 1024,  9 );
            //        hmScaleZ.put(  32 * 1024, 10 );
            //        hmScaleZ.put(  64 * 1024, 11 );
            //        hmScaleZ.put( 128 * 1024, 12 );
            //        hmScaleZ.put( 256 * 1024, 13 );
            //        hmScaleZ.put( 512 * 1024, 14 );
            //
            //        hmTypeScaleZ.put( GM, hmScaleZ );
        }
    }
}
