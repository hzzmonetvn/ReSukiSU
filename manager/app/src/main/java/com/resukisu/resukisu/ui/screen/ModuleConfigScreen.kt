package com.resukisu.resukisu.ui.screen

import android.content.Context.MODE_PRIVATE
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.resukisu.resukisu.R
import com.resukisu.resukisu.ksuApp
import com.resukisu.resukisu.ui.component.SearchAppBar
import com.resukisu.resukisu.ui.component.pinnedScrollBehavior
import com.resukisu.resukisu.ui.component.settings.SettingsDropdownWidget
import com.resukisu.resukisu.ui.theme.CardConfig
import com.resukisu.resukisu.ui.theme.ThemeConfig
import com.resukisu.resukisu.ui.util.LocalSnackbarHost
import com.resukisu.resukisu.ui.viewmodel.ModuleViewModel
import dev.chrisbanes.haze.rememberHazeState

/**
 * @author AlexLiuDev233
 * @date 2026/1/26.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>
@Composable
fun ModuleConfigScreen(
    navigator: DestinationsNavigator,
) {
    // module_settings shared perference    <- 模块id_类型命名，为将来模块可能有更多设置做预留
    //    name              value
    // 模块id_webui    <webuix/ksu/default>
    val moduleSettings = LocalContext.current.getSharedPreferences("module_settings", MODE_PRIVATE)
    val hazeState = if (ThemeConfig.backgroundImageLoaded) rememberHazeState() else null
    val viewModel = viewModel<ModuleViewModel>(
        viewModelStoreOwner = ksuApp
    )
    val pullToRefreshState = rememberPullToRefreshState()
    val scrollBehavior = pinnedScrollBehavior()

    Scaffold(
        topBar = {
            SearchAppBar(
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                scrollBehavior = scrollBehavior,
                searchBarPlaceHolderText = stringResource(R.string.search_modules),
                hazeState = hazeState,
                onBackClick = {
                    navigator.popBackStack()
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = LocalSnackbarHost.current
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { innerPadding ->
        val sharedStiffness = Spring.StiffnessMediumLow
        val cornerRadius = 16.dp
        val connectionRadius = 4.dp

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                viewModel.fetchModuleList(true)
            },
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .align(Alignment.TopCenter),
                    state = pullToRefreshState,
                    isRefreshing = viewModel.isRefreshing,
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
                }

                itemsIndexed(viewModel.haveWebuiModuleList) { index, module ->
                    var moduleEngine by remember {
                        mutableStateOf(
                            moduleSettings.getString(module.id + "_webui", "default") ?: "default"
                        )
                    }
                    val isFirst = index == 0
                    val isLast = index == viewModel.haveWebuiModuleList.size - 1

                    val targetTopRadius = if (isFirst) cornerRadius else connectionRadius
                    val targetBottomRadius = if (isLast) cornerRadius else connectionRadius

                    val animatedTopRadius by animateDpAsState(
                        targetValue = targetTopRadius,
                        animationSpec = spring(stiffness = sharedStiffness),
                        label = "TopCornerRadius"
                    )
                    val animatedBottomRadius by animateDpAsState(
                        targetValue = targetBottomRadius,
                        animationSpec = spring(stiffness = sharedStiffness),
                        label = "BottomCornerRadius"
                    )

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 2.dp),
                        shape = RoundedCornerShape(
                            topStart = animatedTopRadius,
                            topEnd = animatedTopRadius,
                            bottomStart = animatedBottomRadius,
                            bottomEnd = animatedBottomRadius
                        ),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                            alpha = CardConfig.cardAlpha
                        ),
                    ) {
                        SettingsDropdownWidget(
                            title = module.name,
                            iconPlaceholder = false,
                            items = listOf(
                                stringResource(R.string.webui_engine_default),
                                stringResource(R.string.webui_engine_webuix),
                                stringResource(R.string.webui_engine_ksu),
                            ),
                            selectedIndex = when (moduleEngine) {
                                "default" -> 0
                                "webuix" -> 1
                                "ksu" -> 2
                                else -> throw UnsupportedOperationException("Unknown engine: $moduleEngine")
                            },
                            onSelectedIndexChange = { index ->
                                val engine = when (index) {
                                    0 -> "default"
                                    1 -> "webuix"
                                    2 -> "ksu"
                                    else -> throw UnsupportedOperationException("Unknown engine index: $index")
                                }

                                moduleEngine = engine

                                moduleSettings.edit(commit = true) {
                                    putString(module.id + "_webui", engine)
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}