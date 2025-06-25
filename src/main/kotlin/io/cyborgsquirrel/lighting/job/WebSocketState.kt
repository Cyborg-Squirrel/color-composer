package io.cyborgsquirrel.lighting.job

enum class WebSocketState {
    // This state will be removed when WebSocketJobs are spawned by creating
    // a LedClient via the API or from the database at startup.
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