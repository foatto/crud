package foatto.shop.spring.repositories

import foatto.shop.spring.entities.CatalogEntity
import org.springframework.data.repository.CrudRepository

interface CatalogRepository : CrudRepository<CatalogEntity, Int>
