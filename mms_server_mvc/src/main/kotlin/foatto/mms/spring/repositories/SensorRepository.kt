package foatto.mms.spring.repositories

import foatto.mms.spring.entities.SensorEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SensorRepository : JpaRepository<SensorEntity, Int>