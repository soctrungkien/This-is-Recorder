@file:OptIn(ExperimentalScrollBarApi::class)

package com.hihi.recorder.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hihi.recorder.BuildConfig
import com.hihi.recorder.R
import com.hihi.recorder.data.UiStyle
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
 * a hero header followed by preference-style cards. When the device supports
 * RuntimeShader and the blur setting is on, the cards sample the app backdrop
 * with [textureBlur] and become translucent glass panels.
 */
@Composable
fun AboutPage(
    snackbarHostState: SnackbarHostState,
    backdrop: LayerBackdrop?,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onShowHelp: () -> Unit,
    onOpenMitLicense: () -> Unit,
    onOpenApacheLicense: () -> Unit,
) {
    val palette = appPalette()
    val bottomContentPadding = appBottomContentPadding()
    val listState = rememberLazyListState()
    // Same 16 dp corners as the reference About page in `miuix-readonly`.
    val cardCornerRadius = 16.dp
    val cardShape = RoundedCornerShape(cardCornerRadius)
    val cardBlurColors = if (backdrop != null) {
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

    AppScaffold(
        title = stringResource(R.string.section_about),
        subtitle = stringResource(R.string.about_subtitle),
        snackbarHostState = snackbarHostState,
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
                item { AboutHero() }

                item {
                    AboutCard(
                        backdrop = backdrop,
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
                        backdrop = backdrop,
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
                        backdrop = backdrop,
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
                        backdrop = backdrop,
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

@Composable
private fun AboutHero() {
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
                .background(palette.primary),
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
