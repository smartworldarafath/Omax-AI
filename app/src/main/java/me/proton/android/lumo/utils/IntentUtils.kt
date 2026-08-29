package me.proton.android.lumo.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri

import android.content.Context

fun openSettingsIntent(packageName: String): Intent =
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

fun Context.openSettings() {
    startActivity(openSettingsIntent(packageName).apply {
        if (this@openSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    })
}

fun Context.openExternalUrl(url: String) {
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            url.toUri()
        ).apply {
            if (this@openExternalUrl !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    )
}
