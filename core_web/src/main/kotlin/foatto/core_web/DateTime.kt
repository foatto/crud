package foatto.core_web

import kotlin.js.Date

fun DateTime_Arr( dt: Date ) =
    intArrayOf(
        dt.getUTCFullYear(),
        dt.getUTCMonth() + 1,
        dt.getUTCDate(),
        dt.getUTCHours(),
        dt.getUTCMinutes(),
        dt.getUTCSeconds()
    )

fun DateTime_Arr( timeOffset: Int, sec: Int ) = DateTime_Arr( Date( ( sec + timeOffset ) * 1000L ) )

fun DateTime_YMDHMS( timeOffset: Int, sec: Int ) = DateTime_YMDHMS( DateTime_Arr( timeOffset, sec ) )
//fun DateTime_YMDHMS( gc: GregorianCalendar ): StringBuilder = DateTime_YMDHMS( DateTime_Arr( gc ) )
fun DateTime_YMDHMS( arrDT: IntArray ) =
   "${ if( arrDT[ 0 ] < 10 ) '0' else "" }${arrDT[ 0 ]}." +
   "${ if( arrDT[ 1 ] < 10 ) '0' else "" }${arrDT[ 1 ]}." +
   "${ if( arrDT[ 2 ] < 10 ) '0' else "" }${arrDT[ 2 ]} " +
   "${ if( arrDT[ 3 ] < 10 ) '0' else "" }${arrDT[ 3 ]}:" +
   "${ if( arrDT[ 4 ] < 10 ) '0' else "" }${arrDT[ 4 ]}:" +
   "${ if( arrDT[ 5 ] < 10 ) '0' else "" }${arrDT[ 5 ]}"

fun DateTime_DMYHMS( timeOffset: Int, sec: Int ) = DateTime_DMYHMS( DateTime_Arr( timeOffset, sec ) )
//fun DateTime_DMYHMS( gc: GregorianCalendar ): StringBuilder = DateTime_DMYHMS( DateTime_Arr( gc ) )
fun DateTime_DMYHMS( arrDT: IntArray ) =
   "${ if( arrDT[ 2 ] < 10 ) '0' else "" }${arrDT[ 2 ]}." +
   "${ if( arrDT[ 1 ] < 10 ) '0' else "" }${arrDT[ 1 ]}." +
   "${ if( arrDT[ 0 ] < 10 ) '0' else "" }${arrDT[ 0 ]} " +
   "${ if( arrDT[ 3 ] < 10 ) '0' else "" }${arrDT[ 3 ]}:" +
   "${ if( arrDT[ 4 ] < 10 ) '0' else "" }${arrDT[ 4 ]}:" +
   "${ if( arrDT[ 5 ] < 10 ) '0' else "" }${arrDT[ 5 ]}"
