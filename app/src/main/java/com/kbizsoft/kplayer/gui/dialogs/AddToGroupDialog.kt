/*
 * ************************************************************************
 *  AddToGroupDialog.kt
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

package com.kbizsoft.KPlayer.gui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.coroutines.launch
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.medialibrary.interfaces.media.VideoGroup
import com.kbizsoft.medialibrary.media.DummyItem
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.resources.DUMMY_NEW_GROUP
import com.kbizsoft.resources.util.parcelableArray
import com.kbizsoft.tools.AppScope
import com.kbizsoft.tools.CoroutineContextProvider
import com.kbizsoft.tools.DependencyProvider
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.DialogAddToGroupBinding
import com.kbizsoft.KPlayer.gui.SimpleAdapter
import com.kbizsoft.KPlayer.gui.helpers.UiTools.showPinIfNeeded
import com.kbizsoft.KPlayer.viewmodels.mobile.VideoGroupingType
import com.kbizsoft.KPlayer.viewmodels.mobile.VideosViewModel
import java.util.*

class AddToGroupDialog : KPlayerBottomSheetDialogFragment(), SimpleAdapter.ClickHandler {
    override fun getDefaultState(): Int = STATE_EXPANDED

    override fun needToManageOrientation(): Boolean = false

    private lateinit var viewModel: VideosViewModel
    private var forbidNewGroup: Boolean = true
    lateinit var newGroupListener: () -> Unit
    private var isLoading: Boolean = false
        set(value) {
            field = value
            if (::binding.isInitialized) binding.isLoading = value
        }
    private lateinit var binding: DialogAddToGroupBinding
    private lateinit var adapter: SimpleAdapter
    private lateinit var newTrack: Array<MediaWrapper>
    private lateinit var medialibrary: Medialibrary

    private val coroutineContextProvider: CoroutineContextProvider

    override fun initialFocusedView(): View = binding.list

    init {
        AddToGroupDialog.registerCreator { CoroutineContextProvider() }
        coroutineContextProvider = AddToGroupDialog.get(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycleScope.launch { if (requireActivity().showPinIfNeeded()) dismiss() }
        super.onCreate(savedInstanceState)
        medialibrary = Medialibrary.getInstance()
        adapter = SimpleAdapter(this)
        newTrack = try {
            @Suppress("UNCHECKED_CAST")
            val tracks = requireArguments().parcelableArray<MediaWrapper>(KEY_TRACKS) as Array<MediaWrapper>
            tracks
        } catch (e: Exception) {
            emptyArray()
        }

        forbidNewGroup = try {
            requireArguments().getBoolean(FORBID_NEW_GROUP)
        } catch (e: Exception) {
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DialogAddToGroupBinding.inflate(layoutInflater, container, false)
        binding.isLoading = isLoading
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.list.layoutManager = LinearLayoutManager(view.context)
        binding.list.adapter = adapter
        //we have to create the viewmodel that way to avoid the cache from ViewModelProvider which will send the model from the calling activity that may have a different groupingType
        viewModel = VideosViewModel.Factory(requireContext(), VideoGroupingType.NAME, null, null).create(VideosViewModel::class.java)
        updateEmptyView()
        viewModel.provider.pagedList.observe(viewLifecycleOwner) {

            val groupList = it.filter { group -> group is VideoGroup && group.mediaCount() > 1 }.apply {
                forEach { mediaLibraryItem -> mediaLibraryItem.description = resources.getQuantityString(R.plurals.media_quantity, mediaLibraryItem.tracksCount, mediaLibraryItem.tracksCount) }
            }.toMutableList().apply {
                if (newTrack.size > 1 && !forbidNewGroup) {
                    this.add(0, DummyItem(DUMMY_NEW_GROUP, getString(R.string.new_group), getString(R.string.new_group_desc)))
                }
            }
            adapter.submitList(groupList)
            updateEmptyView(groupList.isEmpty())
        }
    }

    private fun updateEmptyView(empty:Boolean = true) {
        binding.empty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun addToGroup(videoGroup: VideoGroup) {
        AppScope.launch(coroutineContextProvider.IO) {
            if (newTrack.isEmpty()) return@launch
            val ids = LinkedList<Long>()
            for (mw in newTrack) {
                val id = mw.id
                if (id == 0L) {
                    var media = medialibrary.getMedia(mw.uri)
                    if (media != null)
                        ids.add(media.id)
                    else {
                        media = medialibrary.addMedia(mw.location, -1L)
                        if (media != null) ids.add(media.id)
                    }
                } else
                    ids.add(id)
            }
            ids.forEach {
                videoGroup.add(it)
            }
        }
        dismiss()
    }

    override fun onClick(item: MediaLibraryItem) {
        when (item) {
            is DummyItem -> {
                newGroupListener.invoke()
                dismiss()
            }
            else -> addToGroup(item as VideoGroup)

        }
    }

    companion object : DependencyProvider<Any>() {

        const val TAG = "KPlayer/SavePlaylistDialog"

        const val KEY_TRACKS = "ADD_TO_GROUP_TRACKS"
        const val FORBID_NEW_GROUP = "FORBID_NEW_GROUP"
    }
}

