/*
 * ************************************************************************
 *  KplayerProgressDialog.java
 * *************************************************************************
 *  Copyright © 2016 KPlayer authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *
 *  *************************************************************************
 */

package com.kbizsoft.KPlayer.gui.dialogs

import android.view.View
import com.kbizsoft.libkplayer.Dialog
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.KplayerProgressDialogBinding

class KplayerProgressDialog : KplayerDialog<Dialog.ProgressDialog, KplayerProgressDialogBinding>() {

    override val layout: Int
        get() = R.layout.kplayer_progress_dialog

    fun updateProgress() {
        binding.progress.progress = (kplayerDialog.position * 100f).toInt()
        binding.cancel.text = kplayerDialog.cancelText
        binding.cancel.visibility = if (kplayerDialog.cancelText.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
}
