/*
 * (C) Copyright 2022 Thanox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package github.tornaco.android.thanos.process.v2

import android.app.ActivityManager
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.enro.core.close
import dev.enro.core.compose.navigationHandle
import github.tornaco.android.thanos.R
import github.tornaco.android.thanos.apps.AppDetailsActivity
import github.tornaco.android.thanos.core.pm.AppInfo
import github.tornaco.android.thanos.module.compose.common.*
import github.tornaco.android.thanos.module.compose.common.theme.ColorDefaults
import github.tornaco.android.thanos.module.compose.common.theme.TypographyDefaults
import github.tornaco.android.thanos.module.compose.common.widget.AppIcon
import github.tornaco.android.thanos.module.compose.common.widget.MD3Badge
import github.tornaco.android.thanos.module.compose.common.widget.ThanoxMediumAppBarScaffold
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningAppStateDetailsPage() {
    val navHandle = navigationHandle<RunningAppStateDetails>()
    val viewModel: RunningAppDetailViewModel = hiltViewModel()
    val state = navHandle.key.state

    RunningAppStateDetailsScreen(state, viewModel) {
        navHandle.close()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RunningAppStateDetailsScreen(
    runningAppState: RunningAppState,
    viewModel: RunningAppDetailViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    ThanoxMediumAppBarScaffold(
        title = {
            Text(
                text = runningAppState.appInfo.appLabel,
                style = TypographyDefaults.appBarTitleTextStyle()
            )
        },
        actions = {
            IconButton(onClick = {
                AppDetailsActivity.start(context, runningAppState.appInfo)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.module_common_ic_md_outline_settings_24),
                    contentDescription = "Settings"
                )
            }
        },
        onBackPressed = onBackPressed
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            runningAppState.processState.forEach { runningProcessState ->
                Card(
                    modifier = Modifier.padding(16.dp),
                    containerColor = ColorDefaults.backgroundSurfaceColor()
                ) {
                    Column {
                        StandardSpacer()
                        ProcessSection(runningAppState.appInfo, runningProcessState, viewModel)
                        StandardSpacer()
                        ServiceSection(runningAppState, runningProcessState, viewModel)
                        StandardSpacer()
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSection(
    runningAppState: RunningAppState,
    runningProcessState: RunningProcessState,
    viewModel: RunningAppDetailViewModel,
) {
    Column(modifier = Modifier) {
        val runningServiceCount = runningProcessState.runningServices.size
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = CenterVertically) {
            Text(
                text = if (runningServiceCount > 0) stringResource(
                    id = R.string.running_processes_item_description_s, runningServiceCount
                ) else stringResource(id = R.string.runningservicedetails_services_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        if (runningServiceCount == 0) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(id = R.string.no_running_services),
                style = MaterialTheme.typography.titleSmall
            )
        } else {
            runningProcessState.runningServices.forEach { service ->
                ServiceTile(runningAppState, service, viewModel)
            }
        }
    }
}

@Composable
private fun ServiceTile(
    runningAppState: RunningAppState,
    service: RunningService,
    viewModel: RunningAppDetailViewModel
) {
    val popMenuExpend = remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickableWithRipple {
                popMenuExpend.value = true
            }
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(12.dp))
        Row(verticalAlignment = CenterVertically) {
            AppIcon(modifier = Modifier.size(42.dp), appInfo = runningAppState.appInfo)
            Spacer(modifier = Modifier.size(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                AppLabelText(appLabel = service.serviceLabel)
                ServiceRunningTime(service)
            }
        }
        TinySpacer()
        Text(
            text = service.running.service.flattenToShortString(),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.size(12.dp))
        ServicePopupMenu(popMenuExpend, service, viewModel)
    }
}

@Composable
private fun ServiceRunningTime(service: RunningService) {
    val activeSince: Long =
        if (service.running.restarting == 0L) service.running.activeSince else -1
    if (activeSince > 0) {
        var runningSeconds by remember {
            mutableStateOf((SystemClock.elapsedRealtime() - activeSince) / 1000L)
        }
        LaunchedEffect(runningSeconds) {
            delay(1000L)
            runningSeconds += 1
        }
        val runningTimeStr = DateUtils.formatElapsedTime(null, runningSeconds)
        Text(
            text = "${service.clientLabel ?: stringResource(id = R.string.service_started_by_app)} • ${
                stringResource(
                    id = R.string.service_running_time
                )
            } $runningTimeStr",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ServicePopupMenu(
    popMenuExpend: MutableState<Boolean>,
    service: RunningService,
    viewModel: RunningAppDetailViewModel
) {
    DropdownPopUpMenu(
        popMenuExpend,
        items = listOf(
            MenuItem(
                "copy",
                stringResource(id = R.string.menu_title_copy),
                R.drawable.ic_file_copy_fill
            ),
            MenuItem(
                "stop",
                stringResource(id = R.string.service_stop),
                R.drawable.ic_close_fill
            )
        )
    ) {
        when (it.id) {
            "copy" -> {
                viewModel.copyServiceName(service)
            }
            "stop" -> {
                viewModel.stopService(service)
            }
            else -> {

            }
        }
    }
}

@Composable
private fun ProcessSection(
    appInfo: AppInfo,
    runningProcessState: RunningProcessState,
    viewModel: RunningAppDetailViewModel
) {
    val popMenuExpend = remember { mutableStateOf(false) }
    val isCached =
        runningProcessState.process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(72.dp)
            .clickableWithRipple {
                popMenuExpend.value = true
            }
            .padding(horizontal = 16.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.size(12.dp))
            Row {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = stringResource(id = R.string.runningservicedetails_processes_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = " (id: ${runningProcessState.process.pid})",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            Row(verticalAlignment = CenterVertically) {
                AppIcon(modifier = Modifier.size(42.dp), appInfo = appInfo)
                Spacer(modifier = Modifier.size(8.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    AppLabelText(appLabel = runningProcessState.process.processName)
                    SmallSpacer()
                    Row(verticalAlignment = CenterVertically) {
                        Text(
                            text = if (isCached) stringResource(id = R.string.cached) else stringResource(
                                id = R.string.running_process_running
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                        SmallSpacer()
                        MD3Badge(runningProcessState.sizeStr)
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            ProcessPopupMenu(popMenuExpend, viewModel, runningProcessState)
        }
    }
}

@Composable
private fun ProcessPopupMenu(
    popMenuExpend: MutableState<Boolean>,
    viewModel: RunningAppDetailViewModel,
    runningProcessState: RunningProcessState
) {
    DropdownPopUpMenu(
        popMenuExpend,
        items = listOf(
            MenuItem(
                "copy",
                stringResource(id = R.string.menu_title_copy),
                R.drawable.ic_file_copy_fill
            ),
            MenuItem(
                "stop",
                stringResource(id = R.string.service_stop),
                R.drawable.ic_close_fill
            )
        )
    ) {
        when (it.id) {
            "copy" -> {
                viewModel.copyProcessName(runningProcessState)
            }
            "stop" -> {
                viewModel.stopProcess(runningProcessState)
            }
            else -> {

            }
        }
    }
}