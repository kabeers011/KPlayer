/*
 * ************************************************************************
 *  BrowserFavoritesModel.kt
 * *************************************************************************
 * Copyright © 2020 KPlayer authors and VideoLAN
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

package com.kbizsoft.KPlayer.viewmodels.browser

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import com.kbizsoft.libkplayer.util.MediaBrowser
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.tools.livedata.LiveDataset
import com.kbizsoft.KPlayer.mediadb.models.BrowserFav
import com.kbizsoft.KPlayer.providers.BrowserProvider
import com.kbizsoft.KPlayer.repository.BrowserFavRepository
import com.kbizsoft.KPlayer.util.convertFavorites

class BrowserFavoritesModel(context: Context) : ViewModel() {
    val favorites = LiveDataset<MediaLibraryItem>()
    val provider = FavoritesProvider(context.applicationContext, favorites, viewModelScope)
}

class FavoritesProvider(
        context: Context,
        dataset: LiveDataset<MediaLibraryItem>,
        scope: CoroutineScope
) : BrowserProvider(context, dataset, null, Medialibrary.SORT_FILENAME, false) {
    private val browserFavRepository = BrowserFavRepository.getInstance(context)

    init {
        browserFavRepository.browserFavorites
                .onEach { list ->
                    convertFavorites(list.sortedWith(compareBy(BrowserFav::title, BrowserFav::type))).let {
                        @Suppress("UNCHECKED_CAST")
                        dataset.postValue(it as MutableList<MediaLibraryItem>)
                        parseSubDirectories()
                    }
                }
                .flowOn(Dispatchers.IO)
                .launchIn(scope)
    }

    override suspend fun requestBrowsing(url: String?, eventListener: MediaBrowser.EventListener, interact : Boolean) = withContext(coroutineContextProvider.IO) {
        initBrowser()
        mediabrowser?.let {
            it.changeEventListener(eventListener)
            if (url != null) it.browse(url.toUri(), getFlags(interact))
        }
    }

    override suspend fun browseRootImpl() {}
}
