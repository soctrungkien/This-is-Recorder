package com.zeroui.kit.glass

import androidx.compose.runtime.Stable
import com.kyant.backdrop.backdrops.LayerBackdrop as LiquidLayer
import top.yukonga.miuix.kmp.blur.LayerBackdrop as BlurLayer

/** Sampling layer that Liquid Glass surfaces refract. */
@Stable
class LiquidBackdrop internal constructor(internal val layer: LiquidLayer)

/** Sampling layer that blurred surfaces read. */
@Stable
class BlurBackdrop internal constructor(internal val layer: BlurLayer)
