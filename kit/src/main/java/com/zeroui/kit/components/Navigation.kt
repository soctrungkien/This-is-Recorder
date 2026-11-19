package com.zeroui.kit.components

import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem
import androidx.compose.material3.NavigationRailItem as MaterialNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.zeroui.kit.theme.isMaterial
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.NavigationBar as MaterialNavigationBar
import androidx.compose.material3.NavigationRail as MaterialNavigationRail
import androidx.compose.material3.Text as MaterialText
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail

/** One destination of a navigation bar or rail. */
@Immutable
data class NavBarItem(
    val label: String,
    val icon: ImageVector,
)

/** True when the window is wide enough for a navigation rail. */
@Composable
fun rememberIsWideWindow(): Boolean {
    val width = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { width.toDp() } >= 600.dp
}

/** Bottom navigation; [floating] applies to the Miuix style. */
@Composable
fun ZeroNavigationBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    when {
        isMaterial() -> MaterialNavigationBar(modifier = modifier) {
            items.forEachIndexed { index, item ->
                MaterialNavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = { MaterialIcon(imageVector = item.icon, contentDescription = null) },
                    label = { MaterialText(item.label) },
                )
            }
        }
        floating -> FloatingNavigationBar(modifier = modifier) {
            items.forEachIndexed { index, item ->
                FloatingNavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        else -> MiuixNavigationBar(modifier = modifier) {
            items.forEachIndexed { index, item ->
                MiuixNavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}

/** Side navigation for wide windows. */
@Composable
fun ZeroNavigationRail(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isMaterial()) {
        MaterialNavigationRail(modifier = modifier) {
            items.forEachIndexed { index, item ->
                MaterialNavigationRailItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = { MaterialIcon(imageVector = item.icon, contentDescription = null) },
                    label = { MaterialText(item.label) },
                )
            }
        }
    } else {
        MiuixNavigationRail(modifier = modifier) {
            items.forEachIndexed { index, item ->
                MiuixNavigationRailItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}
