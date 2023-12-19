/**
 * **************************************************************************
 * PlaylistFragment.kt
 * ****************************************************************************
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
 * ***************************************************************************
 */
package com.kbizsoft.KPlayer.gui

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.medialibrary.interfaces.media.Playlist
import com.kbizsoft.medialibrary.media.MediaLibraryItem
import com.kbizsoft.resources.CTX_PLAY_ALL
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.dp
import com.kbizsoft.tools.putSingle
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.PlaylistsFragmentBinding
import com.kbizsoft.KPlayer.gui.audio.AudioBrowserAdapter
import com.kbizsoft.KPlayer.gui.audio.AudioBrowserFragment
import com.kbizsoft.KPlayer.gui.audio.BaseAudioBrowser
import com.kbizsoft.KPlayer.gui.dialogs.*
import com.kbizsoft.KPlayer.gui.helpers.INavigator
import com.kbizsoft.KPlayer.gui.video.VideoBrowserFragment
import com.kbizsoft.KPlayer.gui.view.EmptyLoadingState
import com.kbizsoft.KPlayer.gui.view.FastScroller
import com.kbizsoft.KPlayer.gui.view.RecyclerSectionItemDecoration
import com.kbizsoft.KPlayer.gui.view.RecyclerSectionItemGridDecoration
import com.kbizsoft.KPlayer.media.MediaUtils
import com.kbizsoft.KPlayer.providers.medialibrary.MedialibraryProvider
import com.kbizsoft.KPlayer.reloadLibrary
import com.kbizsoft.KPlayer.util.getScreenWidth
import com.kbizsoft.KPlayer.util.onAnyChange
import com.kbizsoft.KPlayer.viewmodels.mobile.PlaylistsViewModel
import com.kbizsoft.KPlayer.viewmodels.mobile.getViewModel
import kotlin.math.min

class PlaylistFragment : BaseAudioBrowser<PlaylistsViewModel>(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: PlaylistsFragmentBinding
    private lateinit var playlists: RecyclerView
    private lateinit var playlistAdapter: AudioBrowserAdapter
    private lateinit var fastScroller: FastScroller
    override val isMainNavigationPoint = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = arguments?.getInt(PLAYLIST_TYPE, 0) ?: 0
        viewModel = getViewModel(Playlist.Type.values()[type])
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = PlaylistsFragmentBinding.inflate(inflater, container, false)
        playlists = binding.swipeLayout.findViewById(R.id.audio_list)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeLayout.setOnRefreshListener(this)


        //size of an item
        val spacing = resources.getDimension(R.dimen.kl_half).toInt()

        val dimension = resources.getDimension(R.dimen.default_content_width)
        val totalWidth = if (dimension > 0) min(requireActivity().getScreenWidth(), dimension.toInt()) else requireActivity().getScreenWidth()

        val itemSize = RecyclerSectionItemGridDecoration.getItemSize(totalWidth - spacing * 2, nbColumns, spacing, 16.dp)

        playlistAdapter = AudioBrowserAdapter(MediaLibraryItem.TYPE_PLAYLIST, this, cardSize = itemSize)
        playlistAdapter.onAnyChange { updateEmptyView() }
        adapter = playlistAdapter

        setupLayoutManager()

        playlists.adapter = playlistAdapter
        fastScroller = view.rootView.findViewById(R.id.songs_fast_scroller_playlist) as FastScroller
        fastScroller.attachToCoordinator(requireActivity().findViewById(R.id.appbar) as AppBarLayout, requireActivity().findViewById(R.id.coordinator) as CoordinatorLayout, requireActivity().findViewById(R.id.fab) as FloatingActionButton)
        viewModel.provider.pagedList.observe(viewLifecycleOwner) {
            @Suppress("UNCHECKED_CAST")
            playlistAdapter.submitList(it as PagedList<MediaLibraryItem>)
            updateEmptyView()
        }
        viewModel.provider.loading.observe(viewLifecycleOwner) { loading ->
            if (isResumed) setRefreshing(loading) { }
        }

        viewModel.provider.liveHeaders.observe(viewLifecycleOwner) {
            playlists.invalidateItemDecorations()
        }

        fastScroller.setRecyclerView(getCurrentRV(), viewModel.provider)
        (parentFragment as? VideoBrowserFragment)?.playlistOnlyFavorites = viewModel.provider.onlyFavorites
    }

    override fun onDisplaySettingChanged(key: String, value: Any) {
        when (key) {
            DISPLAY_IN_CARDS -> {
                viewModel.providerInCard = value as Boolean
                setupLayoutManager()
                playlists.adapter = adapter
                activity?.invalidateOptionsMenu()
                Settings.getInstance(requireActivity()).putSingle(viewModel.displayModeKey, value)
            }
            ONLY_FAVS -> {
                viewModel.providers[currentTab].showOnlyFavs(value as Boolean)
                viewModel.refresh()
                (parentFragment as? VideoBrowserFragment)?.playlistOnlyFavorites = value
            }
            CURRENT_SORT -> {
                @Suppress("UNCHECKED_CAST") val sort = value as Pair<Int, Boolean>
                viewModel.providers[currentTab].sort = sort.first
                viewModel.providers[currentTab].desc = sort.second
                viewModel.providers[currentTab].saveSort()
                viewModel.refresh()
            }
        }
    }

    private fun updateEmptyView() {
        if (!isAdded) return
        swipeRefreshLayout.visibility = if (Medialibrary.getInstance().isInitiated) View.VISIBLE else View.GONE
        binding.emptyLoading.emptyText = viewModel.filterQuery?.let {  getString(R.string.empty_search, it) } ?: if (viewModel.provider.onlyFavorites) getString(R.string.nofav) else getString(R.string.nomedia)
        binding.emptyLoading.state =
                when {
                    viewModel.provider.loading.value == true && empty -> EmptyLoadingState.LOADING
                    empty && viewModel.provider.onlyFavorites -> EmptyLoadingState.EMPTY_FAVORITES
                    empty && viewModel.filterQuery != null -> EmptyLoadingState.EMPTY_SEARCH
                    empty -> EmptyLoadingState.EMPTY
                    else -> EmptyLoadingState.NONE
                }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        adapter?.itemCount?.let { getMultiHelper()?.toggleActionMode(true, it)}
        mode.menuInflater.inflate(R.menu.action_mode_audio_browser, menu)
        menu.findItem(R.id.action_mode_audio_add_playlist).isVisible = false
        return true
    }

//    override fun onPrepareOptionsMenu(menu: Menu) {
//        super.onPrepareOptionsMenu(menu)
//        menu.findItem(R.id.ml_menu_sortby).isVisible = false
//        menu.findItem(R.id.ml_menu_display_options).isVisible = true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.ml_menu_display_options -> {
//                //filter all sorts and keep only applicable ones
//                val sorts = arrayListOf(Medialibrary.SORT_ALPHA, Medialibrary.SORT_FILENAME, Medialibrary.SORT_ARTIST, Medialibrary.SORT_ALBUM, Medialibrary.SORT_DURATION, Medialibrary.SORT_RELEASEDATE, Medialibrary.SORT_LASTMODIFICATIONDATE, Medialibrary.SORT_FILESIZE, Medialibrary.NbMedia).filter {
//                    viewModel.provider.canSortBy(it)
//                }
//                //Open the display settings Bottom sheet
//                DisplaySettingsDialog.newInstance(
//                        displayInCards = viewModel.providerInCard,
//                        onlyFavs = viewModel.provider.onlyFavorites,
//                        sorts = sorts,
//                        currentSort = viewModel.provider.sort,
//                        currentSortDesc = viewModel.provider.desc
//                )
//                        .show(requireActivity().supportFragmentManager, "DisplaySettingsDialog")
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    private fun setupLayoutManager() {
        val spacing = resources.getDimension(R.dimen.kl_half).toInt()

        if (playlists.itemDecorationCount > 0) {
            playlists.removeItemDecorationAt(0)
        }
        when (viewModel.providerInCard) {
            true -> {
                val screenWidth = (requireActivity() as? INavigator)?.getFragmentWidth(requireActivity()) ?: requireActivity().getScreenWidth()
                adapter?.cardSize = RecyclerSectionItemGridDecoration.getItemSize(screenWidth, nbColumns, spacing, 16.dp)
                adapter?.let { adapter ->
                    @Suppress("UNCHECKED_CAST")
                    displayListInGrid(playlists, adapter, viewModel.provider as MedialibraryProvider<MediaLibraryItem>, spacing)
                }
            }
            else -> {
                adapter?.cardSize = -1
                playlists.addItemDecoration(
                    RecyclerSectionItemDecoration(
                        resources.getDimensionPixelSize(R.dimen.recycler_section_header_height),
                        true,
                        viewModel.provider
                    )
                )
                playlists.layoutManager = LinearLayoutManager(activity)
            }
        }

        val lp = playlists.layoutParams
        val dimension = requireActivity().resources.getDimension(R.dimen.default_content_width)
        lp.width = if (viewModel.providerInCard) ViewGroup.LayoutParams.MATCH_PARENT else {
            dimension.toInt()
        }
        (playlists.parent as View).setBackgroundColor(if (!viewModel.providerInCard && dimension != -1F) backgroundColor else ContextCompat.getColor(requireContext(), R.color.transparent))
        playlists.setBackgroundColor(if (!viewModel.providerInCard && dimension != -1F) listColor else ContextCompat.getColor(requireContext(), R.color.transparent))
    }

    override fun onClick(v: View, position: Int, item: MediaLibraryItem) {
        if (actionMode == null) {
            val i = Intent(activity, HeaderMediaListActivity::class.java)
            i.putExtra(AudioBrowserFragment.TAG_ITEM, item)
            startActivity(i)
        } else super.onClick(v, position, item)
    }

    override fun onCtxAction(position: Int, option: Long) {
        @Suppress("UNCHECKED_CAST")
        if (option == CTX_PLAY_ALL) MediaUtils.playAll(activity, viewModel.provider as MedialibraryProvider<MediaWrapper>, position, false)
        else super.onCtxAction(position, option)
    }

    override fun onRefresh() {
        activity?.reloadLibrary()
    }

    override fun getTitle(): String = getString(R.string.playlists)

    override fun getCurrentRV(): RecyclerView = playlists

    override fun hasFAB() = false

    companion object {
        private const val PLAYLIST_TYPE = "PLAYLIST_TYPE"
        fun newInstance(type: Playlist.Type) = PlaylistFragment().apply {
            arguments = Bundle().apply {
                putInt(PLAYLIST_TYPE, type.ordinal)
            }
        }
    }
}
