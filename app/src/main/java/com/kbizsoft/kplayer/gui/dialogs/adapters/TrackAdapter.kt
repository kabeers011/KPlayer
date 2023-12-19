/*
 * ************************************************************************
 *  TrackAdapter.kt
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

package com.kbizsoft.KPlayer.gui.dialogs.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kbizsoft.tools.Settings
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.VideoTrackItemBinding
import com.kbizsoft.KPlayer.gui.helpers.MARQUEE_ACTION
import com.kbizsoft.KPlayer.gui.helpers.enableMarqueeEffect
import com.kbizsoft.KPlayer.util.LifecycleAwareScheduler

class TrackAdapter(private val tracks: Array<KplayerTrack>, var selectedTrack: KplayerTrack?, val trackTypePrefix:String) : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    lateinit var trackSelectedListener: (KplayerTrack) -> Unit
    private var scheduler: LifecycleAwareScheduler? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = VideoTrackItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    fun setOnTrackSelectedListener(listener: (KplayerTrack) -> Unit) {
        trackSelectedListener = listener
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tracks[position], tracks[position] == selectedTrack)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (Settings.listTitleEllipsize == 4) scheduler = enableMarqueeEffect(recyclerView)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        scheduler?.cancelAction(MARQUEE_ACTION)
        super.onViewRecycled(holder)
    }


    inner class ViewHolder(val binding: VideoTrackItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {

            itemView.setOnClickListener {
                val oldSelectedIndex = tracks.indexOf(selectedTrack)
                selectedTrack = tracks[layoutPosition]
                notifyItemChanged(oldSelectedIndex)
                notifyItemChanged(layoutPosition)
                trackSelectedListener.invoke(tracks[layoutPosition])
            }
        }

        fun bind(trackDescription: KplayerTrack, selected: Boolean) {
            binding.track = trackDescription
            val context = binding.root.context
            binding.contentDescription = context.getString(R.string.talkback_track, trackTypePrefix, if (trackDescription.getId() == "-1") context.getString(R.string.disable_track) else trackDescription.getName(), if (selected) context.getString(R.string.selected) else "")
            binding.selected = selected
            binding.executePendingBindings()
        }
    }
}