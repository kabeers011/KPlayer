/*****************************************************************************
 * MedialibraryModel.kt
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

package com.kbizsoft.KPlayer.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.tools.CoroutineContextProvider

abstract class MedialibraryModel<T : MediaLibraryItem>(context: Context, coroutineContextProvider: CoroutineContextProvider) : BaseModel<T>(context, coroutineContextProvider), ICallBackHandler by CallBackDelegate() {

    init {
        @Suppress("LeakingThis")
        viewModelScope.registerCallBacks { refresh() }
    }

    override fun onCleared() {
        releaseCallbacks()
        super.onCleared()
    }
}
