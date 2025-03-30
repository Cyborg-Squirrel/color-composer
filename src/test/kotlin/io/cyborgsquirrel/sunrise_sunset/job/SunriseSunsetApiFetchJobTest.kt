package io.cyborgsquirrel.sunrise_sunset.job

import io.cyborgsquirrel.sunrise_sunset.entity.LocationConfigEntity
import io.cyborgsquirrel.sunrise_sunset.entity.SunriseSunsetTimeEntity
import io.cyborgsquirrel.sunrise_sunset.client.SunriseSunsetApiClient
import io.cyborgsquirrel.sunrise_sunset.model.SunriseSunsetModel
import io.cyborgsquirrel.sunrise_sunset.repository.H2LocationConfigRepository
import io.cyborgsquirrel.sunrise_sunset.repository.H2SunriseSunsetTimeRepository
import io.cyborgsquirrel.util.time.TimeHelper
import io.cyborgsquirrel.util.time.TimeHelperImpl
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture

@MicronautTest(startApplication = false, transactional = false)
class SunriseSunsetApiFetchJobTest(
    private val job: SunriseSunsetApiFetchJob,
    private val locationConfigRepository: H2LocationConfigRepository,
    private val sunriseSunsetTimeRepository: H2SunriseSunsetTimeRepository,
    private val client: SunriseSunsetApiClient,
    private val objectMapper: ObjectMapper,
    private val timeHelper: TimeHelper,
) : StringSpec({

    // London
    val demoLocationConfigEntity = LocationConfigEntity(latitude = "51.3026", longitude = "7.39", active = true)
    // Perth
    val demoLocationInactiveConfigEntity =
        LocationConfigEntity(latitude = "-31.5721", longitude = "115.5135", active = false)
    val demoTodayDate = LocalDate.of(2025, 1, 2)
    val demoTomorrowDate = LocalDate.of(2025, 1, 3)
    val sunriseSunsetEntities = mutableListOf<SunriseSunsetTimeEntity>()
    val locationConfigEntities = mutableListOf<LocationConfigEntity>()

    beforeTest {
        // Mock out time
        val mockTimeHelper = getMock(timeHelper)
        every {
            mockTimeHelper.today()
        } answers {
            demoTodayDate
        }
        every {
            mockTimeHelper.tomorrow()
        } answers {
            demoTomorrowDate
        }
        every {
            mockTimeHelper.utcTimestampToZoneDateTime(any())
        } answers {
            ZonedDateTime.parse(it.invocation.args.first() as String, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(
                    TimeZone.getDefault().toZoneId()
                )
        }

        // Mock out data access
        val mockLocationConfigRepo = getMock(locationConfigRepository)
        every {
            mockLocationConfigRepo.save(any())
        } answers {
            val entity = it.invocation.args.first() as LocationConfigEntity
            locationConfigEntities.add(entity)
            entity
        }
        every {
            mockLocationConfigRepo.findByActiveTrue()
        } answers {
            val matchingEntity = locationConfigEntities.firstOrNull { e ->
                e.active == true
            }
            Optional.ofNullable(matchingEntity)
        }

        val mockSunriseSunsetRepo = getMock(sunriseSunsetTimeRepository)
        every {
            mockSunriseSunsetRepo.save(any())
        } answers {
            val entity = it.invocation.args.first() as SunriseSunsetTimeEntity
            sunriseSunsetEntities.add(entity)
            entity
        }
        every {
            mockSunriseSunsetRepo.findByYmdEqualsAndLocationEquals(any(), any())
        } answers {
            val ymdString = it.invocation.args.first() as String
            val location = it.invocation.args.last() as LocationConfigEntity
            val matchingEntity = sunriseSunsetEntities.firstOrNull { e ->
                e.ymd == ymdString && e.location == location
            }
            Optional.ofNullable(matchingEntity)
        }
        every {
            mockSunriseSunsetRepo.findAllOrderByYmd(any())
        } answers {
            sunriseSunsetEntities.sortedBy { e ->
                e.ymd
            }
        }

        // Mock out API calls
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

        // Default data
        locationConfigRepository.save(demoLocationConfigEntity)
        locationConfigRepository.save(demoLocationInactiveConfigEntity)
    }

    afterTest {
        sunriseSunsetEntities.clear()
    }

    fun getYmdString(date: LocalDate): String {
        val year = date.year
        val month = if (date.monthValue < 10) "0${date.monthValue}" else date.monthValue.toString()
        val day = if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else date.dayOfMonth
        return "$year-$month-$day"
    }

    "Running the job for the first time" {
        job.run()

        val sunriseSunsetTimes = sunriseSunsetTimeRepository.findAllOrderByYmd(Pageable.from(0, 5))
        sunriseSunsetTimes.size shouldBe 2

        val todaySunriseSunsetTime = sunriseSunsetTimes.first()
        val tomorrowSunriseSunsetTime = sunriseSunsetTimes.last()

        todaySunriseSunsetTime.ymd shouldBe getYmdString(demoTodayDate)
        tomorrowSunriseSunsetTime.ymd shouldBe getYmdString(demoTomorrowDate)
    }

    "Running the job for the Nth time" {
        val mockSunriseSunsetRepo = getMock(sunriseSunsetTimeRepository)
        val mockClient = getMock(client)
        val demoSunriseSunsetEntity = SunriseSunsetTimeEntity(
            ymd = getYmdString(demoTodayDate),
            json = "{\"results\":{\"sunrise\":\"2025-01-02T15:54:02+00:00\",\"sunset\":\"2025-01-03T00:32:10+00:00\",\"solar_noon\":\"2025-01-02T20:13:06+00:00\",\"day_length\":31088,\"civil_twilight_begin\":\"2025-01-02T15:20:28+00:00\",\"civil_twilight_end\":\"2025-01-03T01:05:44+00:00\",\"nautical_twilight_begin\":\"2025-01-02T14:41:38+00:00\",\"nautical_twilight_end\":\"2025-01-03T01:44:34+00:00\",\"astronomical_twilight_begin\":\"2025-01-02T14:04:26+00:00\",\"astronomical_twilight_end\":\"2025-01-03T02:21:46+00:00\"},\"status\":\"OK\",\"tzid\":\"UTC\"}",
            location = demoLocationConfigEntity,
        )
        sunriseSunsetTimeRepository.save(demoSunriseSunsetEntity)
        verify(exactly = 1) {
            mockSunriseSunsetRepo.save(any())
        }

        job.run()

        val sunriseSunsetTimes = sunriseSunsetTimeRepository.findAllOrderByYmd(Pageable.from(0, 5))
        // Verify this is 2, today's sunrise/sunset data shouldn't be re-requested if it already exists in the db
        sunriseSunsetTimes.size shouldBe 2
        verify(exactly = 2) {
            mockSunriseSunsetRepo.save(any())
        }
        verify(exactly = 1) {
            mockClient.getSunriseSunsetTimes(any(), any(), any())
        }

        val tomorrowSunriseSunsetTime = sunriseSunsetTimes.last()
        tomorrowSunriseSunsetTime.ymd shouldBe getYmdString(demoTomorrowDate)
    }
}) {
    @MockBean(H2SunriseSunsetTimeRepository::class)
    fun sunriseSunsetRepository(): H2SunriseSunsetTimeRepository {
        return mockk()
    }

    @MockBean(H2LocationConfigRepository::class)
    fun locationConfigRepository(): H2LocationConfigRepository {
        return mockk()
    }

    @MockBean(SunriseSunsetApiClient::class)
    fun sunriseSunsetApiClient(): SunriseSunsetApiClient {
        return mockk()
    }

    @MockBean(TimeHelperImpl::class)
    fun timeHelper(): TimeHelper {
        return mockk()
    }
}