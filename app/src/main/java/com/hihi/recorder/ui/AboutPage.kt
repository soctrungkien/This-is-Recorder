@file:OptIn(ExperimentalScrollBarApi::class)

package com.hihi.recorder.ui

import android.os.SystemClock
import android.widget.Toast
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults as MaterialCardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hihi.recorder.BuildConfig
import com.hihi.recorder.R
import com.hihi.recorder.data.UiStyle
import com.hihi.recorder.ui.effect.BgEffectBackground
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi

/**
 * About screen modelled on the Miuix reference page in `miuix-readonly`:
 * a hero header over the HyperOS 3 animated gradient, followed by
 * preference-style cards. When blur is enabled, the top bar and cards sample
 * the shader layer with [textureBlur] and become translucent glass panels. The
 * shader is recorded into its own backdrop, so the glass consumers are siblings
 * of the recorded layer instead of descendants of it.
 */
@Composable
fun AboutPage(
    snackbarHostState: SnackbarHostState,
    blurEnabled: Boolean,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowHelp: () -> Unit,
    onOpenMitLicense: () -> Unit,
    onOpenApacheLicense: () -> Unit,
    onOpenShizukuLicense: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenShowcase: () -> Unit,
) {
    val palette = appPalette()
    val effectSupported = remember { isRuntimeShaderSupported() }
    val effectBackdrop = if (blurEnabled && effectSupported) {
        rememberLayerBackdrop()
    } else {
        null
    }
    val bottomContentPadding = appBottomContentPadding()
    val listState = rememberLazyListState()

    // Easter egg: five taps on the app logo open the component showcase.
    var logoTaps by remember { mutableIntStateOf(0) }
    var lastLogoTap by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val onLogoTap = {
        val now = SystemClock.elapsedRealtime()
        logoTaps = if (now - lastLogoTap <= 2_000L) logoTaps + 1 else 1
        lastLogoTap = now
        val random = (1..10).map { ('a'.code + Random.nextInt(26)).toChar() }.joinToString("")
        Toast.makeText(context, random.toMorse(), Toast.LENGTH_SHORT).show()
        if (logoTaps >= 5) {
            logoTaps = 0
            onOpenShowcase()
        }
        Unit
    }
    // Same 16 dp corners as the reference About page in `miuix-readonly`.
    val cardCornerRadius = 16.dp
    val cardShape = RoundedCornerShape(cardCornerRadius)
    val cardBlurColors = if (effectBackdrop != null) {
        BlurDefaults.blurColors(
            blendColors = listOf(
                BlendColorEntry(
                    palette.surface.copy(alpha = if (palette.isDark) 0.55f else 0.65f),
                ),
            ),
        )
    } else {
        null
    }

    BgEffectBackground(
        dynamicBackground = effectSupported,
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (effectBackdrop != null) {
            Modifier.layerBackdrop(effectBackdrop)
        } else {
            Modifier
        },
        isFullSize = true,
    ) {
        CompositionLocalProvider(LocalAppBackdrop provides effectBackdrop) {
            AppScaffold(
                title = stringResource(R.string.section_about),
                subtitle = stringResource(R.string.about_subtitle),
                snackbarHostState = snackbarHostState,
                containerColor = if (effectSupported) Color.Transparent else palette.background,
                navigationIcon = {
                    AppIconButton(
                        icon = MiuixIcons.Back,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                    )
                },
            ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { AboutHero(onLogoTap = onLogoTap) }

                item {
                    AboutCard(
                        backdrop = effectBackdrop,
                        shape = cardShape,
                        cornerRadius = cardCornerRadius,
                        blurColors = cardBlurColors,
                    ) {
                        AppRow(
                            title = stringResource(R.string.about_source),
                            summary = stringResource(R.string.about_source_summary),
                            leadingIcon = MiuixIcons.Community,
                            onClick = {
                                onOpenUrl(
                                    "https://github.com/soctrungkien/This-is-Recorder",
                                )
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_telegram),
                            summary = stringResource(R.string.pref_telegram_summary),
                            leadingIcon = MiuixIcons.Send,
                            onClick = { onOpenUrl("https://t.me/realknoname") },
                        )
                    }
                }

                item {
                    AboutCard(
                        backdrop = effectBackdrop,
                        shape = cardShape,
                        cornerRadius = cardCornerRadius,
                        blurColors = cardBlurColors,
                    ) {
                        AppRow(
                            title = stringResource(R.string.about_license_mit),
                            summary = stringResource(R.string.about_license_mit_summary),
                            leadingIcon = MiuixIcons.File,
                            onClick = onOpenMitLicense,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_license_apache),
                            summary = stringResource(R.string.about_license_apache_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = onOpenApacheLicense,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.pref_help),
                            summary = stringResource(R.string.help_title),
                            leadingIcon = MiuixIcons.Help,
                            onClick = onShowHelp,
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.about_credits)) }

                item {
                    AboutCard(
                        backdrop = effectBackdrop,
                        shape = cardShape,
                        cornerRadius = cardCornerRadius,
                        blurColors = cardBlurColors,
                    ) {
                        AppRow(
                            title = stringResource(R.string.about_developer),
                            summary = stringResource(R.string.about_developer_summary),
                            leadingIcon = MiuixIcons.Community,
                            onClick = { onOpenUrl("https://github.com/soctrungkien") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_design),
                            summary = stringResource(R.string.about_design_summary),
                            leadingIcon = MiuixIcons.Theme,
                        )
                    }
                }

                item { AppSectionTitle(stringResource(R.string.about_open_source)) }

                item {
                    AboutCard(
                        backdrop = effectBackdrop,
                        shape = cardShape,
                        cornerRadius = cardCornerRadius,
                        blurColors = cardBlurColors,
                    ) {
                        AppRow(
                            title = stringResource(R.string.about_miuix),
                            summary = stringResource(R.string.about_miuix_summary),
                            leadingIcon = MiuixIcons.Layers,
                            onClick = {
                                onOpenUrl("https://github.com/compose-miuix-ui/miuix")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_liquid_glass),
                            summary = stringResource(R.string.about_liquid_glass_summary),
                            leadingIcon = MiuixIcons.Background,
                            onClick = {
                                onOpenUrl("https://github.com/Kyant0/AndroidLiquidGlass")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_zeroui),
                            summary = stringResource(R.string.about_zeroui_summary),
                            leadingIcon = MiuixIcons.Theme,
                            onClick = { onOpenUrl("https://github.com/Xocio/ZeroUI") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_crash_handler),
                            summary = stringResource(R.string.about_crash_handler_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = {
                                onOpenUrl("https://github.com/Bhuvaneshw/CrashHandler")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_coui),
                            summary = stringResource(R.string.about_coui_summary),
                            leadingIcon = MiuixIcons.Theme,
                            onClick = { onOpenUrl("https://github.com/ItosEO/COUI-Kit") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_icon_packs),
                            summary = stringResource(R.string.about_icon_packs_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = { onOpenUrl("https://lucide.dev/") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_oneui_icons),
                            summary = stringResource(R.string.about_oneui_icons_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = {
                                onOpenUrl("https://github.com/OneUIProject/oneui-icons")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_remix_icon),
                            summary = stringResource(R.string.about_remix_icon_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = { onOpenUrl("https://remixicon.com/") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_framework),
                            summary = stringResource(R.string.about_framework_summary),
                            leadingIcon = MiuixIcons.Theme,
                            onClick = { onOpenUrl("https://developer.android.com/compose") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_material3),
                            summary = stringResource(R.string.about_material3_summary),
                            leadingIcon = MiuixIcons.Theme,
                            onClick = {
                                onOpenUrl(
                                    "https://developer.android.com/" +
                                        "jetpack/compose/designsystems/material3",
                                )
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_androidx),
                            summary = stringResource(R.string.about_androidx_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = {
                                onOpenUrl("https://developer.android.com/jetpack/androidx")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_shizuku),
                            summary = stringResource(R.string.about_shizuku_summary),
                            leadingIcon = MiuixIcons.Info,
                            onClick = onOpenShizukuLicense,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.licenses_title),
                            summary = stringResource(R.string.licenses_subtitle),
                            leadingIcon = MiuixIcons.File,
                            onClick = onOpenLicenses,
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_kotlin),
                            summary = stringResource(R.string.about_kotlin_summary),
                            leadingIcon = MiuixIcons.File,
                            onClick = { onOpenUrl("https://kotlinlang.org") },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_coroutines),
                            summary = stringResource(R.string.about_coroutines_summary),
                            leadingIcon = MiuixIcons.Layers,
                            onClick = {
                                onOpenUrl("https://github.com/Kotlin/kotlinx.coroutines")
                            },
                        )
                        AppDivider()
                        AppRow(
                            title = stringResource(R.string.about_serialization),
                            summary = stringResource(R.string.about_serialization_summary),
                            leadingIcon = MiuixIcons.File,
                            onClick = {
                                onOpenUrl("https://github.com/Kotlin/kotlinx.serialization")
                            },
                        )
                    }
                }
            }

            if (LocalUiStyle.current == UiStyle.MIUIX) {
                VerticalScrollBar(
                    adapter = rememberScrollBarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    trackPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = bottomContentPadding,
                    ),
                )
            }
        }
            }
        }
    }
}

private val MorseCodes = mapOf(
    'a' to ".-", 'b' to "-...", 'c' to "-.-.", 'd' to "-..", 'e' to ".",
    'f' to "..-.", 'g' to "--.", 'h' to "....", 'i' to "..", 'j' to ".---",
    'k' to "-.-", 'l' to ".-..", 'm' to "--", 'n' to "-.", 'o' to "---",
    'p' to ".--.", 'q' to "--.-", 'r' to ".-.", 's' to "...", 't' to "-",
    'u' to "..-", 'v' to "...-", 'w' to ".--", 'x' to "-..-", 'y' to "-.--",
    'z' to "--..",
)

private fun String.toMorse(): String = map { MorseCodes[it] ?: "" }.joinToString(" ")

@Composable
private fun AboutHero(onLogoTap: () -> Unit) {
    val palette = appPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.primary)
                .clickable(onClick = onLogoTap),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_about_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.onPrimary),
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        AppText(
            text = stringResource(R.string.app_name),
            style = AppTextStyle.Display,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        AppText(
            text = stringResource(R.string.app_subtitle),
            style = AppTextStyle.SmallBody,
            color = palette.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        AppText(
            text = stringResource(
                R.string.about_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = AppTextStyle.Footnote,
            color = palette.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutCard(
    backdrop: LayerBackdrop?,
    shape: Shape,
    cornerRadius: Dp,
    blurColors: BlurColors?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = appPalette()
    val blurModifier = if (backdrop != null && blurColors != null) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = 60f,
            colors = blurColors,
        )
    } else {
        Modifier
    }
    val containerColor = if (backdrop != null) Color.Transparent else palette.surfaceContainer

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        MiuixCard(
            modifier = Modifier
                .fillMaxWidth()
                .then(blurModifier),
            cornerRadius = cornerRadius,
            insideMargin = PaddingValues(0.dp),
            colors = MiuixCardDefaults.defaultColors(
                color = containerColor,
                contentColor = palette.onSurface,
            ),
        ) {
            Column(content = content)
        }
    } else {
        MaterialCard(
            modifier = Modifier
                .fillMaxWidth()
                .then(blurModifier),
            shape = shape,
            colors = MaterialCardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = palette.onSurface,
            ),
        ) {
            Column(content = content)
        }
    }
}
