package com.just6889.td.tradu_launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UninstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            val data = intent.data
            val packageName = data?.schemeSpecificPart
            Log.d("UninstallReceiver", "PACKAGE_REMOVED: $packageName")
            if (packageName != null) {
                // Buscar si corresponde a algún proyecto y borrar versión local
                val prefs = context.getSharedPreferences("launcher_versions", Context.MODE_PRIVATE)
                val all = prefs.all
                val toRemove = all.keys.filter { key ->
                    key.startsWith("installed_version_") && prefs.getString(key, null) != null &&
                        context.packageManager.getLaunchIntentForPackage(packageName) == null
                }
                for (key in toRemove) {
                    prefs.edit().remove(key).apply()
                }
            }
        }
    }
}
