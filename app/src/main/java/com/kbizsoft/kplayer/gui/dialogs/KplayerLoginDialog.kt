/*
 * ************************************************************************
 *  NetworkLoginDialog.java
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

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.kbizsoft.libkplayer.Dialog
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.KplayerLoginDialogBinding
import com.kbizsoft.KPlayer.gui.helpers.UiTools
import com.kbizsoft.resources.AndroidDevices
import com.kbizsoft.tools.LOGIN_STORE
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.putSingle

class KplayerLoginDialog : KplayerDialog<Dialog.LoginDialog, KplayerLoginDialogBinding>(), View.OnFocusChangeListener {

    private lateinit var settings: SharedPreferences

    override val layout= R.layout.kplayer_login_dialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Settings.showTvUi && !AndroidDevices.hasPlayServices) {
            binding.login.onFocusChangeListener = this
            binding.password.onFocusChangeListener = this
        }
        binding.store.onFocusChangeListener = this
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settings = Settings.getInstance(requireActivity())
    }

    fun onLogin(@Suppress("UNUSED_PARAMETER") v: View) {
        kplayerDialog.postLogin(binding.login.text.toString().trim(),
                binding.password.text.toString().trim(), binding.store.isChecked)
        settings.putSingle(LOGIN_STORE, binding.store.isChecked)
        dismiss()
    }

    fun store() = settings.getBoolean(LOGIN_STORE, true)

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) UiTools.setKeyboardVisibility(v, v is EditText)
    }
}
