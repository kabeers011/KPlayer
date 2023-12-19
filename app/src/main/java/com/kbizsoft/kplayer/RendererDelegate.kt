/*****************************************************************************
 * RendererDelegate.java
 *
 * Copyright © 2017 KPlayer authors and VideoLAN
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
 */
package com.kbizsoft.KPlayer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.kbizsoft.libkplayer.RendererDiscoverer
import com.kbizsoft.libkplayer.RendererItem
import com.kbizsoft.resources.AppContextProvider
import com.kbizsoft.resources.KPlayerInstance
import com.kbizsoft.tools.AppScope
import com.kbizsoft.tools.NetworkMonitor
import com.kbizsoft.tools.isAppStarted
import com.kbizsoft.tools.livedata.LiveDataset
import com.kbizsoft.tools.retry
import java.util.*

object RendererDelegate : RendererDiscoverer.EventListener {

    private const val TAG = "KPlayer/RendererDelegate"
    private val discoverers = ArrayList<RendererDiscoverer>()
    val renderers : LiveDataset<RendererItem> = LiveDataset()

    @Volatile private var started = false

    init {
        NetworkMonitor.getInstance(AppContextProvider.appContext).connectionFlow.onEach { if (it.connected) start() else stop() }.launchIn(AppScope)
    }

    suspend fun start() {
        if (started) return
        val libKplayer = withContext(Dispatchers.IO) { KPlayerInstance.getInstance(AppContextProvider.appContext) }
        started = true
        for (discoverer in RendererDiscoverer.list(libKplayer)) {
            val rd = RendererDiscoverer(libKplayer, discoverer.name)
            discoverers.add(rd)
            rd.setEventListener(this@RendererDelegate)
            retry(5, 1000L) { if (!rd.isReleased) rd.start() else false }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        for (discoverer in discoverers) discoverer.stop()
        if (isAppStarted() || PlaybackService.instance?.run { !isPlaying } != false) {
            PlaybackService.renderer.value = null
        }
        clear()
    }

    private fun clear() {
        discoverers.clear()
        renderers.clear()
    }

    override fun onEvent(event: RendererDiscoverer.Event?) {
        when (event?.type) {
            RendererDiscoverer.Event.ItemAdded -> renderers.add(event.item)
            RendererDiscoverer.Event.ItemDeleted -> renderers.remove(event.item)
        }
    }
}
