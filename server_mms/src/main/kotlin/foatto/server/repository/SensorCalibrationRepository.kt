package foatto.server.repository

import foatto.server.entity.SensorCalibrationEntity
import foatto.server.entity.SensorEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository

interface SensorCalibrationRepository : JpaRepository<SensorCalibrationEntity, Int> {
    fun findBySensorIdOrderBySensorValue(sensorId: Int): List<SensorCalibrationEntity>
    fun findBySensorOrderBySensorValue(sensor: SensorEntity): List<SensorCalibrationEntity>

    fun deleteBySensorId(sensorId: Int): Int

    fun findBySensor(sensor: SensorEntity, pageRequest: PageRequest): Page<SensorCalibrationEntity>

//    @Query(
//        """
//            SELECT sce
//            FROM SensorCalibrationEntity sce
//            WHERE sce.id <> 0
//                AND sce.sensor = ?1
//                AND (
//                        LOWER(sce.sensorValue) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
//                     OR LOWER(sce.dataValue) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
//                )
//        """
//    )
//    fun findBySensorAndFilter(sensor: SensorEntity, findText: String, pageRequest: PageRequest): Page<SensorCalibrationEntity>

}