# Changelog

All notable changes to this project are documented in this file.

## [1.1.0] - 2026-10-25

### Changed

- Replaced the custom floating glass navigation bar with the official Miuix
  `NavigationBar` and `NavigationBarItem` components. The Material UI style now
  uses `androidx.compose.material3.NavigationBar`.
- Tab labels use the library display modes instead of hand-built animations:
  `NavigationBarDisplayMode.IconWithSelectedLabel` when "Selected tab labels" is
  enabled, `NavigationBarDisplayMode.IconAndText` otherwise. Material uses
  `alwaysShowLabel` for the same behaviour.
- Navigation bar container and item colours now come from theme tokens
  (`MiuixTheme.colorScheme` and `MaterialTheme.colorScheme`), with explicit
  high-contrast item colours for dark mode.
- Page transitions still use the official `miuix-nav` `NavDisplay`,
  `NavTransitions.MiuixDefault`, back-stack push/pop, and swipe-to-dismiss.

### Removed

- Removed the custom liquid-glass/blur navigation bar implementation and the
  obsolete "Liquid glass blur" setting.

### Fixed

- Selected and unselected tab icons and labels remain readable in both light and
  dark themes.
