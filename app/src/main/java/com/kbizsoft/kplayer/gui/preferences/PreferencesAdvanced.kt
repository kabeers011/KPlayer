/*
 * *************************************************************************
 *  PreferencesAdvanced.java
 * **************************************************************************
 *  Copyright © 2015 KPlayer authors and VideoLAN
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
 *  ***************************************************************************
 */

package com.kbizsoft.KPlayer.gui.preferences

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.resources.AndroidDevices
import com.kbizsoft.resources.KEY_AUDIO_LAST_PLAYLIST
import com.kbizsoft.resources.KEY_CURRENT_AUDIO
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_ARTIST
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_THUMB
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_TITLE
import com.kbizsoft.resources.KEY_CURRENT_MEDIA
import com.kbizsoft.resources.KEY_CURRENT_MEDIA_RESUME
import com.kbizsoft.resources.KEY_MEDIA_LAST_PLAYLIST
import com.kbizsoft.resources.KEY_MEDIA_LAST_PLAYLIST_RESUME
import com.kbizsoft.resources.ROOM_DATABASE
import com.kbizsoft.resources.SCHEME_PACKAGE
import com.kbizsoft.resources.KPlayerInstance
import com.kbizsoft.tools.BitmapCache
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.putSingle
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.gui.DebugLogActivity
import com.kbizsoft.KPlayer.gui.dialogs.ConfirmDeleteDialog
import com.kbizsoft.KPlayer.gui.dialogs.RenameDialog
import com.kbizsoft.KPlayer.gui.helpers.MedialibraryUtils
import com.kbizsoft.KPlayer.gui.helpers.UiTools
import com.kbizsoft.KPlayer.gui.helpers.hf.StoragePermissionsDelegate.Companion.getWritePermission
import com.kbizsoft.KPlayer.gui.helpers.restartMediaPlayer
import com.kbizsoft.KPlayer.util.FeatureFlag
import com.kbizsoft.KPlayer.util.FileUtils
import com.kbizsoft.KPlayer.util.share
import java.io.File
import java.io.IOException

class PreferencesAdvanced : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun getXml() =  R.xml.preferences_adv

    override fun getTitleId(): Int {
        return R.string.advanced_prefs_category
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FeatureFlag.values().isNotEmpty()) findPreference<Preference>("optional_features")?.isVisible = true

        findPreference<EditTextPreference>("network_caching")?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(5))
            it.setSelection(it.editableText.length)
        }
    }

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == null)
            return false
        when (preference.key) {
            "debug_logs" -> {
                val intent = Intent(requireContext(), DebugLogActivity::class.java)
                startActivity(intent)
                return true
            }
            "clear_history" -> {
                val dialog = ConfirmDeleteDialog.newInstance(title = getString(R.string.clear_playback_history), description = getString(R.string.clear_history_message), buttonText = getString(R.string.clear_history))
                dialog.show((activity as FragmentActivity).supportFragmentManager, RenameDialog::class.simpleName)
                dialog.setListener {
                    Medialibrary.getInstance().clearHistory()
                    Settings.getInstance(requireActivity()).edit()
                        .remove(KEY_AUDIO_LAST_PLAYLIST)
                        .remove(KEY_MEDIA_LAST_PLAYLIST)
                        .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                        .remove(KEY_CURRENT_AUDIO)
                        .remove(KEY_CURRENT_MEDIA)
                        .remove(KEY_CURRENT_MEDIA_RESUME)
                        .remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
                        .remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
                        .remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
                        .apply()
                }
                return true
            }
            "clear_media_db" -> {
                val medialibrary = Medialibrary.getInstance()
                if (medialibrary.isWorking) {
                    activity?.let {
                        Toast.makeText(
                            it,
                            R.string.settings_ml_block_scan,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val dialog = ConfirmDeleteDialog.newInstance(
                        title = getString(R.string.clear_media_db),
                        description = getString(R.string.clear_media_db_message),
                        buttonText = getString(R.string.clear)
                    )
                    dialog.show(
                        requireActivity().supportFragmentManager,
                        RenameDialog::class.simpleName
                    )
                    dialog.setListener {
                        lifecycleScope.launch {
                            withContext((Dispatchers.IO)) {
                                medialibrary.clearDatabase(false)
                                //delete thumbnails
                                try {
                                    requireActivity().getExternalFilesDir(null)?.let {
                                        val files =
                                            File(it.absolutePath + Medialibrary.MEDIALIB_FOLDER_NAME).listFiles()
                                        files?.forEach { file ->
                                            if (file.isFile) FileUtils.deleteFile(file)
                                        }
                                    }
                                    BitmapCache.clear()
                                } catch (e: IOException) {
                                    Log.e(this::class.java.simpleName, e.message, e)
                                }
                            }
                            MedialibraryUtils.addDir(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY, requireContext())
                        }
                    }
                    return true
                }
            }
            "clear_app_data" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val dialog = ConfirmDeleteDialog.newInstance(title = getString(R.string.clear_app_data), description = getString(R.string.clear_app_data_message), buttonText = getString(R.string.clear))
                    dialog.show(requireActivity().supportFragmentManager, RenameDialog::class.simpleName)
                    dialog.setListener { (requireActivity().getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData() }
                } else {
                    val i = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.data = Uri.fromParts(SCHEME_PACKAGE, requireActivity().applicationContext.packageName, null)
                    startActivity(i)
                }
                return true
            }
            "quit_app" -> {
                android.os.Process.killProcess(android.os.Process.myPid())
                return true
            }
            "dump_media_db" -> {
                if (Medialibrary.getInstance().isWorking)
                    UiTools.snacker(requireActivity(), getString(R.string.settings_ml_block_scan))
                else {
                    val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + Medialibrary.KPlayer_MEDIA_DB_NAME)
                    lifecycleScope.launch {
                        if (getWritePermission(Uri.fromFile(dst))) {
                            val copied = withContext(Dispatchers.IO) {
                                val db = File(requireContext().getDir("db", Context.MODE_PRIVATE).toString() + Medialibrary.KPlayer_MEDIA_DB_NAME)

                                FileUtils.copyFile(db, dst)
                            }
                            if (copied)
                                UiTools.snackerConfirm(requireActivity(), getString(R.string.dump_db_succes), confirmMessage = R.string.share, overAudioPlayer = false) {
                                    requireActivity().share(dst)
                                } else {
                                Toast.makeText(context, getString(R.string.dump_db_failure), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                return true
            }
            "dump_app_db" -> {
                val dst = File(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + ROOM_DATABASE)
                lifecycleScope.launch {
                    if (getWritePermission(Uri.fromFile(dst))) {
                        val copied = withContext(Dispatchers.IO) {
                            val db = File(requireContext().getDir("db", Context.MODE_PRIVATE).parent!! + "/databases")

                            val files = db.listFiles()?.map { it.path }?.toTypedArray()

                            if (files == null) false else
                                FileUtils.zip(files, dst.path)

                        }
                        if (copied)
                            UiTools.snackerConfirm(requireActivity(), getString(R.string.dump_db_succes), confirmMessage = R.string.share, overAudioPlayer = false) {
                                requireActivity().share(dst)
                            } else {
                            Toast.makeText(context, getString(R.string.dump_db_failure), Toast.LENGTH_LONG).show()
                        }
                    }
                }
                return true
            }
            "optional_features" -> {
                loadFragment(PreferencesOptional())
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null || key == null) return
        when (key) {
            "network_caching" -> {
                sharedPreferences.edit {
                    try {
                        val origValue = Integer.parseInt(sharedPreferences.getString(key, "0") ?: "0")
                        val newValue = origValue.coerceIn(0, 60000)
                        putInt("network_caching_value", newValue)
                        findPreference<EditTextPreference>(key)?.let { it.text = newValue.toString() }
                        if (origValue != newValue) UiTools.snacker(requireActivity(), R.string.network_caching_popup)
                    } catch (e: NumberFormatException) {
                        putInt("network_caching_value", 0)
                        findPreference<EditTextPreference>(key)?.let { it.text = "0" }
                        UiTools.snacker(requireActivity(), R.string.network_caching_popup)
                    }
                }
                lifecycleScope.launch { restartLibKPlayer() }
            }
            // No break because need KPlayerInstance.restart();
            "custom_libkplayer_options" -> {
                lifecycleScope.launch {
                    try {
                        KPlayerInstance.restart()
                    } catch (e: IllegalStateException) {
                        UiTools.snacker(requireActivity(), R.string.custom_libkplayer_options_invalid)
                        sharedPreferences.putSingle("custom_libkplayer_options", "")
                    } finally {
                        restartMediaPlayer()
                    }
                    restartLibKPlayer()
                }
            }
            "opengl", "deblocking", "enable_frame_skip", "enable_time_stretching_audio", "enable_verbose_mode" -> {
                lifecycleScope.launch { restartLibKPlayer() }
            }
            "prefer_smbv1" -> {
                lifecycleScope.launch { KPlayerInstance.restart() }
                UiTools.restartDialog(requireActivity())
            }
        }
    }

    private suspend fun restartLibKPlayer() {
        KPlayerInstance.restart()
        restartMediaPlayer()
    }
}
