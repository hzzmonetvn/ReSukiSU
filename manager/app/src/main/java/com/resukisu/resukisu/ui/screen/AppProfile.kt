package com.resukisu.resukisu.ui.screen

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppProfileTemplateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TemplateEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.resukisu.resukisu.Natives
import com.resukisu.resukisu.R
import com.resukisu.resukisu.ui.component.profile.AppProfileConfig
import com.resukisu.resukisu.ui.component.profile.RootProfileConfig
import com.resukisu.resukisu.ui.component.profile.TemplateConfig
import com.resukisu.resukisu.ui.component.settings.SettingsBaseWidget
import com.resukisu.resukisu.ui.component.settings.SettingsSwitchWidget
import com.resukisu.resukisu.ui.theme.CardConfig
import com.resukisu.resukisu.ui.theme.ThemeConfig
import com.resukisu.resukisu.ui.theme.getCardElevation
import com.resukisu.resukisu.ui.util.LocalSnackbarHost
import com.resukisu.resukisu.ui.util.forceStopApp
import com.resukisu.resukisu.ui.util.getSepolicy
import com.resukisu.resukisu.ui.util.launchApp
import com.resukisu.resukisu.ui.util.restartApp
import com.resukisu.resukisu.ui.util.setSepolicy
import com.resukisu.resukisu.ui.viewmodel.SuperUserViewModel
import com.resukisu.resukisu.ui.viewmodel.getTemplateInfoById
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

/**
 * @author weishu
 * @date 2023/5/16.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppProfileScreen(
    navigator: DestinationsNavigator,
    appInfo: SuperUserViewModel.AppInfo,
) {
    val context = LocalContext.current
    val snackBarHost = LocalSnackbarHost.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val failToUpdateAppProfile = stringResource(R.string.failed_to_update_app_profile).format(appInfo.label)
    val failToUpdateSepolicy = stringResource(R.string.failed_to_update_sepolicy).format(appInfo.label)
    val suNotAllowed = stringResource(R.string.su_not_allowed).format(appInfo.label)

    val packageName = appInfo.packageName
    val initialProfile = Natives.getAppProfile(packageName, appInfo.uid)
    if (initialProfile.allowSu) {
        initialProfile.rules = getSepolicy(packageName)
    }
    var profile by rememberSaveable {
        mutableStateOf(initialProfile)
    }

    val colorScheme = MaterialTheme.colorScheme
    val cardColor = if (CardConfig.isCustomBackgroundEnabled) {
        Color.Transparent
    } else {
        colorScheme.surfaceContainer
    }

    val hazeState = if (CardConfig.isCustomBackgroundEnabled) rememberHazeState() else null

    Scaffold(
        topBar = {
            TopBar(
                title = appInfo.label,
                packageName = packageName,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardColor,
                    scrolledContainerColor = cardColor
                ),
                onBack = dropUnlessResumed { navigator.popBackStack() },
                scrollBehavior = scrollBehavior,
                hazeState = hazeState
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHost) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        AppProfileInner(
            modifier = (if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topPadding = paddingValues.calculateTopPadding(),
            packageName = appInfo.packageName,
            appLabel = appInfo.label,
            appIcon = {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(appInfo.packageInfo).crossfade(true).build(),
                    contentDescription = appInfo.label,
                    modifier = Modifier
                        .padding(4.dp)
                        .width(48.dp)
                        .height(48.dp)
                )
            },
            profile = profile,
            onViewTemplate = {
                getTemplateInfoById(it)?.let { info ->
                    navigator.navigate(TemplateEditorScreenDestination(info))
                }
            },
            onManageTemplate = {
                navigator.navigate(AppProfileTemplateScreenDestination())
            },
            onProfileChange = {
                scope.launch {
                    if (it.allowSu) {
                        // sync with allowlist.c - forbid_system_uid
                        if (appInfo.uid < 2000 && appInfo.uid != 1000) {
                            snackBarHost.showSnackbar(suNotAllowed)
                            return@launch
                        }
                        if (!it.rootUseDefault && it.rules.isNotEmpty() && !setSepolicy(profile.name, it.rules)) {
                            snackBarHost.showSnackbar(failToUpdateSepolicy)
                            return@launch
                        }
                    }
                    if (!Natives.setAppProfile(it)) {
                        snackBarHost.showSnackbar(failToUpdateAppProfile.format(appInfo.uid))
                    } else {
                        profile = it
                    }
                }
            },
        )
    }
}

@Composable
private fun AppProfileInner(
    modifier: Modifier = Modifier,
    topPadding: Dp,
    packageName: String,
    appLabel: String,
    appIcon: @Composable () -> Unit,
    profile: Natives.Profile,
    onViewTemplate: (id: String) -> Unit = {},
    onManageTemplate: () -> Unit = {},
    onProfileChange: (Natives.Profile) -> Unit,
) {
    val isRootGranted = profile.allowSu

    LazyColumn(modifier = modifier) {
        item {
            Spacer(modifier = Modifier.height(topPadding))
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = getCardElevation(),
            ) {
                AppMenuBox(packageName) {
                    SettingsBaseWidget(
                        title = appLabel,
                        description = packageName,
                        iconPlaceholder = false,
                        rowHeader = {
                            appIcon()
                        }
                    ) {}
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = getCardElevation(),
            )
            {
                SettingsSwitchWidget(
                    icon = Icons.Filled.Security,
                    title = stringResource(id = R.string.superuser),
                    checked = isRootGranted,
                    onCheckedChange = { onProfileChange(profile.copy(allowSu = it)) },
                )
            }
        }

        item {
            Crossfade(
                targetState = isRootGranted,
                label = "RootAccess"
            )
            { current ->
                Column(
                    modifier = Modifier.padding(bottom = 6.dp + 48.dp + 6.dp /* SnackBar height */)
                ) {
                    if (current) {
                        val initialMode = if (profile.rootUseDefault) {
                            Mode.Default
                        } else if (profile.rootTemplate != null) {
                            Mode.Template
                        } else {
                            Mode.Custom
                        }
                        var mode by rememberSaveable {
                            mutableStateOf(initialMode)
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = getCardElevation(),
                        ) {
                            ProfileBox(mode, true) {
                                // template mode shouldn't change profile here!
                                if (it == Mode.Default || it == Mode.Custom) {
                                    onProfileChange(
                                        profile.copy(
                                            rootUseDefault = it == Mode.Default,
                                            rootTemplate = null
                                        )
                                    )
                                }
                                mode = it
                            }
                        }

                        AnimatedVisibility(
                            visible = mode != Mode.Default,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors().copy(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = getCardElevation(),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Crossfade(
                                        targetState = mode,
                                        label = "ProfileMode"
                                    ) { currentMode ->
                                        when (currentMode) {
                                            Mode.Template -> {
                                                TemplateConfig(
                                                    profile = profile,
                                                    onViewTemplate = onViewTemplate,
                                                    onManageTemplate = onManageTemplate,
                                                    onProfileChange = onProfileChange
                                                )
                                            }

                                            Mode.Custom -> {
                                                RootProfileConfig(
                                                    fixedName = true,
                                                    profile = profile,
                                                    onProfileChange = onProfileChange
                                                )
                                            }

                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val mode = if (profile.nonRootUseDefault) Mode.Default else Mode.Custom

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors().copy(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            elevation = getCardElevation(),
                        ) {
                            ProfileBox(mode, false) {
                                onProfileChange(profile.copy(nonRootUseDefault = (it == Mode.Default)))
                            }
                        }

                        AnimatedVisibility(
                            visible = mode == Mode.Custom,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors().copy(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = getCardElevation(),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    AppProfileConfig(
                                        fixedName = true,
                                        profile = profile,
                                        enabled = mode == Mode.Custom,
                                        onProfileChange = onProfileChange
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class Mode(@param:StringRes private val res: Int) {
    Default(R.string.profile_default), Template(R.string.profile_template), Custom(R.string.profile_custom);

    val text: String
        @Composable get() = stringResource(res)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBar(
    title: String,
    packageName: String,
    onBack: () -> Unit,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    hazeState: HazeState? = null
) {
    val hazeStyle = if (ThemeConfig.backgroundImageLoaded) HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = 0.8f
        ),
        tint = HazeTint(Color.Transparent)
    ) else null

    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    val modifier = if (ThemeConfig.backgroundImageLoaded && hazeStyle != null && hazeState != null) {
        Modifier.hazeEffect(hazeState) {
            style = hazeStyle
            noiseFactor = 0f
            blurRadius = 30.dp
            alpha = collapsedFraction
        }
    }
    else Modifier

    LargeFlexibleTopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        subtitle = {
            Text(
                text = packageName,
                modifier = Modifier.alpha(0.8f)
            )
        },
        colors = colors,
        navigationIcon = {
            IconButton(
                onClick = onBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier.shadow(
            elevation = if ((scrollBehavior?.state?.overlappedFraction ?: 0f) > 0.01f)
                4.dp else 0.dp,
        )
    )
}

@Composable
private fun ProfileBox(
    mode: Mode,
    hasTemplate: Boolean,
    onModeChange: (Mode) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SettingsBaseWidget(
            icon = Icons.Filled.AccountCircle,
            title = stringResource(R.string.profile),
            description = mode.text,
        ) {}

        HorizontalDivider(
            thickness = Dp.Hairline,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        )
        {
            FilterChip(
                selected = mode == Mode.Default,
                onClick = { onModeChange(Mode.Default) },
                label = {
                    Text(
                        text = stringResource(R.string.profile_default),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = MaterialTheme.shapes.small
            )

            if (hasTemplate) {
                FilterChip(
                    selected = mode == Mode.Template,
                    onClick = { onModeChange(Mode.Template) },
                    label = {
                        Text(
                            text = stringResource(R.string.profile_template),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = MaterialTheme.shapes.small
                )
            }

            FilterChip(
                selected = mode == Mode.Custom,
                onClick = { onModeChange(Mode.Custom) },
                label = {
                    Text(
                        text = stringResource(R.string.profile_custom),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AppMenuBox(
    packageName: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var touchPoint: Offset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        touchPoint = it
                        expanded = true
                    }
                )
            }
    ) {
        content()

        val (offsetX, offsetY) = with(density) {
            (touchPoint.x.toDp()) to (-touchPoint.y.toDp())
        }

        DropdownMenu(
            expanded = expanded,
            offset = DpOffset(offsetX, offsetY),
            onDismissRequest = {
                expanded = false
            }
        ) {
            AppMenuOption(
                text = stringResource(id = R.string.launch_app),
                onClick = {
                    expanded = false
                    launchApp(packageName)
                }
            )

            AppMenuOption(
                text = stringResource(id = R.string.force_stop_app),
                onClick = {
                    expanded = false
                    forceStopApp(packageName)
                }
            )

            AppMenuOption(
                text = stringResource(id = R.string.restart_app),
                onClick = {
                    expanded = false
                    restartApp(packageName)
                }
            )
        }
    }
}

@Composable
private fun AppMenuOption(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        onClick = onClick
    )
}

@Preview
@Composable
private fun AppProfilePreview() {
    var profile by remember { mutableStateOf(Natives.Profile("")) }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = if (CardConfig.isCustomBackgroundEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Surface {
            AppProfileInner(
                packageName = "icu.nullptr.test",
                appLabel = "Test",
                appIcon = {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                    )
                },
                profile = profile,
                topPadding = 0.dp,
                onProfileChange = {
                    profile = it
                },
            )
        }
    }
}