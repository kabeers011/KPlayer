/*******************************************************************************
 *  BrowserFavRepository.kt
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
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.resources.AppContextProvider
import com.kbizsoft.resources.TYPE_LOCAL_FAV
import com.kbizsoft.resources.TYPE_NETWORK_FAV
import com.kbizsoft.tools.NetworkMonitor
import com.kbizsoft.tools.SingletonHolder
import com.kbizsoft.KPlayer.database.BrowserFavDao
import com.kbizsoft.KPlayer.database.MediaDatabase
import com.kbizsoft.KPlayer.mediadb.models.BrowserFav
import com.kbizsoft.KPlayer.util.Permissions
import com.kbizsoft.KPlayer.util.convertFavorites
import java.util.*


class BrowserFavRepository(private val browserFavDao: BrowserFavDao) {

    private val networkMonitor = NetworkMonitor.getInstance(AppContextProvider.appContext)

    val networkFavs by lazy { browserFavDao.getAllNetworkFavs() }

    val browserFavorites by lazy {
        if (Permissions.canReadStorage(AppContextProvider.appContext))
            browserFavDao.getAll()
        else
            browserFavDao.getAllNetworkFavs()
    }

    val localFavorites by lazy { browserFavDao.getAllLocalFavs() }

    suspend fun addNetworkFavItem(uri: Uri, title: String, iconUrl: String?) = withContext(Dispatchers.IO) {
        browserFavDao.insert(BrowserFav(uri, TYPE_NETWORK_FAV, title, iconUrl))
    }

    suspend fun addLocalFavItem(uri: Uri, title: String, iconUrl: String? = null) = withContext(Dispatchers.IO) {
        browserFavDao.insert(BrowserFav(uri, TYPE_LOCAL_FAV, title, iconUrl))
    }

    val networkFavorites by lazy {
        MediatorLiveData<List<MediaWrapper>>().apply {
            addSource(networkFavs.asLiveData()) { value = convertFavorites(it).filterNetworkFavs() }
            addSource(networkMonitor.connectionFlow.asLiveData()) {
                val favList = convertFavorites(networkFavs.asLiveData().value)
                if (favList.isNotEmpty()) value = if (it.connected) favList.filterNetworkFavs() else emptyList()
            }
        }
    }

    @WorkerThread
    suspend fun browserFavExists(uri: Uri): Boolean = withContext(Dispatchers.IO) { browserFavDao.get(uri).isNotEmpty() }

    suspend fun isFavNetwork(searchUri: Uri): Boolean = withContext(Dispatchers.IO) { browserFavDao.get(searchUri).any { it.type == TYPE_NETWORK_FAV && it.uri == searchUri } }

    suspend fun deleteBrowserFav(uri: Uri) = withContext(Dispatchers.IO) { browserFavDao.delete(uri) }

    private fun List<MediaWrapper>.filterNetworkFavs() : List<MediaWrapper> {
        return when {
            isEmpty() -> this
            !networkMonitor.connected -> emptyList()
            !NetworkMonitor.getInstance(AppContextProvider.appContext).lanAllowed -> {
                val schemes = Arrays.asList("ftp", "sftp", "ftps", "http", "https")
                mutableListOf<MediaWrapper>().apply { this@filterNetworkFavs.filterTo(this) { schemes.contains(it.uri.scheme) } }
            }
            else -> this
        }
    }

    companion object : SingletonHolder<BrowserFavRepository, Context>({ BrowserFavRepository(MediaDatabase.getInstance(it).browserFavDao()) })
}
