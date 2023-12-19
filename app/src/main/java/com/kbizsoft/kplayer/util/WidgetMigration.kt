/*
 * ************************************************************************
 *  WidgetMigration.kt
 * *************************************************************************
 * Copyright © 2022 KPlayer authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package com.kbizsoft.KPlayer.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.putSingle
import com.kbizsoft.KPlayer.gui.dialogs.WidgetMigrationDialog
import com.kbizsoft.KPlayer.widget.KPlayerAppWidgetProviderBlack
import com.kbizsoft.KPlayer.widget.KPlayerAppWidgetProviderWhite


private const val WIDGET_MIGRATION_KEY = "widget_migration_key"
object WidgetMigration {
    fun launchIfNeeded(context: AppCompatActivity) {
        val settings = Settings.getInstance(context)
        if (!settings.getBoolean(WIDGET_MIGRATION_KEY, false)) {
            AppWidgetManager.getInstance(context)?.let {manager ->
                if (manager.getAppWidgetIds(ComponentName(context, KPlayerAppWidgetProviderWhite::class.java)).isNotEmpty() || manager.getAppWidgetIds(ComponentName(context, KPlayerAppWidgetProviderBlack::class.java)).isNotEmpty()) {
                    val widgetMigrationDialog = WidgetMigrationDialog()
                    widgetMigrationDialog.show(context.supportFragmentManager, "fragment_widget_migration")
                }
                val pm: PackageManager = context.application.packageManager
                pm.setComponentEnabledSetting(ComponentName(context.application, KPlayerAppWidgetProviderBlack::class.java), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(ComponentName(context.application, KPlayerAppWidgetProviderWhite::class.java), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }


            settings.putSingle(WIDGET_MIGRATION_KEY, true)
        }
    }
}