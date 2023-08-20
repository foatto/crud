package foatto.mms.spring.repositories

import foatto.mms.spring.entities.SensorCalibrationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SensorCalibrationRepository : JpaRepository<SensorCalibrationEntity, Int>