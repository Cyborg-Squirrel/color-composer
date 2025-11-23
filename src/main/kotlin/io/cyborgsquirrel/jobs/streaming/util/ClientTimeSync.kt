package io.cyborgsquirrel.jobs.streaming.util

import io.cyborgsquirrel.util.time.TimeHelper
import org.slf4j.LoggerFactory

/**
 * A utility class for syncing client time with server time.
 *
 * The time synchronization algorithm assumes symmetric network latency (same time for
 * request and response transmission) and calculates the offset by:
 * 1. Recording the server timestamp before the request
 * 2. Getting the client timestamp from the callback
 * 3. Recording the server timestamp after the response
 * 4. Calculating the network latency
 * 5. Calculating the difference between the Color Composer client time to relative to the server time
 *
 * @property lastTimeSyncPerformedAt The server time in milliseconds since epoch when the
 *                                   most recent time sync was performed. This represents
 *                                   the time when the synchronization was completed.
 * @property clientTimeOffset The difference between client time and server time in milliseconds.
 *                            A positive value means the client's clock is ahead of the server,
 *                            while a negative value means the client's clock is behind the server.
 *
 * @constructor Creates a new [ClientTimeSync] instance with the specified time helper.
 * @param timeHelper The [TimeHelper] used to get server time.
 */
class ClientTimeSync(private val timeHelper: TimeHelper) {
    var lastTimeSyncPerformedAt = 0L
        private set

    var clientTimeOffset = 0L
        private set

    /**
     * Performs time synchronization with the server.
     *
     * This method makes a round-trip request to the server to calculate the time offset.
     * The server timestamp is obtained through the provided callback function.
     *
     * @param requestClientTimeCallback A callback function that returns the current client timestamp
     *                                  in milliseconds since epoch. This callback is called at
     *                                  the beginning and end of the synchronization process
     *                                  to measure network latency.
     *
     * @throws Exception if the time synchronization process fails
     */
    suspend fun doTimeSync(requestClientTimeCallback: suspend () -> Long) {
        val requestTimestamp = timeHelper.millisSinceEpoch()
        val clientTimestamp = requestClientTimeCallback()

        if (clientTimestamp < 0) {
            logger.info("Skipping time sync. Received client time: $clientTimestamp.")
            return
        }

        val responseTimestamp = timeHelper.millisSinceEpoch()

        // Assume request and response take the same amount of time for the network
        // to transmit. Divide by 2 so we only count the transmission time going one way.
        val networkLatency = (responseTimestamp - requestTimestamp) / 2
        val adjustedClientTime = clientTimestamp - networkLatency
        clientTimeOffset = adjustedClientTime - responseTimestamp

        lastTimeSyncPerformedAt = responseTimestamp
        logger.info("Client time sync complete. Offset in millis: $clientTimeOffset")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientTimeSync::class.java)
    }
}