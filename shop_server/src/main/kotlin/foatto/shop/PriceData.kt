package foatto.shop

import foatto.sql.CoreAdvancedConnection
import java.time.ZoneId
import java.time.ZonedDateTime

object PriceData {

    //--- загрузка прайса
    fun loadPrice(conn: CoreAdvancedConnection, priceType: Int): Map<Int, List<Pair<Int, Double>>> {
        val hmPrice = mutableMapOf<Int, MutableList<Pair<Int, Double>>>()

        val rs = conn.executeQuery(" SELECT catalog_id , ye , mo , da , price_value FROM SHOP_price WHERE price_type = $priceType ORDER BY catalog_id , ye , mo , da ")
        while (rs.next()) {
            val catalogID = rs.getInt(1)
            val time = ZonedDateTime.of(rs.getInt(2), rs.getInt(3), rs.getInt(4), 0, 0, 0, 0, ZoneId.systemDefault()).toEpochSecond().toInt()
            val value = rs.getDouble(5)

            val alHistory = hmPrice.getOrPut(catalogID) { mutableListOf() }
            alHistory.add(Pair(time, value))
        }
        rs.close()

        return hmPrice
    }

    //--- получить цену товара за определённую дату
    fun getPrice(hmPrice: Map<Int, List<Pair<Int, Double>>>, catalogID: Int, zoneId: ZoneId, ye: Int, mo: Int, da: Int): Double {
        val zdt = ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId)
        return getPrice(hmPrice, catalogID, zdt.toEpochSecond().toInt())
    }

    fun getPrice(hmPrice: Map<Int, List<Pair<Int, Double>>>, catalogID: Int, calcTime: Int): Double {
        val alHistory = hmPrice[catalogID]
        if (alHistory != null) {
            for (i in alHistory.size - 1 downTo 0) {
                val (time, price) = alHistory[i]
                if (time <= calcTime) {
                    return price
                }
            }
        }
        return 0.0
    }
}