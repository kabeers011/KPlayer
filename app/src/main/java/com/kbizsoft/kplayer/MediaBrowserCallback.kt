/*
 *****************************************************************************
 * MediaBrowserCallback.kt
 *****************************************************************************
 * Copyright © 2021 KPlayer authors and VideoLAN
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

package com.kbizsoft.KPlayer

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.SendChannel
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.tools.conflatedActor

/**
 * @see com.kbizsoft.KPlayer.viewmodels.ICallBackHandler
 */
interface IMediaBrowserCallback {
    fun registerHistoryCallback(callback: () -> Unit)
    fun registerMediaCallback(callback: () -> Unit)
    fun removeCallbacks()
}

/**
 *@see com.kbizsoft.KPlayer.viewmodels.CallBackDelegate
 */
class MediaBrowserCallback(private val playbackService: PlaybackService) : IMediaBrowserCallback,
        Medialibrary.MediaCb,
        Medialibrary.ArtistsCb,
        Medialibrary.AlbumsCb,
        Medialibrary.GenresCb,
        Medialibrary.PlaylistsCb,
        Medialibrary.HistoryCb {

    private val medialibrary = Medialibrary.getInstance()
    private lateinit var historyActor: SendChannel<Unit>
    private lateinit var refreshActor: SendChannel<Unit>

    override fun registerHistoryCallback(callback: () -> Unit) {
        historyActor = playbackService.lifecycleScope.conflatedActor { callback() }
        medialibrary.addHistoryCb(this)
    }

    override fun onHistoryModified() {
        historyActor.trySend(Unit)
    }

    override fun registerMediaCallback(callback: () -> Unit) {
        refreshActor = playbackService.lifecycleScope.conflatedActor { callback() }
        medialibrary.addMediaCb(this)
        medialibrary.addArtistsCb(this)
        medialibrary.addAlbumsCb(this)
        medialibrary.addGenreCb(this)
        medialibrary.addPlaylistCb(this)
    }

    override fun onMediaAdded() {
        refreshActor.trySend(Unit)
    }

    override fun onMediaDeleted(id: LongArray?) {
        refreshActor.trySend(Unit)
    }

    override fun onMediaModified() {
        /* Intentionally Ignored */
    }

    override fun onMediaConvertedToExternal(id: LongArray?) {
        refreshActor.trySend(Unit)
    }

    override fun onArtistsAdded() {
        refreshActor.trySend(Unit)
    }

    override fun onArtistsModified() {
        refreshActor.trySend(Unit)
    }

    override fun onArtistsDeleted() {
        refreshActor.trySend(Unit)
    }

    override fun onAlbumsAdded() {
        refreshActor.trySend(Unit)
    }

    override fun onAlbumsModified() {
        refreshActor.trySend(Unit)
    }

    override fun onAlbumsDeleted() {
        refreshActor.trySend(Unit)
    }

    override fun onGenresAdded() {
        refreshActor.trySend(Unit)
    }

    override fun onGenresModified() {
        refreshActor.trySend(Unit)
    }

    override fun onGenresDeleted() {
        refreshActor.trySend(Unit)
    }

    override fun onPlaylistsAdded() {
        refreshActor.trySend(Unit)
    }

    override fun onPlaylistsModified() {
        refreshActor.trySend(Unit)
    }

    override fun onPlaylistsDeleted() {
        refreshActor.trySend(Unit)
    }

    fun onShuffleChanged() {
        refreshActor.trySend(Unit)
    }

    override fun removeCallbacks() {
        if (::refreshActor.isInitialized) {
            medialibrary.removeMediaCb(this)
            medialibrary.removeArtistsCb(this)
            medialibrary.removeAlbumsCb(this)
            medialibrary.removeGenreCb(this)
            medialibrary.removePlaylistCb(this)
            refreshActor.close()
        }
        if (::historyActor.isInitialized) {
            medialibrary.removeHistoryCb(this)
            historyActor.close()
        }
    }
}