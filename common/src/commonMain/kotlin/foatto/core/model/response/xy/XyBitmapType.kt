package foatto.core.model.response.xy

class XyBitmapType(val name: String, val descr: String) {

    companion object {

        const val MN = "mn"   // MAPNIK
        const val MS = "ms"   // MapSurfer

        //--------------------------------------------------------------------------------------------------------------

        val hmEnabledNameDescr = hashMapOf(
            MN to "MAPNIK (OpenStreetMap)",
            MS to "MapSurfer (OpenStreetMap)"
        )

        //--------------------------------------------------------------------------------------------------------------

        const val BITMAP_DIR = "/map/image/"
        const val BITMAP_EXT = "png"
        const val BLOCK_SIZE = 256

        //--------------------------------------------------------------------------------------------------------------

        val hmTypeScaleZ = mutableMapOf<String, MutableMap<Int, Int>>()

        init {
            // mn = MAPNIK
            // ms = MapSurfer
            val hmScaleZ = mutableMapOf<Int, Int>()
            hmScaleZ[16] = 18
            hmScaleZ[32] = 17
            hmScaleZ[64] = 16
            hmScaleZ[128] = 15
            hmScaleZ[256] = 14
            hmScaleZ[512] = 13
            hmScaleZ[1024] = 12
            hmScaleZ[2 * 1024] = 11
            hmScaleZ[4 * 1024] = 10
            hmScaleZ[8 * 1024] = 9
            hmScaleZ[16 * 1024] = 8
            hmScaleZ[32 * 1024] = 7
            hmScaleZ[64 * 1024] = 6
            hmScaleZ[128 * 1024] = 5
            hmScaleZ[256 * 1024] = 4
            hmScaleZ[512 * 1024] = 3

            hmTypeScaleZ[MN] = hmScaleZ
            hmTypeScaleZ[MS] = hmScaleZ
        }
    }
}
