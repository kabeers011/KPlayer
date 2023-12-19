/**
 * **************************************************************************
 * AboutVersionDialog.kt
 * ****************************************************************************
 * Copyright © 2015 KPlayer authors and VideoLAN
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
package com.kbizsoft.KPlayer.gui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.kbizsoft.KPlayer.BuildConfig
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.DialogAboutVersionBinding

/**
 * Dialog showing the info of the current version
 */
class AboutVersionDialog : KPlayerBottomSheetDialogFragment() {

    private lateinit var binding: DialogAboutVersionBinding

    companion object {

        fun newInstance(): AboutVersionDialog {
            return AboutVersionDialog()
        }
    }

    override fun getDefaultState(): Int {
        return STATE_EXPANDED
    }

    override fun needToManageOrientation(): Boolean {
        return false
    }

    override fun initialFocusedView(): View = binding.medias2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DialogAboutVersionBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.version.text = BuildConfig.KPlayer_VERSION_NAME
        binding.medias2.text = getString(R.string.build_time)
        binding.changelog.text = getString(R.string.changelog).replace("*", "•")
        binding.revision.text = getString(R.string.build_revision)
        binding.kplayerRevision.text = getString(R.string.build_kplayer_revision)
        binding.libkplayerRevision.text = getString(R.string.build_libkplayer_revision)
        binding.libkplayerVersion.text = BuildConfig.LIBKPlayer_VERSION
        binding.compiledBy.text = getString(R.string.build_host)
    }


}





