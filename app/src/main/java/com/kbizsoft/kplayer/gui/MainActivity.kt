/*****************************************************************************
 * MainActivity.java
 *
 * Copyright © 2011-2019 KPlayer authors and VideoLAN
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
 */

package com.kbizsoft.KPlayer.gui

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kbizsoft.libkplayer.util.AndroidUtil
import com.kbizsoft.medialibrary.interfaces.Medialibrary
import com.kbizsoft.resources.ACTIVITY_RESULT_OPEN
import com.kbizsoft.resources.ACTIVITY_RESULT_PREFERENCES
import com.kbizsoft.resources.ACTIVITY_RESULT_SECONDARY
import com.kbizsoft.resources.EXTRA_TARGET
import com.kbizsoft.tools.*
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.StartActivity
import com.kbizsoft.KPlayer.gui.audio.AudioBrowserFragment
import com.kbizsoft.KPlayer.gui.browser.BaseBrowserFragment
import com.kbizsoft.KPlayer.gui.dialogs.AllAccessPermissionDialog
import com.kbizsoft.KPlayer.gui.dialogs.NotificationPermissionManager
import com.kbizsoft.KPlayer.gui.helpers.INavigator
import com.kbizsoft.KPlayer.gui.helpers.Navigator
import com.kbizsoft.KPlayer.gui.helpers.UiTools
import com.kbizsoft.KPlayer.gui.helpers.UiTools.isTablet
import com.kbizsoft.KPlayer.gui.video.VideoGridFragment
import com.kbizsoft.KPlayer.interfaces.Filterable
import com.kbizsoft.KPlayer.interfaces.IRefreshable
import com.kbizsoft.KPlayer.java.VideoManager
import com.kbizsoft.KPlayer.media.MediaUtils
import com.kbizsoft.KPlayer.reloadLibrary
import com.kbizsoft.KPlayer.util.Permissions
import com.kbizsoft.KPlayer.util.Util
import com.kbizsoft.KPlayer.util.WidgetMigration
import com.kbizsoft.KPlayer.util.getScreenWidth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "KPlayer/MainActivity"

class MainActivity : ContentActivity(),
        INavigator by Navigator()
{
    var refreshing: Boolean = false
        set(value) {
            field = value
        }
    private lateinit var mediaLibrary: Medialibrary
    private var scanNeeded = false
    private lateinit var toolbarIcon: ImageView

    private val STORAGE_PERMISSION_CODE = 1001

    override fun getSnackAnchorView(overAudioPlayer:Boolean): View? {
        val view = super.getSnackAnchorView(overAudioPlayer)
        return if (view?.id == android.R.id.content && !isTablet()) {if(overAudioPlayer) findViewById(android.R.id.content) else findViewById(R.id.appbar)} else view
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Util.checkCpuCompatibility(this)
        /*** Start initializing the UI  */
        setContentView(R.layout.main)
        initAudioPlayerContainerActivity()
        setupNavigation(savedInstanceState)

        /* Set up the action bar */
        prepareActionBar()
        /* Reload the latest preferences */
        scanNeeded = savedInstanceState == null && settings.getBoolean(KEY_MEDIALIBRARY_AUTO_RESCAN, true)
        mediaLibrary = Medialibrary.getInstance()

//        KPlayerBilling.getInstance(application).retrieveSkus()
        WidgetMigration.launchIfNeeded(this)
        NotificationPermissionManager.launchIfNeeded(this)


        /* =================================== */
        /* =========== Code By MKN =========== */
        /* === Download videos from server === */
        /* =================================== */


        try {
            // Log.e("VideoManager.is_running", VideoManager.is_running.toString())
            //if(!VideoManager.is_running) {
                // Mark as Script Already Running
                //VideoManager.is_running = true

                // Check if the app has permission
                // Check for runtime permissions on Android 11
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // .Toast.makeText(this@MainActivity, "Checking R", Toast.LENGTH_LONG).show()
                    if (Environment.isExternalStorageManager()) {
                        downloadVideosFromServer()
                    }
                }
                // Check for runtime permissions on Android Marshmallow
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkStoragePermission()) {
                        // Permission already granted
                        downloadVideosFromServer()
                    } else {
                        // Request permission
                        requestStoragePermission()
                    }
                } else {
                    // Permission granted at install time on devices running older Android versions
                    downloadVideosFromServer()
                }
            //}
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error: " + e.message, Toast.LENGTH_LONG).show()
        }

        /* =================================== */
    }


    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // Your code here
                downloadVideosFromServer()
            } else {
                // Permission denied
                // Handle accordingly (show a message, disable features, etc.)
                Toast.makeText(this@MainActivity, "Permission denied by user", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadVideosFromServer(){
         Toast.makeText(this@MainActivity, "Checking for videos on server", Toast.LENGTH_LONG).show()

        // Generate timestamp
        // Get current time in milliseconds
        val currentTimeMillis = System.currentTimeMillis()

        // Create a SimpleDateFormat to format the timestamp
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

        // Create a Date object using the current time
        val currentDate = Date(currentTimeMillis)

        // Generate timestamp
        val timestamp: String = sdf.format(currentDate)

        // Initialize and sync videos on app startup
        val videoManager = VideoManager()


        // Make a Retrofit call to fetch video data from the server with the specified time parameter
        VideoManager.handler = handler
        // Call syncVideos with the generated timestamp
        videoManager.syncVideos(timestamp, this@MainActivity)
    }

    override fun onResume() {
        super.onResume()
        //Only the partial permission is granted for Android 11+
        if (!settings.getBoolean(PERMISSION_NEVER_ASK, false) && settings.getLong(PERMISSION_NEXT_ASK, 0L) < System.currentTimeMillis() && Permissions.canReadStorage(this) && !Permissions.hasAllAccess(this)) {
            UiTools.snackerMessageInfinite(this, getString(R.string.partial_content))?.setAction(R.string.more) {
                AllAccessPermissionDialog.newInstance().show(supportFragmentManager, AllAccessPermissionDialog::class.simpleName)
            }?.show()
            settings.putSingle(PERMISSION_NEXT_ASK, System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2))
        }
        configurationChanged(getScreenWidth())
    }


    private fun prepareActionBar() {
        toolbarIcon = findViewById(R.id.toolbar_icon)
        updateIncognitoModeIcon()
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
            setDisplayShowTitleEnabled(false)
        }
    }

    override fun onStart() {
        super.onStart()
        if (mediaLibrary.isInitiated) {
            /* Load media items from database and storage */
            if (scanNeeded && Permissions.canReadStorage(this) && !mediaLibrary.isWorking) this.reloadLibrary()
        }
    }

    override fun onStop() {
        super.onStop()
        if (changingConfigurations == 0) {
            /* Check for an ongoing scan that needs to be resumed during onResume */
            scanNeeded = mediaLibrary.isWorking
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val current = currentFragment
        supportFragmentManager.putFragment(outState, "current_fragment", current!!)
        outState.putInt(EXTRA_TARGET, currentFragmentId)
        super.onSaveInstanceState(outState)
    }

    override fun onRestart() {
        super.onRestart()
        /* Reload the latest preferences */
        reloadPreferences()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onBackPressed() {


        /* Close playlist search if open or Slide down the audio player if it is shown entirely. */
        if (isAudioPlayerReady && (audioPlayer.backPressed() || slideDownAudioPlayer()))
            return

        // If it's the directory view, a "backpressed" action shows a parent.
        val fragment = currentFragment
        if (fragment is BaseBrowserFragment && fragment.goBack()) {
            return
        }
        if (AndroidUtil.isNougatOrLater && isInMultiWindowMode) {
            UiTools.confirmExit(this)
            return
        }
        finish()
    }

    override fun startSupportActionMode(callback: ActionMode.Callback): ActionMode? {
        appBarLayout.setExpanded(true)
        return super.startSupportActionMode(callback)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.ml_menu_refresh)?.isVisible = Permissions.canReadStorage(this)
//        menu?.findItem(R.id.incognito_mode)?.isChecked = Settings.getInstance(this).getBoolean(KEY_INCOGNITO, false)
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Handle onClick form menu buttons
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.ml_menu_filter) UiTools.setKeyboardVisibility(appBarLayout, false)

        // Handle item selection
        return when (item.itemId) {
            // Refresh
            R.id.ml_menu_refresh -> {
                if (Permissions.canReadStorage(this)) forceRefresh()
                true
            }
//            R.id.incognito_mode -> {
//                lifecycleScope.launch {
//                    if (showPinIfNeeded()) return@launch
//                    Settings.getInstance (this@MainActivity).putSingle(KEY_INCOGNITO, !Settings.getInstance(this@MainActivity).getBoolean(KEY_INCOGNITO, false))
//                    item.isChecked = !item.isChecked
//                    updateIncognitoModeIcon()
//                }
//                true
//
            android.R.id.home ->
                // Slide down the audio player or toggle the sidebar
                slideDownAudioPlayer()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateIncognitoModeIcon() {
        val incognito = Settings.getInstance (this).getBoolean(KEY_INCOGNITO, false)
        toolbarIcon.setImageDrawable(ContextCompat.getDrawable(this, if (incognito) R.drawable.ic_incognito else R.drawable.icon))

    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return if (currentFragment is Filterable) {
            (currentFragment as Filterable).allowedToExpand()
        } else false
    }

    fun forceRefresh() {
        forceRefresh(currentFragment)
    }

    private fun forceRefresh(current: Fragment?) {
        if (!mediaLibrary.isWorking) {
            if (current != null && current is IRefreshable)
                (current as IRefreshable).refresh()
            else
                reloadLibrary()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (KPlayerBilling.getInstance(this.application).iabHelper.handleActivityResult(requestCode, resultCode, data)) return
        if (requestCode == ACTIVITY_RESULT_PREFERENCES) {
            when (resultCode) {
                RESULT_RESCAN -> this.reloadLibrary()
                RESULT_RESTART, RESULT_RESTART_APP -> {
                    val intent = Intent(this@MainActivity, if (resultCode == RESULT_RESTART_APP) StartActivity::class.java else MainActivity::class.java)
                    finish()
                    startActivity(intent)
                }
                RESULT_UPDATE_SEEN_MEDIA -> for (fragment in supportFragmentManager.fragments)
                    if (fragment is VideoGridFragment)
                        fragment.updateSeenMediaMarker()
                RESULT_UPDATE_ARTISTS -> {
                    val fragment = currentFragment
                    if (fragment is AudioBrowserFragment) fragment.viewModel.refresh()
                }
            }
        } else if (requestCode == ACTIVITY_RESULT_OPEN && resultCode == Activity.RESULT_OK) {
            MediaUtils.openUri(this, data!!.data)
        } else if (requestCode == ACTIVITY_RESULT_SECONDARY) {
            if (resultCode == RESULT_RESCAN) {
                forceRefresh(currentFragment)
            } else {
                scanNeeded = false
            }
        }
    }

    private val handler = Handler(Handler.Callback {
        forceRefresh()
        Log.e("main", "Main Tasks");
        // Toast.makeText(this@MainActivity, "Refreshing", Toast.LENGTH_SHORT).show();
        true
    })

    // Note. onKeyDown will not occur while moving within a list
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            toolbar.menu.findItem(R.id.ml_menu_filter).expandActionView()
        }
        return super.onKeyDown(keyCode, event)
    }
}
