package foatto.shop.spring.repositories

import foatto.shop.spring.entities.DocumentContentEntity
import org.springframework.data.repository.CrudRepository

interface DocumentContentRepository : CrudRepository<DocumentContentEntity, Int> {

    fun findAllByMarkCode(markCode: String): List<DocumentContentEntity>
}
