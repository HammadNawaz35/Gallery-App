package com.gallery.app.data.service

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSessionService

/**
 * Background service for video playback (Picture-in-Picture, background audio).
 */
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: androidx.media3.session.MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = androidx.media3.session.MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo) =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run { player.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}
