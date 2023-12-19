/*****************************************************************************
 * NetworkProvider.kt
 *****************************************************************************
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
 *****************************************************************************/

package com.kbizsoft.KPlayer.providers

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kbizsoft.libkplayer.util.MediaBrowser
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.medialibrary.media.DummyItem
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.tools.NetworkMonitor
import com.kbizsoft.tools.livedata.LiveDataset
import com.kbizsoft.KPlayer.R

class NetworkProvider(context: Context, dataset: LiveDataset<MediaLibraryItem>, url: String? = null): BrowserProvider(context, dataset, url, Medialibrary.SORT_FILENAME, false), Observer<List<MediaWrapper>> {


    override suspend fun browseRootImpl() {
        dataset.clear()
        dataset.value = mutableListOf()
        if (NetworkMonitor.getInstance(context).lanAllowed) browse()
    }

    override fun fetch() {}

    override suspend fun requestBrowsing(url: String?, eventListener: MediaBrowser.EventListener, interact : Boolean) = withContext(Dispatchers.IO) {
        initBrowser()
        mediabrowser?.let {
            it.changeEventListener(eventListener)
            if (url != null) it.browse(url.toUri(), getFlags(interact))
            else it.discoverNetworkShares()
        }
    }

    override fun refresh() {
        val list by lazy(LazyThreadSafetyMode.NONE) { getList(url!!) }
        when {
            url == null -> {
                browseRoot()
            }
            list !== null -> {
                dataset.value = list as MutableList<MediaLibraryItem>
                removeList(url)
                parseSubDirectories()
                computeHeaders(list as MutableList<MediaLibraryItem>)
            }
            else -> super.refresh()
        }

    }

    override fun parseSubDirectories(list : List<MediaLibraryItem>?) {
        if (url != null) super.parseSubDirectories(list)
    }

    override fun stop() {
        if (url == null) clearListener()
        return super.stop()
    }

    override fun onChanged(favs: List<MediaWrapper>) {
        val data = dataset.value.toMutableList()
        data.listIterator().run {
            while (hasNext()) {
                val item = next()
                if (item.hasStateFlags(MediaLibraryItem.FLAG_FAVORITE) || item is DummyItem) remove()
            }
        }
        dataset.value = data.apply { getFavoritesList(favs)?.let { addAll(0, it) } }
    }

    private fun getFavoritesList(favs: List<MediaWrapper>?): MutableList<MediaLibraryItem>? {
        if (favs?.isNotEmpty() == true) {
            val list = mutableListOf<MediaLibraryItem>()
            list.add(0, DummyItem(context.getString(R.string.network_favorites)))
            for ((index, fav) in favs.withIndex()) list.add(index + 1, fav)
            list.add(DummyItem(context.getString(R.string.network_shared_folders)))
            return list
        }
        return null
    }
}