package io.cyborgsquirrel.jobs.streaming

enum class StreamingJobState {
    // The websocket job needs a client and strip, until it has both it is in this state
    SetupIncomplete,

    // The job is waiting for the connection attempt to complete
    WaitingForConnection,

    // LedClient is connected but no effects are being rendered
    ConnectedIdle,

    // Send the strip configuration to the client
    SettingsSync,

    // LedClient is disconnected
    DisconnectedIdle,

    // Client's frame buffer is full, server pausing until it's time to send more frames
    BufferFullWaiting,

    // Server needs to re-sync time with the client
    TimeSyncRequired,

    // Rendering the effect(s) and sending them to the client
    RenderingEffect,
}