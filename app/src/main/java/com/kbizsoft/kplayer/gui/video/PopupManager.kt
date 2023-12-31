/*
 * ************************************************************************
 *  PopupManager.java
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

package com.kbizsoft.KPlayer.gui.video

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.view.GestureDetectorCompat
import com.kbizsoft.libkplayer.MediaPlayer
import com.kbizsoft.libkplayer.interfaces.IMedia
import com.kbizsoft.libkplayer.interfaces.IKPlayerVout
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.resources.ACTION_REMOTE_PLAYPAUSE
import com.kbizsoft.resources.ACTION_REMOTE_STOP
import com.kbizsoft.resources.ACTION_REMOTE_SWITCH_VIDEO
import com.kbizsoft.resources.util.startForegroundCompat
import com.kbizsoft.resources.util.stopForegroundCompat
import com.kbizsoft.tools.POPUP_KEEPSCREEN
import com.kbizsoft.tools.Settings
import com.kbizsoft.tools.VIDEO_RESUME_TIME
import com.kbizsoft.tools.VIDEO_RESUME_URI
import com.kbizsoft.tools.putSingle
import com.kbizsoft.KPlayer.PlaybackService
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.gui.helpers.MISC_CHANNEL_ID
import com.kbizsoft.KPlayer.gui.view.PopupLayout
import com.kbizsoft.KPlayer.util.getPendingIntent
import kotlin.math.abs
import kotlin.math.floor

class PopupManager constructor(private val service: PlaybackService) : PlaybackService.Callback, GestureDetector.OnDoubleTapListener, View.OnClickListener, GestureDetector.OnGestureListener, IKPlayerVout.OnNewVideoLayoutListener, IKPlayerVout.Callback {

    private var rootView: PopupLayout? = null
    private lateinit var expandButton: ImageView
    private lateinit var closeButton: ImageView
    private lateinit var playPauseButton: ImageView
    private val alwaysOn: Boolean = Settings.getInstance(service).getBoolean(POPUP_KEEPSCREEN, false)

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val visibility = if (msg.what == SHOW_BUTTONS) View.VISIBLE else View.GONE
            playPauseButton.visibility = visibility
            closeButton.visibility = visibility
            expandButton.visibility = visibility
        }
    }

    fun removePopup() {
        hideNotification()
        val view = rootView ?: return
        service.removeCallback(this)
        val kplayerVout = service.vout
        kplayerVout?.detachViews()
        view.close()
        rootView = null
    }

    fun showPopup() {
        service.addCallback(this)
        val li = LayoutInflater.from(service.applicationContext)
        rootView = li.inflate(R.layout.video_popup, null) as PopupLayout
        val view = rootView ?: return
        if (alwaysOn) view.keepScreenOn = true
        playPauseButton = view.findViewById(R.id.video_play_pause)
        closeButton = view.findViewById(R.id.popup_close)
        expandButton = view.findViewById(R.id.popup_expand)
        playPauseButton.setOnClickListener(this)
        closeButton.setOnClickListener(this)
        expandButton.setOnClickListener(this)

        val gestureDetector = GestureDetectorCompat(service, this)
        gestureDetector.setOnDoubleTapListener(this)
        view.setGestureDetector(gestureDetector)

        val kplayerVout = service.vout ?: return
        kplayerVout.setVideoView(view.findViewById<View>(R.id.player_surface) as SurfaceView)
        kplayerVout.addCallback(this)
        kplayerVout.attachViews(this)
        view.setKPlayerVOut(kplayerVout)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (playPauseButton.visibility == View.VISIBLE) return false
        handler.sendEmptyMessage(SHOW_BUTTONS)
        handler.sendEmptyMessageDelayed(HIDE_BUTTONS, MSG_DELAY)
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        expandToVideoPlayer()
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }


    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (abs(velocityX) > FLING_STOP_VELOCITY || velocityY > FLING_STOP_VELOCITY) {
            stopPlayback()
            return true
        }
        return false
    }

    override fun onNewVideoLayout(kplayerVout: IKPlayerVout, width: Int, height: Int,
                                  visibleWidth: Int, visibleHeight: Int, sarNum: Int, sarDen: Int) {
        val view = rootView ?: return
        val displayW = view.width
        val displayH = view.height

        // sanity check
        if (displayW * displayH == 0) {
            Log.e(TAG, "Invalid surface size")
            return
        }

        if (width == 0 || height == 0) {
            view.setViewSize(displayW, displayH)
            return
        }

        // compute the aspect ratio
        var dw = displayW.toDouble()
        var dh = displayH.toDouble()
        val ar = if (sarDen == sarNum) {
            /* No indication about the density, assuming 1:1 */
            visibleWidth.toDouble() / visibleHeight.toDouble()
        } else {
            /* Use the specified aspect ratio */
            val vw = visibleWidth * sarNum.toDouble() / sarDen
            vw / visibleHeight
        }

        // compute the display aspect ratio
        val dar = dw / dh
        if (dar < ar)
            dh = dw / ar
        else
            dw = dh * ar

        view.setViewSize(floor(dw).toInt(), floor(dh).toInt())
    }

    override fun update() {}

    override fun onMediaEvent(event: IMedia.Event) {}

    override fun onMediaPlayerEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                rootView?.let { view ->
                    if (!alwaysOn) view.keepScreenOn = true
                    playPauseButton.setImageResource(R.drawable.ic_popup_pause)
                }
                showNotification()
            }
            MediaPlayer.Event.Paused -> {
                rootView?.let { view ->
                    if (!alwaysOn) view.keepScreenOn = false
                    playPauseButton.setImageResource(R.drawable.ic_popup_play)
                }
                showNotification()
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.video_play_pause -> if (service.hasMedia()) {
                if (service.isPlaying)
                    service.pause()
                else
                    service.play()
            }
            R.id.popup_close -> stopPlayback()
            R.id.popup_expand -> expandToVideoPlayer()
        }
    }

    private fun expandToVideoPlayer() {
        removePopup()
        if (service.hasMedia() && !service.isPlaying)
            service.currentMediaWrapper?.let { mw -> mw.flags = MediaWrapper.MEDIA_PAUSED }
        service.switchToVideo()
    }

    private fun stopPlayback() {
        val time = service.getTime()
        if (time != -1L) {
            // remove saved position if in the last 5 seconds
            // else, go back 2 seconds, to compensate loading time
            val resumeTime = if (service.length - time < 5000L) 0L else 2000L
            // Save position
            if (service.isSeekable) {
                Settings.getInstance(service).putSingle(VIDEO_RESUME_TIME, resumeTime)
                service.currentMediaLocation?.let { Settings.getInstance(service).putSingle(VIDEO_RESUME_URI, it) }
            }
        }
        service.stop()
    }

    private fun showNotification() {
        val piStop = service.getPendingIntent(Intent(ACTION_REMOTE_STOP))
        val builder = NotificationCompat.Builder(service, MISC_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_video)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(service.title)
                .setContentText(service.getString(R.string.popup_playback))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(service.sessionPendingIntent)
                .setDeleteIntent(piStop)

        //Switch
        val notificationIntent = Intent(ACTION_REMOTE_SWITCH_VIDEO)
        val piExpand = service.getPendingIntent(notificationIntent)
        //PLay Pause
        val piPlay = service.getPendingIntent(Intent(ACTION_REMOTE_PLAYPAUSE))

        if (service.isPlaying)
            builder.addAction(R.drawable.ic_popup_pause, service.getString(R.string.pause), piPlay)
        else
            builder.addAction(R.drawable.ic_popup_play, service.getString(R.string.play), piPlay)
        builder.addAction(R.drawable.ic_popup_fullscreen, service.getString(R.string.popup_expand), piExpand)
        service.startForegroundCompat(42, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    }

    private fun hideNotification() {
        service.stopForegroundCompat()
    }

    override fun onSurfacesCreated(kplayerVout: IKPlayerVout) {
        service.setVideoAspectRatio(null)
        service.setVideoScale(0f)
        service.setVideoTrackEnabled(true)
        if (service.hasMedia()) {
            service.flush()
            playPauseButton.setImageResource(if (service.isPlaying) R.drawable.ic_popup_pause else R.drawable.ic_popup_play)
        } else
            service.playIndex(service.currentMediaPosition)
        showNotification()
    }

    override fun onSurfacesDestroyed(kplayerVout: IKPlayerVout) {
        kplayerVout.removeCallback(this)
    }

    companion object {

        private const val TAG = "KPlayer/PopupManager"

        private const val FLING_STOP_VELOCITY = 3000f
        private const val MSG_DELAY = 3000L

        private const val SHOW_BUTTONS = 0
        private const val HIDE_BUTTONS = 1
    }
}
