package io.cyborgsquirrel.lighting.rendering

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.NightriderLightEffect
import io.cyborgsquirrel.lighting.effects.service.LightEffectRegistry
import io.cyborgsquirrel.lighting.effects.settings.NightriderColorFillEffectSettings
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.filters.IntensityFilter
import io.cyborgsquirrel.lighting.filters.LightEffectFilter
import io.cyborgsquirrel.lighting.filters.ReverseFilter
import io.cyborgsquirrel.lighting.filters.settings.IntensityFilterSettings
import io.cyborgsquirrel.lighting.model.LedStripModel
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.cyborgsquirrel.util.time.TimeHelper
import io.kotest.core.spec.style.StringSpec
import reactor.core.publisher.Flux
import java.lang.management.ManagementFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Phase-0 allocation baseline for the render pipeline (effect -> filters -> blend -> segment).
 *
 * Measures bytes allocated and wall time per rendered frame for a matrix of strip length x effect count x
 * filter count, so we can decide whether the buffer-reuse refactor is worth it and quantify each phase afterward.
 *
 * This is a benchmark, not a correctness test, so it is gated behind an env var to keep `./gradlew test` fast.
 * Run it with:
 *
 *     BENCH=true ./gradlew test --tests "*RenderAllocationBenchmark*" --info
 *
 * Notes on accuracy:
 *  - Allocation is read via [com.sun.management.ThreadMXBean.getThreadAllocatedBytes] on the thread running the loop,
 *    so only the render path is counted (no test-framework / mock noise).
 *  - The registry and time helper are handwritten fakes; mockk stubs allocate per call and would skew the count.
 *  - The time helper advances enough per call that every effect is "due" every frame, so we measure the worst case
 *    (a fully rendered frame) rather than the cache-hit fast path that the real wall clock would mostly hit.
 */
class RenderAllocationBenchmark : StringSpec({

    val benchEnabled = System.getenv("BENCH")?.lowercase() == "true"

    val clientUuid = "bench-client"
    val warmupFrames = 20_000
    val measureFrames = 50_000

    // Matrix to sweep. Adjust to match your real deployment.
    val stripLengths = listOf(60, 150, 300, 600)
    val effectCounts = listOf(1, 2, 4)
    val filterCounts = listOf(0, 2)

    "render allocation matrix".config(enabled = benchEnabled) {
        // Silence the renderer's per-frame WARN logging so log-formatting allocation doesn't pollute the count.
        // Done reflectively because logback is runtimeOnly and not on the test compile classpath.
        runCatching {
            val logger = org.slf4j.LoggerFactory.getLogger("io.cyborgsquirrel.lighting")
            val levelClass = Class.forName("ch.qos.logback.classic.Level")
            val off = levelClass.getField("OFF").get(null)
            logger.javaClass.getMethod("setLevel", levelClass).invoke(logger, off)
        }

        val threadMx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(threadMx.isThreadAllocatedMemorySupported) { "Per-thread allocation measurement not supported on this JVM" }
        threadMx.isThreadAllocatedMemoryEnabled = true
        val tid = Thread.currentThread().threadId()

        // Keep the JIT from eliminating the render call as dead code.
        var blackhole = 0L

        println()
        println("=== Render allocation baseline (per rendered frame) ===")
        println(String.format("%-6s %-8s %-8s %14s %12s %14s", "len", "effects", "filters", "bytes/frame", "ns/frame", "MB/s@60fps"))

        for (length in stripLengths) {
            for (effects in effectCounts) {
                for (filters in filterCounts) {
                    val strip = singleStrip(length)
                    val registry = StaticRegistry(strip, effects, filters)
                    val renderer = LightEffectRendererImpl(registry)
                    val strips = listOf<LedStripModel>(strip)

                    repeat(warmupFrames) { blackhole += renderer.renderFrames(strips, clientUuid).size }

                    val allocBefore = threadMx.getThreadAllocatedBytes(tid)
                    val timeBefore = System.nanoTime()
                    repeat(measureFrames) { blackhole += renderer.renderFrames(strips, clientUuid).size }
                    val timeAfter = System.nanoTime()
                    val allocAfter = threadMx.getThreadAllocatedBytes(tid)

                    val bytesPerFrame = (allocAfter - allocBefore) / measureFrames
                    val nsPerFrame = (timeAfter - timeBefore) / measureFrames
                    val mbPerSecAt60 = bytesPerFrame * 60.0 / 1_000_000.0

                    println(
                        String.format(
                            "%-6d %-8d %-8d %14d %12d %14.1f",
                            length, effects, filters, bytesPerFrame, nsPerFrame, mbPerSecAt60
                        )
                    )
                }
            }
        }

        println("(blackhole=$blackhole)")
        println("=======================================================")
    }
})

private fun singleStrip(length: Int) = SingleLedStripModel(
    name = "bench-strip",
    uuid = "bench-strip-$length",
    pin = "0",
    length = length,
    height = 1,
    blendMode = BlendMode.Layer,
    brightness = 100,
    clientUuid = "bench-client",
    inverted = false,
)

/**
 * Registry that returns a fixed set of real effects + filters for the one strip under test.
 * Handwritten so its method calls allocate nothing (unlike mockk stubs).
 */
private class StaticRegistry(
    strip: SingleLedStripModel,
    effectCount: Int,
    filterCount: Int,
) : LightEffectRegistry {

    private val timeHelper = FixedStepTimeHelper(stepMillis = 100)

    private val effects: List<ActiveLightEffect> = (0 until effectCount).map { i ->
        val effect = NightriderLightEffect(
            numberOfLeds = strip.length,
            settings = NightriderColorFillEffectSettings(),
            palette = null,
            timeHelper = timeHelper,
        )
        val filters: List<LightEffectFilter> = when (filterCount) {
            0 -> emptyList()
            else -> listOf(
                IntensityFilter(IntensityFilterSettings(intensity = 0.8f), "f-int-$i"),
                ReverseFilter("f-rev-$i"),
            ).take(filterCount)
        }
        ActiveLightEffect(
            effectUuid = "e-$i",
            layer = i,
            skipFramesIfBlank = false,
            status = LightEffectStatus.Playing,
            effect = effect,
            filters = filters,
            strip = strip,
        )
    }

    override fun getAllEffectsForStrip(stripUuid: String): List<ActiveLightEffect> = effects

    override val updates: Flux<List<ActiveLightEffect>> = Flux.empty()
    override fun addOrUpdateEffect(lightEffect: ActiveLightEffect) = throw NotImplementedError()
    override fun removeEffect(lightEffect: ActiveLightEffect) = throw NotImplementedError()
    override fun getEffectWithUuid(uuid: String): ActiveLightEffect = throw NotImplementedError()
    override fun getEffectsForClient(clientUuid: String): List<ActiveLightEffect> = throw NotImplementedError()
    override fun getAllEffects(): List<ActiveLightEffect> = effects
    override fun removeAllEffects() = throw NotImplementedError()
}

/**
 * Time helper whose clock jumps forward a fixed amount on every read, guaranteeing each effect's update is always
 * "due" so we measure a fully rendered frame rather than the cache-hit path.
 */
private class FixedStepTimeHelper(private val stepMillis: Long) : TimeHelper {
    private var millis = 0L
    override fun millisSinceEpoch(): Long {
        millis += stepMillis
        return millis
    }

    override fun today(): LocalDate = LocalDate.EPOCH
    override fun tomorrow(): LocalDate = LocalDate.EPOCH.plusDays(1)
    override fun now(): LocalDateTime = LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneOffset.UTC)
    override fun dateTimeFromMillis(millisSinceEpoch: Long): LocalDateTime =
        LocalDateTime.ofEpochSecond(millisSinceEpoch / 1000, 0, ZoneOffset.UTC)
    override fun utcTimestampToZoneDateTime(utcDateTimeString: String): ZonedDateTime =
        ZonedDateTime.now(ZoneOffset.UTC)
}
