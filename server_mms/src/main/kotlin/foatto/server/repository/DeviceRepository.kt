package foatto.server.repository

import foatto.server.entity.DeviceEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceRepository : JpaRepository<DeviceEntity, Int> {

    fun findBySerialNo(serialNo: String): List<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE oe.id = ?1
        """
    )
    fun findByObjectId(objectId: Int): List<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            WHERE de.id <> 0
                AND de.userId IN ?1
        """
    )
    fun findByUserIdIn(userIds: List<Int>, pageRequest: PageRequest): Page<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE de.id <> 0
                AND oe = ?1
                AND de.userId IN ?2
        """
    )
    fun findByObjAndUserIdIn(obj: ObjectEntity, userIds: List<Int>, pageRequest: PageRequest): Page<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE de.id <> 0
                AND de.userId IN ?1
                AND (
                        LOWER(de.serialNo) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellImei) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellNumber) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellIcc) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellOperator) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellImei2) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellNumber2) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellIcc2) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.cellOperator2) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.fwVersion) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.lastSessionStatus) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.lastSessionError) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<DeviceEntity>

    @Query(
        """
            SELECT de
            FROM DeviceEntity de
            LEFT JOIN de.obj oe
            WHERE de.id <> 0
                AND oe = ?1
                AND de.userId IN ?2
                AND (
                        LOWER(de.serialNo) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellImei) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellNumber) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellIcc) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellOperator) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellImei2) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellNumber2) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellIcc2) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.cellOperator2) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.fwVersion) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.lastSessionStatus) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(de.lastSessionError) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                )
        """
    )
    fun findByObjAndUserIdInAndFilter(obj: ObjectEntity, userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<DeviceEntity>
}
