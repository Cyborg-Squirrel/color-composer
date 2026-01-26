package io.cyborgsquirrel.lighting.rendering.post_processing

import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.LedStripPoolModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameModel
import io.cyborgsquirrel.lighting.rendering.model.RenderedFrameSegmentModel
import org.slf4j.LoggerFactory

class FrameSegmentationHelper {

    fun segmentFrame(
        clientStrips: List<LedStripModel>,
        clientUuid: String,
        frame: RenderedFrameModel
    ): List<RenderedFrameSegmentModel> {
        if (!clientStrips.contains(frame.strip)) {
            logger.error("Frame strip ${frame.strip.uuid} is not in list of client strips ${clientStrips.map { it.uuid }}")
            throw Exception("None of the specified client LED strips match the frame's LED strip!")
        }
        return when (val frameStrip = frame.strip) {
            is SingleLedStripModel -> {
                val clientSingleStrip =
                    clientStrips.filterIsInstance<SingleLedStripModel>().first { it.uuid == frameStrip.uuid }
                listOf(RenderedFrameSegmentModel(clientSingleStrip, frame.sequenceNumber, frame.frameData))
            }

            is LedStripPoolModel -> {
                val clientSingleStrips = mutableListOf<SingleLedStripModel>()
                for (cs in clientStrips) {
                    when (cs) {
                        is SingleLedStripModel -> clientSingleStrips.add(cs)
                        is LedStripPoolModel -> clientSingleStrips.addAll(cs.strips.filter { it.clientUuid == clientUuid })
                    }
                }

                val frameList = mutableListOf<RenderedFrameSegmentModel>()
                var startIndex = 0
                for (fs in frameStrip.strips) {
                    for (cs in clientSingleStrips) {
                        if (cs == fs) {
                            val segmentData = frame.frameData.subList(startIndex, startIndex + cs.length)
                            frameList.add(
                                RenderedFrameSegmentModel(
                                    cs,
                                    frame.sequenceNumber,
                                    if (cs.inverted) segmentData.reversed() else segmentData
                                )
                            )
                            break
                        }
                    }
                    startIndex += fs.length
                }

                frameList
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FrameSegmentationHelper::class.java)
    }
}