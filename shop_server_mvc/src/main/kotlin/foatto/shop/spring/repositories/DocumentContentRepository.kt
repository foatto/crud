package foatto.shop.spring.repositories

import foatto.shop.spring.entities.DocumentContentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentContentRepository : JpaRepository<DocumentContentEntity, Int> {

    fun findAllByMarkCode(markCode: String): List<DocumentContentEntity>
}
