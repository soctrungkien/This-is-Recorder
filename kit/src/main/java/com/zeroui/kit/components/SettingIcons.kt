package com.zeroui.kit.components

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Sidebar
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.Trim
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.icon.extended.ZoomOut

object SettingIcons {

    val layers = SettingIcon({ MiuixIcons.Layers }, { RoundedIcons.Layers }, IconHue.Indigo)







    val apps = SettingIcon({ MiuixIcons.GridView }, { RoundedIcons.Apps }, IconHue.Indigo)








    /** QQ */





    val tune = SettingIcon({ MiuixIcons.Tune }, { RoundedIcons.Tune }, IconHue.Blue)

    /** Trim */


    val share = SettingIcon({ MiuixIcons.Share }, { RoundedIcons.Share }, IconHue.Blue)


    val reset = SettingIcon({ MiuixIcons.Reset }, { RoundedIcons.RestartAlt }, IconHue.Orange)


    val importData = SettingIcon({ MiuixIcons.Import }, { RoundedIcons.FileDownload }, IconHue.Teal)








    val translate = SettingIcon({ MiuixIcons.Translate }, { RoundedIcons.Translate }, IconHue.Blue)



    val theme = SettingIcon({ MiuixIcons.Theme }, { RoundedIcons.Palette }, IconHue.Pink)

    val uiStyle = SettingIcon({ MiuixIcons.VerticalSplit }, { RoundedIcons.DisplaySettings }, IconHue.Indigo)



    val monet = SettingIcon({ MiuixIcons.Background }, { RoundedIcons.Wallpaper }, IconHue.Pink)

    val keyColor = SettingIcon({ MiuixIcons.Theme }, { RoundedIcons.Colorize }, IconHue.Pink)

    val paletteStyle = SettingIcon({ MiuixIcons.Album }, { RoundedIcons.Style }, IconHue.Purple)

    val resetAppearance = SettingIcon({ MiuixIcons.Reset }, { RoundedIcons.RestartAlt }, IconHue.Orange)

    val pureBlack = SettingIcon({ MiuixIcons.Show }, { RoundedIcons.DarkMode }, IconHue.Gray)

    val reduceMotion = SettingIcon({ MiuixIcons.Pause }, { RoundedIcons.Animation }, IconHue.Teal)

    val iconStyle = SettingIcon({ MiuixIcons.Image }, { RoundedIcons.Interests }, IconHue.Purple)



    val material = SettingIcon({ MiuixIcons.Layers }, { RoundedIcons.BlurOn }, IconHue.Teal)

    val predictiveBack = SettingIcon({ MiuixIcons.Back }, { RoundedIcons.Swipe }, IconHue.Blue)

    val navTabs = SettingIcon({ MiuixIcons.Sidebar }, { RoundedIcons.Tab }, IconHue.Indigo)

    val homeBackground = SettingIcon({ MiuixIcons.Image }, { RoundedIcons.Image }, IconHue.Pink)


    val navLabels = SettingIcon({ MiuixIcons.Rename }, { RoundedIcons.DriveFileRenameOutline }, IconHue.Blue)

    val uiScale = SettingIcon({ MiuixIcons.ZoomOut }, { RoundedIcons.DisplaySettings }, IconHue.Indigo)

    val listDensity = SettingIcon({ MiuixIcons.ListView }, { RoundedIcons.FormatListBulleted }, IconHue.Gray)

    val smoothCorners = SettingIcon({ MiuixIcons.Album }, { RoundedIcons.Widgets }, IconHue.Purple)

    val pageTransition = SettingIcon({ MiuixIcons.Forward }, { RoundedIcons.Layers }, IconHue.Teal)

    val haptics = SettingIcon({ MiuixIcons.VolumeUp }, { RoundedIcons.NotificationsActive }, IconHue.Orange)

    val topBar = SettingIcon({ MiuixIcons.VerticalSplit }, { RoundedIcons.Layers }, IconHue.Indigo)

    val elasticScroll = SettingIcon({ MiuixIcons.Sort }, { RoundedIcons.Swipe }, IconHue.Teal)

    val homeTitle = SettingIcon({ MiuixIcons.Rename }, { RoundedIcons.DriveFileRenameOutline }, IconHue.Blue)

    val wideNav = SettingIcon({ MiuixIcons.Sidebar }, { RoundedIcons.Tab }, IconHue.Indigo)

    val dialogPosition = SettingIcon({ MiuixIcons.MoreCircle }, { RoundedIcons.Widgets }, IconHue.Purple)

    val refreshRate = SettingIcon({ MiuixIcons.Stopwatch }, { RoundedIcons.Speed }, IconHue.Green)


    val appLog = SettingIcon({ MiuixIcons.Notes }, { RoundedIcons.BugReport }, IconHue.Red)




    val autoUpdate = SettingIcon({ MiuixIcons.Update }, { RoundedIcons.SystemUpdate }, IconHue.Green)

    val hideLauncher = SettingIcon({ MiuixIcons.Hide }, { RoundedIcons.VisibilityOff }, IconHue.Gray)



    val about = SettingIcon({ MiuixIcons.Info }, { RoundedIcons.Info }, IconHue.Blue)

    val checkUpdate = SettingIcon({ MiuixIcons.Refresh }, { RoundedIcons.Update }, IconHue.Green)

    val github = SettingIcon({ MiuixIcons.Link }, { RoundedIcons.Code }, IconHue.Gray)

    val licenses = SettingIcon({ MiuixIcons.File }, { RoundedIcons.Gavel }, IconHue.Gray)

    val contributors = SettingIcon({ MiuixIcons.Contacts }, { RoundedIcons.Group }, IconHue.Indigo)








}
