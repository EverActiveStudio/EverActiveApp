package pl.everactive.backend.repositories

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import pl.everactive.backend.entities.EventEntity

interface EventRepository : CoroutineCrudRepository<EventEntity, Long>
