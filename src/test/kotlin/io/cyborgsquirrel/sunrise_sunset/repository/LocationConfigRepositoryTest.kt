package io.cyborgsquirrel.sunrise_sunset.repository

import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@MicronautTest(startApplication = false, transactional = false)
class LocationConfigRepositoryTest(
    private val locationConfigRepository: H2LocationConfigRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
) : StringSpec({

    // Seattle
    val demoLocationConfigEntity = LocationConfigEntity(latitude = "47.36", longitude = "-122.20", active = true)
    // New York City
    val demoLocationInactiveConfigEntity =
        LocationConfigEntity(latitude = "40.4246", longitude = "-74.022", active = false)
    val demoSunriseSunsetEntity = SunriseSunsetTimeEntity(
        ymd = "2025-01-02",
        json = "{\"results\":{\"sunrise\":\"2025-01-02T15:54:02+00:00\",\"sunset\":\"2025-01-03T00:32:10+00:00\",\"solar_noon\":\"2025-01-02T20:13:06+00:00\",\"day_length\":31088,\"civil_twilight_begin\":\"2025-01-02T15:20:28+00:00\",\"civil_twilight_end\":\"2025-01-03T01:05:44+00:00\",\"nautical_twilight_begin\":\"2025-01-02T14:41:38+00:00\",\"nautical_twilight_end\":\"2025-01-03T01:44:34+00:00\",\"astronomical_twilight_begin\":\"2025-01-02T14:04:26+00:00\",\"astronomical_twilight_end\":\"2025-01-03T02:21:46+00:00\"},\"status\":\"OK\",\"tzid\":\"UTC\"}",
        location = demoLocationConfigEntity,
    )

    fun verifyConfigsAreEqual(retrievedEntity: LocationConfigEntity) {
        retrievedEntity.latitude shouldBe demoLocationConfigEntity.latitude
        retrievedEntity.longitude shouldBe demoLocationConfigEntity.longitude
        retrievedEntity.active shouldBe demoLocationConfigEntity.active
    }

    afterTest {
        sunriseSunsetTimeRepository.deleteAll()
        locationConfigRepository.deleteAll()
    }

    "Create a config entity" {
        val savedEntity = locationConfigRepository.save(demoLocationConfigEntity)
        // Default id value is -1 because it is not valid in SQL
        // If this saved successfully we should have an auto-generated id greater than 0
        savedEntity.id shouldBeGreaterThan 0
    }

    "Query all config entities" {
        locationConfigRepository.save(demoLocationConfigEntity)
        val retrievedEntities = locationConfigRepository.queryAll()

        retrievedEntities.isEmpty() shouldBe false
        retrievedEntities.size shouldBe 1

        val retrievedEntity = retrievedEntities.first()
        verifyConfigsAreEqual(retrievedEntity)
    }

    "Query active config entities" {
        locationConfigRepository.save(demoLocationConfigEntity)
        locationConfigRepository.save(demoLocationInactiveConfigEntity)
        val retrievedEntities = locationConfigRepository.findByActiveTrue()

        retrievedEntities.isPresent shouldBe true

        val retrievedEntity = retrievedEntities.get()
        verifyConfigsAreEqual(retrievedEntity)
    }

    "Query a location entity with associated sunrise sunset times" {
        locationConfigRepository.save(demoLocationConfigEntity)
        sunriseSunsetTimeRepository.save(demoSunriseSunsetEntity)
        val retrievedEntities = locationConfigRepository.findByActiveTrue()

        retrievedEntities.isPresent shouldBe true

        val retrievedEntity = retrievedEntities.get()
        verifyConfigsAreEqual(retrievedEntity)
        retrievedEntity.sunriseSunsetTimes.size shouldBe 1
        retrievedEntity.sunriseSunsetTimes.first().ymd shouldBe demoSunriseSunsetEntity.ymd
        retrievedEntity.sunriseSunsetTimes.first().json shouldBe demoSunriseSunsetEntity.json
    }
})