package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable

//import jakarta.persistence.*

//@Entity
//@Table(name = "led_strip_client")
@Serdeable
@MappedEntity("led_strip_clients")
data class LedStripClientEntity(
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id", updatable = false, nullable = false)
    @Id
    @GeneratedValue
    var id: Long = -1,

//    @OneToMany(mappedBy = "client")
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "client")
    var strips: Set<LedStripEntity> = setOf(),

//    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null,

//    @Column(name = "address", nullable = false, unique = true)
    var address: String? = null,

//    @Column(name = "ws_port", nullable = false, unique = false)
    var wsPort: Int? = null,

//    @Column(name = "api_port", nullable = false, unique = false)
    var apiPort: Int? = null,
)