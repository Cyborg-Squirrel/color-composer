package io.cyborgsquirrel.lighting.job

enum class WebSocketState {
    // The websocket job needs a client and strip, until it has both it is in this state
    InsufficientData,

    // LedClient is connected but no effects are being rendered
    ConnectedIdle,

    // LedClient is disconnected
    DisconnectedIdle,

    // Client's frame buffer is full, server pausing until it's time to send more frames
    BufferFullWaiting,

    // Server needs to re-sync time with the client
    TimeSyncRequired,

    // Rendering the effect(s) and sending them to the client
    RenderingEffect,
}