package io.cyborgsquirrel.clients.entity

import io.cyborgsquirrel.clients.enums.ClientType
import io.cyborgsquirrel.clients.enums.ColorOrder
import io.cyborgsquirrel.led_strips.entity.LedStripEntity
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@MappedEntity("led_strip_clients")
data class LedStripClientEntity(

    @Id
    @GeneratedValue
    var id: Long = -1,

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "client")
    var strips: Set<LedStripEntity> = setOf(),

    var name: String?,

    var uuid: String?,

    var address: String?,

    @MappedEntity("client_type")
    @Enumerated(EnumType.STRING)
    var clientType: ClientType?,

    @MappedEntity("color_order")
    @Enumerated(EnumType.STRING)
    var colorOrder: ColorOrder?,

    var wsPort: Int? = null,

    var apiPort: Int? = null,
) {
    // Overrides to prevent infinite looping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LedStripClientEntity) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (address != other.address) return false
        if (clientType != other.clientType) return false
        if (colorOrder != other.colorOrder) return false
        if (uuid != other.uuid) return false
        if (apiPort != other.apiPort) return false
        if (wsPort != other.wsPort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (address ?: 0).hashCode()
        result = 31 * result + (clientType?.ordinal ?: 0).hashCode()
        result = 31 * result + (colorOrder ?: 0).hashCode()
        result = 31 * result + (uuid ?: 0).hashCode()
        result = 31 * result + (apiPort ?: 0).hashCode()
        result = 31 * result + (wsPort ?: 0).hashCode()
        return result
    }

    override fun toString(): String {
        return "LedStripClientEntity(id=$id, name=$name, address=$address, clientType=$clientType, colorOrder=$colorOrder uuid=$uuid, apiPort=$apiPort, wsPort=$wsPort)"
    }
}