package com.kbizsoft.KPlayer.interfaces

import android.view.View
import com.kbizsoft.medialibrary.media.MediaLibraryItem

interface ITVEventsHandler {
    fun onClickPlay(v: View, position: Int)
    fun onClickPlayNext(v: View, position: Int)
    fun onClickAppend(v: View, position: Int)
    fun onClickAddToPlaylist(v: View, position: Int)
    fun onClickMoveUp(v: View, position: Int)
    fun onClickMoveDown(v: View, position: Int)
    fun onClickRemove(v: View, position: Int)
    fun onFocusChanged(item: MediaLibraryItem)

}
