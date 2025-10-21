package me.timeto.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.timeto.app.ui.Screen
import me.timeto.app.ui.c
import me.timeto.app.ui.form.FormHeader
import me.timeto.app.ui.form.FormInput
import me.timeto.app.ui.form.FormSwitch
import me.timeto.app.ui.form.button.FormButton
import me.timeto.app.ui.form.padding.FormPaddingHeaderSection
import me.timeto.app.ui.form.padding.FormPaddingSectionHeader
import me.timeto.app.ui.form.padding.FormPaddingTop
import me.timeto.app.ui.header.Header
import me.timeto.app.ui.header.HeaderActionButton
import me.timeto.app.ui.header.HeaderCancelButton
import me.timeto.app.ui.navigation.LocalNavigationFs
import me.timeto.app.ui.navigation.LocalNavigationLayer
import me.timeto.app.ui.navigation.NavigationAlert
import me.timeto.app.ui.rememberVm
import me.timeto.shared.vm.settings.SupabaseSettingsVm

@Composable
fun SupabaseSettingsFs() {

    val navigationFs = LocalNavigationFs.current
    val navigationLayer = LocalNavigationLayer.current

    val (vm, state) = rememberVm {
        SupabaseSettingsVm()
    }

    BackHandler {
        navigationLayer.close()
    }

    // Show test result dialog
    LaunchedEffect(state.testResult) {
        val result = state.testResult
        if (result != null) {
            navigationFs.dialog { dialogLayer ->
                NavigationAlert(
                    message = result,
                    withCancelButton = false,
                    buttonText = "OK",
                    buttonColor = c.blue,
                    onButtonClick = {
                        vm.clearTestResult()
                        dialogLayer.close()
                    }
                )
            }
        }
    }

    Screen {

        val scrollState = rememberLazyListState()

        Header(
            title = "Supabase Sync",
            scrollState = scrollState,
            actionButton = if (state.isSaveEnabled) {
                HeaderActionButton(
                    text = "Save",
                    isEnabled = true,
                    onClick = {
                        vm.save {
                            navigationFs.alert("Configuration saved!")
                        }
                    },
                )
            } else null,
            cancelButton = HeaderCancelButton(
                text = "Cancel",
                onClick = {
                    navigationLayer.close()
                },
            ),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = scrollState,
            contentPadding = PaddingValues(bottom = 25.dp),
        ) {

            item {
                FormPaddingTop()

                // Info text
                Text(
                    text = "Configure Supabase connection for cloud synchronization. Your data will be sent to your personal Supabase database.",
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    color = c.secondaryText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )

                FormPaddingSectionHeader()
                FormHeader("CONFIGURATION")
                FormPaddingHeaderSection()

                FormInput(
                    initText = state.url,
                    placeholder = "https://xxx.supabase.co",
                    onChange = { vm.setUrl(it) },
                    isFirst = true,
                    isLast = false,
                    isAutoFocus = false,
                    imeAction = ImeAction.Next,
                )

                FormInput(
                    initText = state.displayKey,
                    placeholder = "Supabase API Key",
                    onChange = { vm.setKey(it) },
                    isFirst = false,
                    isLast = true,
                    isAutoFocus = false,
                    imeAction = ImeAction.Done,
                )
            }

            // Test Connection
            if (state.isConfigured) {
                item {
                    FormPaddingSectionHeader()

                    FormButton(
                        title = if (state.isTestingConnection) "Testing..." else "Test Connection",
                        isFirst = true,
                        isLast = true,
                        titleColor = c.blue,
                        onClick = {
                            vm.testConnection()
                        },
                    )
                }
            }

            // Enable/Disable
            if (state.isConfigured) {
                item {
                    FormPaddingSectionHeader()
                    FormHeader("SYNC STATUS")
                    FormPaddingHeaderSection()

                    FormSwitch(
                        title = "Enable Sync",
                        isEnabled = state.isEnabled,
                        isFirst = true,
                        isLast = false,
                        onChange = { isEnabled ->
                            if (isEnabled) {
                                vm.enable()
                            } else {
                                vm.disable()
                            }
                        },
                    )

                    FormButton(
                        title = "Last Sync",
                        note = state.lastSyncText,
                        isFirst = false,
                        isLast = true,
                        withArrow = false,
                        onClick = {},
                    )
                }
            }

            // Delete configuration
            if (state.isConfigured) {
                item {
                    FormPaddingSectionHeader()

                    FormButton(
                        title = "Clear Configuration",
                        titleColor = c.red,
                        isFirst = true,
                        isLast = true,
                        onClick = {
                            navigationFs.dialog { dialogLayer ->
                                NavigationAlert(
                                    message = "Are you sure you want to delete Supabase configuration?",
                                    withCancelButton = true,
                                    buttonText = "Delete",
                                    buttonColor = c.red,
                                    onButtonClick = {
                                        vm.clearConfig()
                                        dialogLayer.close()
                                    }
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
