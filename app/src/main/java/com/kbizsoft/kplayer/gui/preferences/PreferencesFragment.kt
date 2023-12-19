/*
 * *************************************************************************
 *  PreferencesFragment.java
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

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.kbizsoft.libkplayer.util.AndroidUtil
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.resources.KEY_AUDIO_LAST_PLAYLIST
import com.kbizsoft.resources.KEY_CURRENT_AUDIO
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_ARTIST
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_THUMB
import com.kbizsoft.resources.KEY_CURRENT_AUDIO_RESUME_TITLE
import com.kbizsoft.resources.KEY_CURRENT_MEDIA
import com.kbizsoft.resources.KEY_CURRENT_MEDIA_RESUME
import com.kbizsoft.resources.KEY_MEDIA_LAST_PLAYLIST
import com.kbizsoft.resources.KEY_MEDIA_LAST_PLAYLIST_RESUME
import com.kbizsoft.resources.util.parcelable
import com.kbizsoft.tools.AUDIO_RESUME_PLAYBACK
import com.kbizsoft.tools.PLAYBACK_HISTORY
import com.kbizsoft.tools.RESULT_RESTART
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.Settings.isPinCodeSet
import com.kbizsoft.tools.VIDEO_RESUME_PLAYBACK
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.gui.PinCodeActivity
import com.kbizsoft.KPlayer.gui.PinCodeReason
import com.kbizsoft.KPlayer.gui.SecondaryActivity
import com.kbizsoft.KPlayer.gui.dialogs.ConfirmAudioPlayQueueDialog
import com.kbizsoft.KPlayer.gui.helpers.UiTools
import com.kbizsoft.KPlayer.gui.preferences.search.PreferenceItem
import com.kbizsoft.KPlayer.util.Permissions

class PreferencesFragment : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    var pinCodeResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadFragment(PreferencesParentalControl())
        }
    }

    override fun getXml() = R.xml.preferences

    override fun getTitleId() = R.string.preferences

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.parcelable<PreferenceItem>(EXTRA_PREF_END_POINT)?.let { endPoint ->
            when (endPoint.parentScreen) {
                R.xml.preferences_ui -> loadFragment(PreferencesUi().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
                R.xml.preferences_video -> loadFragment(PreferencesVideo().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
                R.xml.preferences_subtitles -> loadFragment(PreferencesSubtitles().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
                R.xml.preferences_audio -> loadFragment(PreferencesAudio().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
                R.xml.preferences_adv -> loadFragment(PreferencesAdvanced().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
                R.xml.preferences_casting -> loadFragment(PreferencesCasting().apply {
                    arguments = bundleOf(EXTRA_PREF_END_POINT to endPoint)
                })
            }
            arguments = null
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "directories" -> {
                if (Medialibrary.getInstance().isWorking) {
                    UiTools.snacker(requireActivity(), getString(R.string.settings_ml_block_scan))
                } else {
                    val activity = requireActivity()
                    val intent = Intent(activity.applicationContext, SecondaryActivity::class.java)
                    intent.putExtra("fragment", SecondaryActivity.STORAGE_BROWSER)
                    startActivity(intent)
                    activity.setResult(RESULT_RESTART)
                }
                return true
            }
            "ui_category" -> loadFragment(PreferencesUi())
            "video_category" -> loadFragment(PreferencesVideo())
            "subtitles_category" -> loadFragment(PreferencesSubtitles())
            "audio_category" -> loadFragment(PreferencesAudio())
            "adv_category" -> loadFragment(PreferencesAdvanced())
            "casting_category" -> loadFragment(PreferencesCasting())
            "parental_control" -> {
                if (requireActivity().isPinCodeSet())
                    loadFragment(PreferencesParentalControl())
                else {
                    val intent = PinCodeActivity.getIntent(requireActivity(), PinCodeReason.FIRST_CREATION)
                    pinCodeResult.launch(intent)
                }
            }
            PLAYBACK_HISTORY -> {
                val activity = activity
                activity?.setResult(RESULT_RESTART)
                return true
            }
            AUDIO_RESUME_PLAYBACK -> {

                val audioResumePref = findPreference<CheckBoxPreference>(AUDIO_RESUME_PLAYBACK)
                if (audioResumePref?.isChecked == false) {
                    val dialog = ConfirmAudioPlayQueueDialog()
                    dialog.show((activity as FragmentActivity).supportFragmentManager, ConfirmAudioPlayQueueDialog::class.simpleName)
                    dialog.setListener {
                        Settings.getInstance(requireActivity()).edit()
                                .remove(KEY_AUDIO_LAST_PLAYLIST)
                                .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                                .remove(KEY_CURRENT_AUDIO_RESUME_TITLE)
                                .remove(KEY_CURRENT_AUDIO_RESUME_ARTIST)
                                .remove(KEY_CURRENT_AUDIO_RESUME_THUMB)
                                .remove(KEY_CURRENT_AUDIO)
                                .remove(KEY_CURRENT_MEDIA)
                                .remove(KEY_CURRENT_MEDIA_RESUME)
                                .apply()
                        val activity = activity
                        activity?.setResult(RESULT_RESTART)
                        audioResumePref.isChecked = false
                    }

                    audioResumePref.isChecked = true
                }
                return true
            }
            VIDEO_RESUME_PLAYBACK -> {
                Settings.getInstance(requireActivity()).edit()
                        .remove(KEY_MEDIA_LAST_PLAYLIST)
                        .remove(KEY_MEDIA_LAST_PLAYLIST_RESUME)
                        .remove(KEY_CURRENT_MEDIA_RESUME)
                        .remove(KEY_CURRENT_MEDIA)
                        .apply()
                val activity = activity
                activity?.setResult(RESULT_RESTART)
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val activity = activity ?: return
        if (sharedPreferences == null || key == null) return

        when (key) {
            "video_action_switch" -> if (!AndroidUtil.isOOrLater && findPreference<ListPreference>(key)?.value == "2"
                    && !Permissions.canDrawOverlays(activity))
                Permissions.checkDrawOverlaysPermission(activity)
            PLAYBACK_HISTORY -> {
                if (sharedPreferences.getBoolean(key, true)) {
                    findPreference<CheckBoxPreference>(AUDIO_RESUME_PLAYBACK)?.isChecked = true
                    findPreference<CheckBoxPreference>(VIDEO_RESUME_PLAYBACK)?.isChecked = true
                }
            }
        }
    }
}
