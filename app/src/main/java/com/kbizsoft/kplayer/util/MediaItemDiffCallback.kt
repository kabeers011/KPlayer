package com.kbizsoft.KPlayer.util

import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.KPlayer.gui.DiffUtilAdapter


class MediaItemDiffCallback<T : MediaLibraryItem> : DiffUtilAdapter.DiffCallback<T>() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem === newItem && oldItem.equals(newItem)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return true
    }

    companion object {
        private const val TAG = "KPlayer/MediaItemDiffCallback"
    }
}
