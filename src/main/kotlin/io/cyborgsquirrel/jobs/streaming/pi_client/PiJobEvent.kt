package io.cyborgsquirrel.jobs.streaming.pi_client

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect

/**
 * Cross-thread signals processed by the [PiClientWebSocketJob] loop coroutine.
 *
 * All Reactor subscribers and WebSocket I/O callbacks funnel their notifications
 * through a single-consumer channel so the loop coroutine remains the sole owner
 * of mutable job state.
 */
internal sealed class PiJobEvent {
    /** A binary frame was received from the Pi over the WebSocket. */
    class ResponseReceived(val bytes: ByteArray) : PiJobEvent()

    /** The WebSocket session closed (peer close, network error, etc). */
    data object Disconnected : PiJobEvent()

    /** The set of active light effects changed. The handler filters for this client. */
    data class EffectsUpdated(val effects: List<ActiveLightEffect>) : PiJobEvent()
}
