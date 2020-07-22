package foatto.core_server.app

object AppParameter {

    private const val EMPTY = ""

    const val ACTION = "action"   //!!! также используется для ANDROID

    const val ALIAS = "alias"

    const val REFERER = "referer" // через него передается referer_id=ХХХ, по которому берется значение referer
    //    public static final String MESSAGE = "message"; // через него передается message_id=ХХХ, по которому берется ссылка на MessageData

    const val ID = "id"

    const val PARENT_ALIAS = "parent_alias"
    const val PARENT_ID = "parent_id"

    const val PARENT_USER_ID = "parent_user_id"

    const val FINDER = "finder"   // через него передается finder_id=ХХХ, по которому берется значение finder
    const val PAGE = "page"
    const val SELECTOR = "selector"
    const val SORT = "sort"

    const val FORM_DATA = "form_data"         // (предварительно) заполненные данные формы
    const val FORM_SELECTOR = "form_selector" // номер запускаемого селектора в форме

    //--- через него передается graphic_start_data_id=ХХХ, по которому берется значение graphic_start_data
    const val GRAPHIC_START_DATA = "graphic_start_data"

    //---- через него передается xy_start_data_id=ХХХ, по которому берется значение xy_start_data
    const val XY_START_DATA = "xy_start_data"

    fun parseParam( appParam: String, hmParam: MutableMap<String, String> ): String {
        var action = EMPTY
        //--- если в строке нет ни одной пары "параметр=значение", значит это просто action
        if( !appParam.contains( '=' ) ) {
            action = appParam
            hmParam[ACTION] = action
        }
        else {
            appParam.split( "&" ).forEach { pair ->
                val arr = pair.split( "=" )
                val name = arr[ 0 ]
                val value = arr[ 1 ]
                hmParam[ name ] = value
                if( name == ACTION) action = value
            }
        }
        return action
    }

    fun collectParam( hmParam: Map<String, String> ): String {
        var param = ""
        hmParam.forEach { key, value ->  param += "&$key=$value" }
        return param.substring( 1 ) // пропускаем первый &
    }

    fun setParam( appParam: String, paramName: String, paramValue: String ): String {
        val hmParam = mutableMapOf<String, String>()
        parseParam(appParam, hmParam)
        hmParam[ paramName ] = paramValue
        return collectParam(hmParam)
    }
}
