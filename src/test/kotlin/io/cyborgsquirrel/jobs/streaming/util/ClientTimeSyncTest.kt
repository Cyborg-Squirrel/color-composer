package io.cyborgsquirrel.jobs.streaming.util

import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest

class ClientTimeSyncTest : StringSpec({
    "Initialize with timestamps set to 0" {
        val timeHelper = mockk<TimeHelper>()
        val clientTimeSync = ClientTimeSync(timeHelper)

        clientTimeSync.lastTimeSyncPerformedAt shouldBe 0L
        clientTimeSync.clientTimeOffset shouldBe 0L
    }

    "Perform time sync" {
        val timeHelper = mockk<TimeHelper>()
        val mockClientTime = 1640995200000L
        val requestTimestamp = 1640995200100L
        val responseTimestamp = 1640995200300L

        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            requestTimestamp,
            responseTimestamp
        )

        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { mockClientTime }
        }

        clientTimeSync.lastTimeSyncPerformedAt shouldBe responseTimestamp
        clientTimeSync.clientTimeOffset shouldNotBe 0L

        // Request/response timestamps
        verify(exactly = 2) { timeHelper.millisSinceEpoch() }
    }

    "Negative value from client time sync callback" {
        val timeHelper = mockk<TimeHelper>()
        val negativeClientTime = -1L

        coEvery { timeHelper.millisSinceEpoch() } returns 1640995200000L

        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { negativeClientTime }
        }

        // Should not update time sync values when callback returns negative value
        clientTimeSync.lastTimeSyncPerformedAt shouldBe 0L
        clientTimeSync.clientTimeOffset shouldBe 0L
    }

    "Client time behind server" {
        val timeHelper = mockk<TimeHelper>()
        val mockClientTime = 1640995200000L
        val requestTimestamp = 1640995200100L
        val responseTimestamp = 1640995200300L

        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            requestTimestamp,
            responseTimestamp
        )

        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { mockClientTime }
        }

        // requestResponseDuration = (300 - 100) / 2 = 100ms
        // adjustedClientTime = 1640995200000 - 100 = 1640995199900
        // clientTimeOffset = 1640995199900 - 1640995200300 = -400ms
        clientTimeSync.clientTimeOffset shouldBe -400L
    }

    "Client time ahead of server" {
        val timeHelper = mockk<TimeHelper>()
        val mockClientTime = 1640995201000L
        val requestTimestamp = 1640995200100L
        val responseTimestamp = 1640995200500L

        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            requestTimestamp,
            responseTimestamp
        )

        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { mockClientTime }
        }

        // requestResponseDuration = (500 - 100) / 2 = 200ms
        // adjustedClientTime = 1640995201000 - 200 = 1640995200800
        // clientTimeOffset = 1640995200800 - 1640995200500 = 300ms
        clientTimeSync.clientTimeOffset shouldBe 300L
    }

    "lastTimeSyncPerformedAt updated after multiple calls" {
        val timeHelper = mockk<TimeHelper>()
        val mockClientTime = 1640995200000L
        val firstRequestTimestamp = 1640995200100L
        val firstResponseTimestamp = 1640995200300L
        val secondRequestTimestamp = 1640995200400L
        val secondResponseTimestamp = 1640995200600L

        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            firstRequestTimestamp,
            firstResponseTimestamp,
            secondRequestTimestamp,
            secondResponseTimestamp
        )

        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { mockClientTime }
            clientTimeSync.doTimeSync { mockClientTime }
        }

        // Should be updated to the last sync time
        clientTimeSync.lastTimeSyncPerformedAt shouldBe secondResponseTimestamp
    }
})