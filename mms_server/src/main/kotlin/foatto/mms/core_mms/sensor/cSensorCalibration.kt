package foatto.mms.core_mms.sensor

import foatto.core.app.*
import foatto.core.link.*
import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cStandart

class cSensorCalibration : cStandart() {

    companion object {

        private val SENSOR_ID = "sensor_id"

        //--- префикс полей со значениями
        private val SENSOR_FIELD_PREFIX = "value_sensor_"
        private val DATA_FIELD_PREFIX = "value_data_"
    }

    override fun getForm( hmOut: MutableMap<String, Any> ): FormResponse {

        val id = getIDFromParam()
        //--- мегаформа ввода калибровок открывается только при попытке их создания,
        //--- иначе запускаем обычную привычную форму
        if( id != 0 ) return super.getForm( hmOut )

        val refererID = hmParam[ AppParameter.REFERER ]
        var refererURL: String? = null
        if( refererID != null ) refererURL = chmSession[ AppParameter.REFERER + refererID ] as String

        //--- подготовка "чистого" appParam для кнопок формы
        //--- ( простое клонирование исходного hmParam здесь не годится,
        //--- т.к. придёт много попутных мусорных параметров, которые могут внезапно выстрелить где-нибудь
        val formParam = getFormParam()

        //--- начало нестандартной части ---------------------------------------------------------------------------------------

        //--- сбор парентов
        val sensorID = getParentID( "mms_sensor" )!!

        val sqlCalibration = " SELECT value_sensor , value_data FROM MMS_sensor_calibration WHERE sensor_id = $sensorID ORDER BY value_sensor "
        val alSensorValue = mutableListOf<Int?>()
        val alDataValue = mutableListOf<Double?>()

        val rs = stm.executeQuery( sqlCalibration )
        while( rs.next() ) {
            alSensorValue.add( rs.getInt( 1 ) )
            alDataValue.add( rs.getDouble( 2 ) )
        }
        rs.close()

        //--- добавим пустых полей для добавления новых калибровок
        //--- полное кол-во строк = текущему + 50% добавки пустых ( минимум 30 строк, если получается меньше )
        val nextSize = Math.max( 30, alSensorValue.size * 3 / 2 )
        while( alSensorValue.size < nextSize ) {
            alSensorValue.add( null )
            alDataValue.add( null )
        }

        //--- окончание нестандартной части ------------------------------------------------------------------------------------

        //--- заголовок формы
        val alHeader = mutableListOf<Pair<String,String>>()
        fillHeader( null, false, alHeader, hmOut )

        //--- основные поля - применяются сокращенные/оптимизированные варианты getFormCell
        val alFormCell = mutableListOf<FormCell>()
        for( rowIndex in alSensorValue.indices ) {
            //--- Значение датчика
            val sensorValue = alSensorValue[ rowIndex ]
            var fci = FormCell( FormCellType.STRING )
            fci.name = StringBuilder( SENSOR_FIELD_PREFIX ).append( rowIndex ).toString()
            fci.value = if( sensorValue == null ) "" else getSplittedLong( sensorValue.toLong() ).toString()
            fci.column = 10
            fci.itEditable = true
            fci.caption = " "  // совсем нулевая строка даст невидимое поле
            alFormCell.add( fci )

            //--- Значение измеряемой величины
            val dataValue = alDataValue[ rowIndex ]
            fci = FormCell( FormCellType.STRING )
            fci.name = StringBuilder( DATA_FIELD_PREFIX ).append( rowIndex ).toString()
            fci.value = if( dataValue == null ) "" else getSplittedDouble( dataValue, 1 ).toString()
            fci.column = 10
            fci.itEditable = true
            fci.caption = " "  // совсем нулевая строка даст невидимое поле
            alFormCell.add( fci )
        }

        val alFormButton = mutableListOf<FormButton>()
        alFormButton.add( FormButton(
            url = AppParameter.setParam(
                        AppParameter.setParam( formParam, AppParameter.ACTION, AppAction.SAVE ),
                        SENSOR_ID, sensorID.toString()
                  ),
            caption = model.getSaveButonCaption( aliasConfig ),
            iconName = ICON_NAME_SAVE,
            withNewData = true,   // передавать ли новые данные ( для SAVE ) или передать старые данные ( для DELETE и проч. комманд )
            key = BUTTON_KEY_SAVE
        ) )
        if( refererURL != null ) {
            alFormButton.add( FormButton(
                url = refererURL,
                caption = "Выйти",
                iconName = ICON_NAME_EXIT,
                withNewData = false,
                key = BUTTON_KEY_EXIT
            ) )
        }

        return FormResponse(
            tab = aliasConfig.descr,
            alHeader = alHeader,
            columnCount = 2,
            alFormColumn = listOf( "Значение датчика", "Значение измеряемой величины" ),
            alFormCell = alFormCell,
            alFormButton = alFormButton
        )

    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val id = getIDFromParam()
        //--- мегаформа ввода калибровок сохраняет только при попытке их создания,
        //--- иначе запускаем обычный процесс сохранения
        if( id != 0 ) return super.doSave(action, alFormData, hmOut)

        val sensorID = hmParam[ SENSOR_ID ]!!.toInt()
        //--- удалить старые записи
        stm.executeUpdate( " DELETE FROM MMS_sensor_calibration WHERE sensor_id = $sensorID " )
        //--- добавить новые
        var i = 0
        while( i < alFormData.size ) {
            //--- сокращенный/оптимизированный вариант чтения из соответствующих DataInt/DataDouble
            val strSensor = alFormData[ i++ ].stringValue!!
            val strData = alFormData[ i++ ].stringValue!!
            //--- строки с пустыми значениями просто пропускаем
            if( strSensor.isEmpty() || strData.isEmpty() ) continue
            val sensorValue: Int
            val dataValue: Double
            try {
                sensorValue = strSensor.replace( " ", "" ).toInt()
                dataValue = strData.replace( ',', '.' ).replace( " ", "" ).toDouble()
            }
            catch( t: Throwable ) {
                continue
            }
            //--- неправильно введенные числа тоже игнорируем
            stm.executeUpdate(
                " INSERT INTO MMS_sensor_calibration ( id, sensor_id , value_sensor , value_data ) VALUES ( " +
                stm.getNextID( "MMS_sensor_calibration", "id" ) + " , $sensorID , $sensorValue , $dataValue ) " )
        }

        return AppParameter.setParam( chmSession[ AppParameter.REFERER + hmParam[ AppParameter.REFERER ] ] as String, AppParameter.ID, Integer.toString( id ) )
    }
}
