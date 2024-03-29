package foatto.ts.core_ts

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBinary
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.calc.AbstractObjectStateCalc

class cData : cTSOneObjectParent() {

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ДО фильтров поиска и страничной разбивки
    override fun generateColumnDataBeforeFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateColumnDataBeforeFilter(hmColumnData)

        generateColumnData(hmColumnData)
    }

    override fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.getCalculatedFormColumnData(id, hmColumnData)

        generateColumnData(hmColumnData)
    }

    private fun generateColumnData(hmColumnData: MutableMap<iColumn, iData>) {
        val md = model as mData

        //--- прописываем глобальное/локальное время (оно прописано в поле ontime как якобы id-поле)
        val timeUTC = (hmColumnData[md.columnId] as DataInt).intValue

        (hmColumnData[md.columnDataOnTimeUTC] as DataDateTimeInt).setDateTime(timeUTC)
        (hmColumnData[md.columnDataOnTimeLocal] as DataDateTimeInt).setDateTime(timeUTC)

        //--- берём сырые бинарные данные
        val dsd = hmColumnData[md.columnDataBinary] as DataBinary
        val bb = dsd.binaryValue
        //--- список полей прописанных датчиков
        val tmSensorColumn = md.tmSensorColumn
        //--- строка-сборник данных по прочим/непрописанным датчикам
        val sbOtherData = StringBuilder()

        val hmSensorPortType = md.hmSensorPortType

        while (bb.hasRemaining()) {
            val (portNum, dataSize) = AbstractObjectStateCalc.getSensorPortNumAndDataSize(bb)
            val isPresentedPort = tmSensorColumn.containsKey(portNum)
            //--- по каждому номеру порта - составляем визуальное представление значения
            val sensorValue = AbstractObjectStateCalc.getSensorString(hmSensorPortType[portNum], dataSize, bb)
            //--- выводим только определённые порты
            if (isPresentedPort) {
                (hmColumnData[tmSensorColumn[portNum]!!] as DataString).text = sensorValue
            } else {
                sbOtherData.append(if (sbOtherData.isEmpty()) "" else " ").append(portNum).append('=').append(sensorValue)
            }
        }

        (hmColumnData[md.columnDataSensorOther] as DataString).text = sbOtherData.toString()
    }
}
