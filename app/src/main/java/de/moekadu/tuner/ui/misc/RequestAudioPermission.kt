package de.moekadu.tuner.ui.misc

import android.Manifest
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import de.moekadu.tuner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
private fun CoroutineScope.launchSnackbar(
    context: Context,
    snackbarHostState: SnackbarHostState,
    permission: PermissionState
) {
    launch {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.audio_record_permission_rationale),
            actionLabel = context.getString(R.string.settings),
            withDismissAction = false
        )
        when (result) {
            SnackbarResult.Dismissed -> {}
            SnackbarResult.ActionPerformed -> {
                permission.launchPermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberTunerAudioPermission(snackbarHostState: SnackbarHostState): Boolean {
    val context = LocalContext.current

    val reopenSnackbarChannel = remember { Channel<Boolean>(Channel.CONFLATED) }
    val permission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO) {
        if (!it)
            reopenSnackbarChannel.trySend(false)
    }
    val permissionGranted by remember { derivedStateOf { permission.status.isGranted }}
//    Log.v("Tuner", "MainGraph: 1: permissions_granted = ${permission.status.isGranted}, rational = ${permission.status.shouldShowRationale}")

    // TODO: relaunching ssems to fail ...
    LaunchedEffect(permission.status, reopenSnackbarChannel) {
        if (!permission.status.isGranted) {
            if (permission.status.shouldShowRationale)
                launchSnackbar(context, snackbarHostState, permission)
            else
                permission.launchPermissionRequest()

            for (reopen in reopenSnackbarChannel)
                launchSnackbar(context, snackbarHostState, permission)
        }
    }
    return permissionGranted
}
