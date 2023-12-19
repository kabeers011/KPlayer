package com.kbizsoft.KPlayer.gui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kbizsoft.medialibrary.media.DummyItem
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.tools.dp
import com.kbizsoft.KPlayer.databinding.SimpleItemBinding
import com.kbizsoft.KPlayer.gui.helpers.getDummyItemIcon

private val cb = object : DiffUtil.ItemCallback<MediaLibraryItem>() {
    override fun areItemsTheSame(oldItem: MediaLibraryItem, newItem: MediaLibraryItem) = oldItem == newItem
    override fun areContentsTheSame(oldItem: MediaLibraryItem, newItem: MediaLibraryItem) = true
}

class SimpleAdapter(val handler: ClickHandler) : ListAdapter<MediaLibraryItem, SimpleAdapter.ViewHolder>(cb) {


    interface ClickHandler {
        fun onClick(item: MediaLibraryItem)
    }

    private lateinit var inflater : LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (!this::inflater.isInitialized) inflater = LayoutInflater.from(parent.context)
        return ViewHolder(handler, SimpleItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.item = getItem(position)
        holder.binding.imageWidth = 48.dp
        (getItem(position) as? DummyItem)?.let {
            holder.binding.cover =  getDummyItemIcon(holder.itemView.context, it)
        }
    }

    fun isEmpty() = itemCount == 0

    class ViewHolder(handler: ClickHandler, val binding: SimpleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.handler = handler
        }
    }

}