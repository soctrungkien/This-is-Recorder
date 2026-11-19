package com.zeroui.kit.effect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.zeroui.kit.glass.isLiquidGlassSupported
import kotlin.math.pow

/** Duration of one ripple. */
const val LiquidRippleMillis = 1300

/** AGSL ring lens applied to a whole layer. */
private const val ShaderSource = """
uniform shader content;
uniform float2 center;
uniform float radius;
uniform float band;
uniform float strength;
uniform float fade;

half4 main(float2 coord) {
    float2 v = coord - center;
    float d = length(v);
    float x = (d - radius) / band;
    if (abs(x) >= 1.0) {
        return content.eval(coord);
    }
    float2 dir = v / max(d, 1.0);
    float env = 1.0 - x * x;
    float2 o = dir * (strength * sin(x * 3.14159265) * env);
    half4 g = content.eval(coord - o);
    half r = content.eval(coord - o * 1.08).r;
    half b = content.eval(coord - o * 0.92).b;
    half4 c = half4(r, g.g, b, g.a);
    float light = 0.65 + 0.35 * dot(dir, float2(-0.6, -0.8));
    float crest = exp(-x * x * 16.0) * light;
    half white = half((0.12 * env + 0.6 * crest) * fade * g.a);
    c.rgb = mix(c.rgb, half3(1.0) * g.a, white);
    return c;
}
"""

/** Holder of the ripple shader. */
class LiquidRipple internal constructor(private val shader: Any) {
    /** Returns the render effect at progress [p], or null outside the animation. */
    fun effect(
        p: Float,
        center: Offset,
        minRadius: Float,
        maxRadius: Float,
        density: Float,
    ): androidx.compose.ui.graphics.RenderEffect? {
        if (p <= 0f || p >= 1f) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val s = shader as RuntimeShader
        val e = 1f - (1f - p).pow(3)
        val fade = (1f - p).pow(1.1f)
        s.setFloatUniform("center", center.x, center.y)
        s.setFloatUniform("radius", minRadius + e * (maxRadius - minRadius))
        s.setFloatUniform("band", (22f + 18f * p) * density)
        s.setFloatUniform("strength", 22f * density * fade)
        s.setFloatUniform("fade", fade)
        return RenderEffect.createRuntimeShaderEffect(s, "content").asComposeRenderEffect()
    }
}

/** Returns the ripple on Android 13 and above; null below. */
@Composable
fun rememberLiquidRipple(): LiquidRipple? {
    val supported = isLiquidGlassSupported()
    return remember(supported) {
        if (supported) {
            LiquidRipple(RuntimeShader(ShaderSource))
        } else null
    }
}
