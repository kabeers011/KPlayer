/*****************************************************************************
 * HistoryAdapter.java
 *
 * Copyright © 2012-2015 KPlayer authors and VideoLAN
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
package com.kbizsoft.KPlayer.gui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.resources.UPDATE_SELECTION
import com.kbizsoft.tools.MultiSelectAdapter
import com.kbizsoft.tools.MultiSelectHelper
import com.kbizsoft.tools.Settings
import com.kbizsoft.KPlayer.BR
import com.kbizsoft.KPlayer.databinding.HistoryItemBinding
import com.kbizsoft.KPlayer.databinding.HistoryItemCardBinding
import com.kbizsoft.KPlayer.gui.helpers.*
import com.kbizsoft.KPlayer.interfaces.IListEventsHandler
import com.kbizsoft.KPlayer.interfaces.SwipeDragHelperAdapter
import com.kbizsoft.KPlayer.util.LifecycleAwareScheduler
import com.kbizsoft.KPlayer.util.isOTG
import com.kbizsoft.KPlayer.util.isSD
import com.kbizsoft.KPlayer.util.isSchemeNetwork


class HistoryAdapter(private val inCards: Boolean = false, private val listEventsHandler: IListEventsHandler? = null) : DiffUtilAdapter<MediaWrapper, HistoryAdapter.ViewHolder>(),
        MultiSelectAdapter<MediaWrapper>, IEventsSource<Click> by EventsSource(), SwipeDragHelperAdapter {

    val updateEvt : LiveData<Unit> = MutableLiveData()
    private lateinit var layoutInflater: LayoutInflater
    var multiSelectHelper: MultiSelectHelper<MediaWrapper> = MultiSelectHelper(this, UPDATE_SELECTION)
    var scheduler: LifecycleAwareScheduler? = null

    inner class ViewHolder(binding: ViewDataBinding) : SelectorViewHolder<ViewDataBinding>(binding), MarqueeViewHolder {

        override val titleView = when (binding) {
            is HistoryItemBinding -> binding.title
            is HistoryItemCardBinding -> binding.title
            else -> null
        }

        init {
            this.binding = binding
            when (binding) {
                is HistoryItemBinding -> binding.holder = this
                is HistoryItemCardBinding -> binding.holder = this
            }

        }

        fun onClick(@Suppress("UNUSED_PARAMETER") v: View) {
            eventsChannel.trySend(SimpleClick(layoutPosition))
        }

        fun onLongClick(@Suppress("UNUSED_PARAMETER") v: View) = eventsChannel.trySend(LongClick(layoutPosition)).isSuccess

        fun onImageClick(@Suppress("UNUSED_PARAMETER") v: View) {
            if (inCards)
                eventsChannel.trySend(SimpleClick(layoutPosition))
            else
                eventsChannel.trySend(ImageClick(layoutPosition))
        }

        override fun isSelected() = getItem(layoutPosition).hasStateFlags(MediaLibraryItem.FLAG_SELECTED)
        fun recycle() {
            when (binding) {
                is HistoryItemBinding -> (binding as HistoryItemBinding).title.isSelected = false
                is HistoryItemCardBinding -> (binding as HistoryItemCardBinding).title.isSelected = false
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (inCards && Settings.listTitleEllipsize == 4) scheduler = enableMarqueeEffect(recyclerView)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        scheduler?.cancelAction(MARQUEE_ACTION)
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!::layoutInflater.isInitialized) layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(if (inCards) HistoryItemCardBinding.inflate(layoutInflater, parent, false) else HistoryItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val media = getItem(position)
        when (holder.binding) {
            is HistoryItemBinding -> {
                (holder.binding as HistoryItemBinding).media = media
                holder.binding.setVariable(BR.isNetwork, media.uri.scheme.isSchemeNetwork())
                holder.binding.setVariable(BR.isSD, media.uri.isSD())
                holder.binding.setVariable(BR.isOTG, media.uri.isOTG())
                (holder.binding as HistoryItemBinding).cover = getMediaIconDrawable(holder.itemView.context, media.type)
                ((holder.binding as HistoryItemBinding).icon.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = if (media.type == MediaWrapper.TYPE_VIDEO) "16:10" else "1"
            }
            is HistoryItemCardBinding -> {
                (holder.binding as HistoryItemCardBinding).media = media
                holder.binding.setVariable(BR.isNetwork, media.uri.scheme.isSchemeNetwork())
                holder.binding.setVariable(BR.isSD, media.uri.isSD())
                holder.binding.setVariable(BR.isOTG, media.uri.isOTG())
                (holder.binding as HistoryItemCardBinding).cover = getMediaIconDrawable(holder.itemView.context, media.type)
                ((holder.binding as HistoryItemCardBinding).icon.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = if (media.type == MediaWrapper.TYPE_VIDEO) "16:10" else "1"
            }
        }


        holder.selectView(multiSelectHelper.isSelected(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNullOrEmpty())
            super.onBindViewHolder(holder, position, payloads)
        else
            holder.selectView(multiSelectHelper.isSelected(position))
    }

    override fun getItemId(arg0: Int): Long {
        return 0
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onUpdateFinished() {
        (updateEvt as MutableLiveData).value = Unit
    }

    companion object {

        const val TAG = "KPlayer/HistoryAdapter"
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {    }

    override fun onItemDismiss(position: Int) {
        val item = getItem(position)
        listEventsHandler?.onRemove(position, item)
    }

    override fun onItemMoved(dragFrom: Int, dragTo: Int) {    }

    override fun createCB(): DiffCallback<MediaWrapper> = object : DiffCallback<MediaWrapper>() {
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].title == newList[newItemPosition].title &&
                        oldList[oldItemPosition].description == newList[newItemPosition].description
    }
}