package com.example.musicplayer

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SONGS    = "songs"
        const val EXTRA_POSITION = "position"
    }

    private var songs = arrayListOf<Song>()
    private var currentIndex = 0
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var ivAlbumArt: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton

    private val seekBarUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.takeIf { it.isPlaying }?.let {
                val pos = it.currentPosition
                seekBar.progress   = pos
                tvCurrentTime.text = pos.toLong().formatAsTime()
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        songs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_SONGS, Song::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_SONGS) ?: arrayListOf()
        }
        currentIndex = intent.getIntExtra(EXTRA_POSITION, 0)

        ivAlbumArt    = findViewById(R.id.iv_album_art)
        tvTitle       = findViewById(R.id.tv_title)
        tvArtist      = findViewById(R.id.tv_artist)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime   = findViewById(R.id.tv_total_time)
        seekBar       = findViewById(R.id.seek_bar)
        btnPrevious   = findViewById(R.id.btn_previous)
        btnPlayPause  = findViewById(R.id.btn_play_pause)
        btnNext       = findViewById(R.id.btn_next)

        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnPrevious.setOnClickListener  { playPrevious() }
        btnNext.setOnClickListener      { playNext() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentTime.text = progress.toLong().formatAsTime()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar) { handler.removeCallbacks(seekBarUpdater) }
            override fun onStopTrackingTouch(bar: SeekBar)  { handler.post(seekBarUpdater) }
        })

        playSong(currentIndex)
    }

    private fun playSong(index: Int) {
        if (songs.isEmpty()) return
        currentIndex = index
        val song = songs[currentIndex]

        tvTitle.text  = song.title
        tvArtist.text = song.artist
        loadAlbumArt(song)
        releasePlayer()

        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@PlayerActivity, songUri)
            prepare()
            seekBar.max        = duration
            seekBar.progress   = 0
            tvTotalTime.text   = duration.toLong().formatAsTime()
            tvCurrentTime.text = 0L.formatAsTime()
            setOnCompletionListener { playNext() }
            start()
        }
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        handler.post(seekBarUpdater)
    }

    private fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            handler.removeCallbacks(seekBarUpdater)
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        } else {
            player.start()
            handler.post(seekBarUpdater)
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    private fun playNext() {
        playSong((currentIndex + 1) % songs.size)
    }

    private fun playPrevious() {
        // Restart the current track if more than 3 seconds in
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            mediaPlayer?.seekTo(0)
            seekBar.progress   = 0
            tvCurrentTime.text = 0L.formatAsTime()
        } else {
            playSong((currentIndex - 1 + songs.size) % songs.size)
        }
    }

    private fun loadAlbumArt(song: Song) {
        Thread {
            val bitmap: Bitmap? = runCatching {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(this, uri)
                    retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                } finally {
                    retriever.release()
                }
            }.getOrNull()

            runOnUiThread {
                if (bitmap != null) ivAlbumArt.setImageBitmap(bitmap)
                else ivAlbumArt.setImageResource(R.drawable.ic_music_note)
            }
        }.start()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(seekBarUpdater)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
