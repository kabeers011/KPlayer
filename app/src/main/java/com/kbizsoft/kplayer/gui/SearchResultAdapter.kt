package com.kbizsoft.KPlayer.gui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kbizsoft.medialibrary.Tools
import com.kbizsoft.medialibrary.interfaces.media.Genre
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.medialibrary.interfaces.media.Playlist
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.resources.AppContextProvider
import com.kbizsoft.tools.dp
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.SearchItemBinding
import com.kbizsoft.KPlayer.gui.helpers.SelectorViewHolder
import com.kbizsoft.KPlayer.gui.helpers.UiTools
import com.kbizsoft.KPlayer.util.generateResolutionClass
import java.lang.StringBuilder


class SearchResultAdapter internal constructor(private val mLayoutInflater: LayoutInflater) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private var mDataList: Array<MediaLibraryItem>? = null
    internal lateinit var mClickHandler: SearchActivity.ClickHandler

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SearchItemBinding.inflate(mLayoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mDataList!![position]
        if (item.artworkMrl.isNullOrEmpty())
            holder.binding.cover = UiTools.getDefaultCover(holder.itemView.context, item)
        holder.binding.item = item
        val isNotVideo = item !is MediaWrapper || item.type != MediaWrapper.TYPE_VIDEO
        holder.binding.isSquare = isNotVideo
        holder.binding.coverWidth = if (isNotVideo) 48.dp else 100.dp
        holder.binding.description = when {
            (item as? MediaWrapper)?.type == MediaWrapper.TYPE_VIDEO -> {
                if (item.length > 0) {
                    val resolution = generateResolutionClass(item.width, item.height)
                    if (resolution !== null) {
                        "${Tools.millisToString(item.length)}  •  $resolution"
                    } else Tools.millisToString(item.length)
                } else null
            }
            item is Playlist || item is Genre -> holder.itemView.context.getString(R.string.track_number, item.tracksCount)
            else -> item.description
        }
    }

    fun add(newList: Array<MediaLibraryItem>) {
        mDataList = newList
        notifyDataSetChanged()
    }

    internal fun setClickHandler(clickHandler: SearchActivity.ClickHandler) {
        mClickHandler = clickHandler
    }

    override fun getItemCount(): Int {
        return if (mDataList == null) 0 else mDataList!!.size
    }

    inner class ViewHolder(binding: SearchItemBinding) : SelectorViewHolder<SearchItemBinding>(binding) {

        init {
            binding.holder = this
            binding.handler = mClickHandler
        }
    }
}
