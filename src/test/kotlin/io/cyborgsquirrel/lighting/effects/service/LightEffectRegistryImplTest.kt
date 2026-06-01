package io.cyborgsquirrel.lighting.effects.service

import io.cyborgsquirrel.lighting.effects.ActiveLightEffect
import io.cyborgsquirrel.lighting.effects.LightEffect
import io.cyborgsquirrel.lighting.enums.BlendMode
import io.cyborgsquirrel.lighting.enums.LightEffectStatus
import io.cyborgsquirrel.lighting.model.SingleLedStripModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class LightEffectRegistryImplTest : StringSpec({

    fun strip(uuid: String) = SingleLedStripModel("s", uuid, "0", 10, 1, BlendMode.Layer, 100, "client-1", false)

    fun activeEffect(uuid: String, strip: SingleLedStripModel) = ActiveLightEffect(
        effectUuid = uuid,
        layer = 0,
        skipFramesIfBlank = false,
        status = LightEffectStatus.Playing,
        effect = mockk<LightEffect>(),
        filters = emptyList(),
        strip = strip,
    )

    "indexes effects by strip uuid" {
        val registry = LightEffectRegistryImpl()
        val stripA = strip("strip-a")
        val stripB = strip("strip-b")
        val effectA = activeEffect("a", stripA)
        val effectB = activeEffect("b", stripB)

        registry.addOrUpdateEffect(effectA)
        registry.addOrUpdateEffect(effectB)

        registry.getAllEffectsForStrip("strip-a") shouldBe listOf(effectA)
        registry.getAllEffectsForStrip("strip-b") shouldBe listOf(effectB)
        registry.getAllEffectsForStrip("missing") shouldBe emptyList()
    }

    "returns a stable list instance until the effect set changes" {
        val registry = LightEffectRegistryImpl()
        val stripA = strip("strip-a")
        registry.addOrUpdateEffect(activeEffect("a", stripA))

        val first = registry.getAllEffectsForStrip("strip-a")
        val second = registry.getAllEffectsForStrip("strip-a")
        (first === second) shouldBe true

        // A change rebuilds the index, producing a new instance so the renderer knows to recompose.
        registry.addOrUpdateEffect(activeEffect("c", stripA))
        (registry.getAllEffectsForStrip("strip-a") === first) shouldBe false
    }
})
