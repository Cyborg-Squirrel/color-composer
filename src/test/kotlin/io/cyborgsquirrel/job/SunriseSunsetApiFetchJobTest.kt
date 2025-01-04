package io.cyborgsquirrel.job

import io.cyborgsquirrel.client.SunriseSunsetApiClient
import io.cyborgsquirrel.entity.LocationConfigEntity
import io.cyborgsquirrel.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.model.responses.sunrise_sunset.SunriseSunsetModel
import io.cyborgsquirrel.repository.H2LocationConfigRepository
import io.cyborgsquirrel.repository.H2SunriseSunsetTimeRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.serde.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension.getMock
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.*
import java.util.concurrent.CompletableFuture

@MicronautTest
class SunriseSunsetApiFetchJobTest(
    private val job: SunriseSunsetApiFetchJob,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val client: SunriseSunsetApiClient,
    private val objectMapper: ObjectMapper,
) : StringSpec({

    // London
    val demoLocationConfigEntity = LocationConfigEntity(latitude = "51.3026", longitude = "7.39", active = true)
    // Perth
    val demoLocationInactiveConfigEntity =
        LocationConfigEntity(latitude = "-31.5721", longitude = "115.5135", active = false)
    val entities = mutableListOf<SunriseSunsetTimeEntity>()

    beforeTest {
        locationConfigRepository.save(demoLocationConfigEntity)
        locationConfigRepository.save(demoLocationInactiveConfigEntity)

        val mockSunriseSunsetRepo = getMock(sunriseSunsetTimeRepository)
        every {
            mockSunriseSunsetRepo.save(any())
        } answers {
            val entity = it.invocation.args.first() as SunriseSunsetTimeEntity
            entities.add(entity)
            entity
        }
        every {
            mockSunriseSunsetRepo.findByYmdEqualsAndLocationEquals(any(), any())
        } answers {
            val ymdString = it.invocation.args.first() as String
            val location = it.invocation.args.last() as LocationConfigEntity
            val matchingEntity = entities.firstOrNull { e ->
                e.ymd == ymdString && e.location == location
            }
            Optional.ofNullable(matchingEntity)
        }
        every {
            mockSunriseSunsetRepo.findAllOrderByYmd(any())
        } answers {
            entities.sortedBy { e ->
                e.ymd
            }
        }

        val mockClient = getMock(client)
        every {
            mockClient.getSunriseSunsetTimes(any(), any(), any())
        } answers {
            val ymdString = it.invocation.args.last() as String
            val ymdObject = LocalDate.parse(ymdString)
            if (ymdObject.dayOfMonth == 2) {
                val sunriseSunsetModel = objectMapper.readValue(
                    SunriseSunsetApiTestData.apiResponse2025Jan2Json,
                    SunriseSunsetModel::class.java
                )
                CompletableFuture.completedFuture(sunriseSunsetModel)
            } else {
                val sunriseSunsetModel = objectMapper.readValue(
                    SunriseSunsetApiTestData.apiResponse2025Jan3Json,
                    SunriseSunsetModel::class.java
                )
                CompletableFuture.completedFuture(sunriseSunsetModel)
            }
        }
    }

    afterTest {
        locationConfigRepository.deleteAll()
        entities.clear()
    }

    fun getYmdString(date: LocalDate): String {
        val year = date.year
        val month = if (date.monthValue < 10) "0${date.monthValue}" else date.monthValue.toString()
        val day = if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else date.dayOfMonth
        return "$year-$month-$day"
    }

    "Running the job for the first time" {
        val today = LocalDate.of(2025, 1, 2)
        val tomorrow = today.plusDays(1)
        job.run()

        val sunriseSunsetTimes = sunriseSunsetTimeRepository.findAllOrderByYmd(Pageable.from(0, 5))
        sunriseSunsetTimes.size shouldBe 2

        val todaySunriseSunsetTime = sunriseSunsetTimes.first()
        val tomorrowSunriseSunsetTime = sunriseSunsetTimes.last()

        todaySunriseSunsetTime.ymd shouldBe getYmdString(today)
        tomorrowSunriseSunsetTime.ymd shouldBe getYmdString(tomorrow)
    }

    "Running the job for the Nth time" {
        val mockSunriseSunsetRepo = getMock(sunriseSunsetTimeRepository)
        val mockClient = getMock(client)
        val today = LocalDate.of(2025, 1, 2)
        val tomorrow = today.plusDays(1)
        val demoSunriseSunsetEntity = SunriseSunsetTimeEntity(
            ymd = getYmdString(today),
            json = "{\"results\":{\"sunrise\":\"2025-01-02T15:54:02+00:00\",\"sunset\":\"2025-01-03T00:32:10+00:00\",\"solar_noon\":\"2025-01-02T20:13:06+00:00\",\"day_length\":31088,\"civil_twilight_begin\":\"2025-01-02T15:20:28+00:00\",\"civil_twilight_end\":\"2025-01-03T01:05:44+00:00\",\"nautical_twilight_begin\":\"2025-01-02T14:41:38+00:00\",\"nautical_twilight_end\":\"2025-01-03T01:44:34+00:00\",\"astronomical_twilight_begin\":\"2025-01-02T14:04:26+00:00\",\"astronomical_twilight_end\":\"2025-01-03T02:21:46+00:00\"},\"status\":\"OK\",\"tzid\":\"UTC\"}",
            location = demoLocationConfigEntity,
        )
        sunriseSunsetTimeRepository.save(demoSunriseSunsetEntity)
        verify(exactly = 1) {
            mockSunriseSunsetRepo.save(any())
        }

        job.run()

        val sunriseSunsetTimes = sunriseSunsetTimeRepository.findAllOrderByYmd(Pageable.from(0, 5))
        sunriseSunsetTimes.size shouldBe 2
        verify(exactly = 2) {
            mockSunriseSunsetRepo.save(any())
        }
        verify(exactly = 1) {
            mockClient.getSunriseSunsetTimes(any(), any(), any())
        }

        val tomorrowSunriseSunsetTime = sunriseSunsetTimes.last()
        tomorrowSunriseSunsetTime.ymd shouldBe getYmdString(tomorrow)
    }
}) {
    @MockBean(H2SunriseSunsetTimeRepository::class)
    fun sunriseSunsetRepository(): H2SunriseSunsetTimeRepository {
        return mockk()
    }

    @MockBean(SunriseSunsetApiClient::class)
    fun sunriseSunsetApiClient(): SunriseSunsetApiClient {
        return mockk()
    }
}