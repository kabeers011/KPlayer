/*
 * ************************************************************************
 *  WidgetRepository.kt
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

package com.kbizsoft.KPlayer.repository

import android.content.Context
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.kbizsoft.tools.SingletonHolder
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.database.MediaDatabase
import com.kbizsoft.KPlayer.database.WidgetDao
import com.kbizsoft.KPlayer.mediadb.models.Widget
import com.kbizsoft.KPlayer.widget.utils.WidgetCache


class WidgetRepository(private val widgetDao: WidgetDao) {

    suspend fun getAllWidgets() = withContext(Dispatchers.IO) {
        widgetDao.getAll()
    }

    suspend fun getWidget(id: Int) = withContext(Dispatchers.IO) {
        widgetDao.get(id)
    }

    fun getWidgetFlow(id: Int): Flow<Widget> {
        return widgetDao.getFlow(id)
    }

    suspend fun addWidget(widget:Widget) = withContext(Dispatchers.IO) {
        widgetDao.insert(widget)
    }

    suspend fun updateWidget(widget:Widget, preventCacheClear:Boolean = false) {
        if (!preventCacheClear) WidgetCache.clear(widget)
        withContext(Dispatchers.IO) {
            widgetDao.update(widget)
        }
    }

    suspend fun deleteWidget(id: Int) = withContext(Dispatchers.IO) {
        widgetDao.delete(id)
    }

    suspend fun createNew(context: Context, appWidgetId: Int): Widget {
        val widget = Widget(appWidgetId, 0, 0, 0, 0, true, ContextCompat.getColor(context, R.color.black), ContextCompat.getColor(context, R.color.white), 10, 10, 100, showConfigure = true, showSeek = true, showCover = true)
        addWidget(widget)
        return widget
    }


    companion object : SingletonHolder<WidgetRepository, Context>({ WidgetRepository(MediaDatabase.getInstance(it).widgetDao()) })
}
