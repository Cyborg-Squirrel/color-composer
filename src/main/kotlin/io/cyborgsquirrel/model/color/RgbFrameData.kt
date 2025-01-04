package io.cyborgsquirrel.model.color

/**
 * RGB frame to be streamed to a client over a WebSocket.
 *
 * [timestamp] is the millis since epoch time to show the frame.
 * If [timestamp] is set to 0 the frame will be rendered instantly.
 *
 * [rgbData] are the NeoPixel RGB values to be displayed on the strip in the order
 * they should be displayed.
 */
data class RgbFrameData(val timestamp: Long = 0, val rgbData: List<RgbColor>)