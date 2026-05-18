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

        clientTimeSync.mostRecentTimeSyncPerformedAt shouldBe 0L
        clientTimeSync.mostRecentClientTimeOffset shouldBe 0L
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

        clientTimeSync.mostRecentTimeSyncPerformedAt shouldBe responseTimestamp
        clientTimeSync.mostRecentClientTimeOffset shouldNotBe 0L

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
        clientTimeSync.mostRecentTimeSyncPerformedAt shouldBe 0L
        clientTimeSync.mostRecentClientTimeOffset shouldBe 0L
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
        clientTimeSync.mostRecentClientTimeOffset shouldBe -400L
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
        clientTimeSync.mostRecentClientTimeOffset shouldBe 300L
    }

    "Millis between newest and oldest time sync returns 0 when there have been fewer than two time syncs" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(1640995200100L, 1640995200300L)
        val clientTimeSync = ClientTimeSync(timeHelper)

        // Time sync has never been performed, should be 0
        clientTimeSync.millisBetweenNewestAndOldestTimeSync() shouldBe 0L

        runTest { clientTimeSync.doTimeSync { 1640995200000L } }
        clientTimeSync.millisBetweenNewestAndOldestTimeSync() shouldBe 0L
    }

    "Millis between newest and oldest time sync returns difference between last and first performedAt" {
        val timeHelper = mockk<TimeHelper>()
        // Each doTimeSync uses two millisSinceEpoch() calls (request + response).
        // responseTimestamp (second call) becomes performedAt in the history entry.
        // first entry performedAt = 200, second = 600, third = 1000
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            1640995200100L, 1640995200200L,
            1640995200500L, 1640995200600L,
            1640995200900L, 1640995201000L,
        )
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            repeat(3) { clientTimeSync.doTimeSync { 1640995200000L } }
        }

        clientTimeSync.millisBetweenNewestAndOldestTimeSync() shouldBe 800L
    }

    "Time sync history starts empty" {
        val timeHelper = mockk<TimeHelper>()
        val clientTimeSync = ClientTimeSync(timeHelper)

        clientTimeSync.timeSyncHistory shouldBe emptyList()
    }

    "Time sync history contains entry after sync" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(1640995200100L, 1640995200300L)
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest { clientTimeSync.doTimeSync { 1640995200000L } }

        clientTimeSync.timeSyncHistory.size shouldBe 1
        clientTimeSync.timeSyncHistory[0] shouldBe TimeSyncRecord(1640995200300L, -400L, 100)
    }

    "Time sync history skipped when callback returns negative value" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returns 1640995200000L
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest { clientTimeSync.doTimeSync { -1L } }

        clientTimeSync.timeSyncHistory shouldBe emptyList()
    }

    "Time sync history keeps last 5 entries" {
        val timeHelper = mockk<TimeHelper>()
        val timestamps = (1..12).map { 1640995200000L + it * 100L }
        coEvery { timeHelper.millisSinceEpoch() } returnsMany timestamps
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            repeat(6) { clientTimeSync.doTimeSync { 1640995200000L } }
        }

        // Note: timeHelper.millisSinceEpoch() is called twice per time sync, once before sending request to the client
        // and once when the response is received.
        clientTimeSync.timeSyncHistory.size shouldBe 5
        clientTimeSync.timeSyncHistory.first().performedAt shouldBe timestamps[3]
        clientTimeSync.timeSyncHistory.last().performedAt shouldBe timestamps[11]
    }

    "Average client time offset returns 0 when history is empty" {
        val timeHelper = mockk<TimeHelper>()
        val clientTimeSync = ClientTimeSync(timeHelper)

        clientTimeSync.averageClientTimeOffset() shouldBe 0L
    }

    "Average client time offset returns single value when history has one entry" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(1640995200100L, 1640995200300L)
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest { clientTimeSync.doTimeSync { 1640995200000L } }

        // clientTimeOffset = -400
        clientTimeSync.averageClientTimeOffset() shouldBe -400L
    }

    "Average client time offset averages multiple entries" {
        val timeHelper = mockk<TimeHelper>()
        // first sync:  latency=(300-100)/2=100, adjustedClient=200000-100=199900, offset=199900-200300=-400
        // second sync: latency=(100-100)/2=0,   adjustedClient=200200-0=200200,   offset=200200-200100=100
        // average: (-400 + 100) / 2 = -150
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            1640995200100L, 1640995200300L,
            1640995200100L, 1640995200100L,
        )
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { 1640995200000L }
            clientTimeSync.doTimeSync { 1640995200200L }
        }

        clientTimeSync.averageClientTimeOffset() shouldBe -150L
    }

    "Average client time offset does not overflow with large Long values" {
        val timeHelper = mockk<TimeHelper>()
        // serverTime=0 so networkLatency=0; offset = clientTime - 0 = clientTime
        // Two offsets near Long.MAX_VALUE whose naive sum overflows; average should still be correct
        val bigOffset = Long.MAX_VALUE - 100
        val serverTime = 0L

        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(
            serverTime, serverTime,
            serverTime, serverTime,
        )
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            clientTimeSync.doTimeSync { bigOffset }
            clientTimeSync.doTimeSync { bigOffset }
        }

        clientTimeSync.averageClientTimeOffset() shouldBe bigOffset
    }

    "Median client time offset returns 0 when history is empty" {
        val timeHelper = mockk<TimeHelper>()
        val clientTimeSync = ClientTimeSync(timeHelper)

        clientTimeSync.medianClientTimeOffset() shouldBe 0L
    }

    "Median client time offset returns single value when history has one entry" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returnsMany listOf(1640995200100L, 1640995200300L)
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest { clientTimeSync.doTimeSync { 1640995200000L } }

        // clientTimeOffset = -400
        clientTimeSync.medianClientTimeOffset() shouldBe -400L
    }

    "Median client time offset with five entries returns value at ceil(n/2) index of sorted offsets" {
        val timeHelper = mockk<TimeHelper>()
        coEvery { timeHelper.millisSinceEpoch() } returns 0L
        val clientTimeSync = ClientTimeSync(timeHelper)

        runTest {
            listOf(50L, 10L, 30L, 70L, 20L).forEach { clientTime ->
                clientTimeSync.doTimeSync { clientTime }
            }
        }

        // sorted offsets: [10, 20, 30, 50, 70] → ceil(5/2)=3 → index 3 = 50
        clientTimeSync.medianClientTimeOffset() shouldBe 50L
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
        clientTimeSync.mostRecentTimeSyncPerformedAt shouldBe secondResponseTimestamp
    }
})