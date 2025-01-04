package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable
//import jakarta.persistence.*

//@Entity
//@Table(name = "group_member_led_strip")
@Serdeable
@MappedEntity("group_member_led_strips")
data class GroupMemberLedStripEntity(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id", updatable = false, nullable = false)
    @Id
    @GeneratedValue
    var id: Long = -1,

//    @ManyToOne
    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var group: LedStripGroupEntity? = null,

//    @ManyToOne
    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var strip: LedStripEntity? = null,

//    @Column(name = "inverted", nullable = false)
    var inverted: Boolean? = null,

//    @Column(name = "group_index", nullable = false)
    var groupIndex: Int? = null,
)