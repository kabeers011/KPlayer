/*******************************************************************************
 *  SlaveRepository.kt
 * ****************************************************************************
 * Copyright © 2018 KPlayer authors and VideoLAN
 *
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
 ******************************************************************************/

package com.kbizsoft.KPlayer.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kbizsoft.libkplayer.interfaces.IMedia
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.tools.IOScopedObject
import com.kbizsoft.tools.SingletonHolder
import com.kbizsoft.KPlayer.database.MediaDatabase
import com.kbizsoft.KPlayer.database.SlaveDao
import com.kbizsoft.KPlayer.mediadb.models.Slave


class SlaveRepository(private val slaveDao:SlaveDao) : IOScopedObject() {

    fun saveSlave(mediaPath: String, type: Int, priority: Int, uriString: String): Job {
        return launch {
            slaveDao.insert(com.kbizsoft.KPlayer.mediadb.models.Slave(mediaPath, type, priority, uriString))
        }
    }

    fun saveSlaves(mw: MediaWrapper): List<Job>? {
        return mw.slaves?.let{
            it.map { saveSlave(mw.location, it.type, it.priority, it.uri) }
        }
    }

    suspend fun getSlaves(mrl: String): List<IMedia.Slave> {
        return withContext(Dispatchers.IO) {
            val slaves = try {
                slaveDao.get(mrl)
            } catch (e: SQLiteException) {
                emptyList<com.kbizsoft.KPlayer.mediadb.models.Slave>()
            }
            val mediaSlaves = slaves.map {
                var uri = it.uri
                if (uri.isNotEmpty())
                    uri = Uri.decode(it.uri)
                IMedia.Slave(it.type, it.priority, uri)
            }
             mediaSlaves
        }
    }

    companion object : SingletonHolder<SlaveRepository, Context>({ SlaveRepository(MediaDatabase.getInstance(it).slaveDao()) })
}
