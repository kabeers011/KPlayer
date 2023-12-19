package com.kbizsoft.KPlayer

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.resources.util.getFromMl
import com.kbizsoft.tools.AppScope
import com.kbizsoft.tools.isAppStarted
import com.kbizsoft.KPlayer.gui.DialogActivity
import com.kbizsoft.KPlayer.util.scanAllowed


private const val TAG = "KPlayer/StoragesMonitor"
class StoragesMonitor : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (!isAppStarted()) when (action) {
            Intent.ACTION_MEDIA_MOUNTED -> intent.data?.let { actor.trySend(Mount(context, it)) }
            Intent.ACTION_MEDIA_UNMOUNTED -> intent.data?.let { actor.trySend(Unmount(context, it)) }
            else -> return
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val actor = AppScope.actor<MediaEvent>(capacity = Channel.UNLIMITED) {
        for (action in channel) when (action){
            is Mount -> {
                if (action.uuid.isEmpty()) return@actor
                Log.i("StoragesMonitor", "Storage management: mount: ${action.uuid} - ${action.path}")
                if (action.path.scanAllowed()) {
                    val isNew = action.ctx.getFromMl {
                        val isNewForML = !isDeviceKnown(action.uuid, action.path, true)
                        addDevice(action.uuid, action.path, true)
                        isNewForML
                    }
                    if (isNew) {
                        val intent = Intent(action.ctx, DialogActivity::class.java).apply {
                            setAction(DialogActivity.KEY_DEVICE)
                            putExtra(DialogActivity.EXTRA_PATH, action.path)
                            putExtra(DialogActivity.EXTRA_UUID, action.uuid)
                            putExtra(DialogActivity.EXTRA_SCAN, isNew)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        action.ctx.startActivity(intent)
                    }
                }
            }
            is Unmount -> {
                Log.i("StoragesMonitor", "Storage management: unmount: ${action.uuid} - ${action.path}")
                delay(100L)
                Medialibrary.getInstance().removeDevice(action.uuid, action.path)
            }
        }
    }
}

private sealed class MediaEvent(val ctx: Context)
private class Mount(ctx: Context, val uri : Uri, val path : String = uri.path!!, val uuid : String = uri.lastPathSegment!!) : MediaEvent(ctx)
private class Unmount(ctx: Context, val uri : Uri, val path : String = uri.path!!, val uuid : String = uri.lastPathSegment!!) : MediaEvent(ctx)

fun Context.enableStorageMonitoring() {
    val componentName = ComponentName(applicationContext, StoragesMonitor::class.java)
    applicationContext.packageManager.setComponentEnabledSetting(componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
}