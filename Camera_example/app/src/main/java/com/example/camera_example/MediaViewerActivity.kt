package com.example.camera_example

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.camera_example.databinding.ActivityMediaViewerBinding
import java.io.File

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private lateinit var mediaFile: File
    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
        isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
        mediaFile = File(path)

        binding.tvMediaName.text = mediaFile.name

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnShare.setOnClickListener { shareMedia() }

        if (isVideo) showVideo() else showPhoto()
    }

    private fun showPhoto() {
        binding.imgFullPhoto.visibility = View.VISIBLE
        Glide.with(this).load(mediaFile).into(binding.imgFullPhoto)
    }

    private fun showVideo() {
        binding.videoView.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE

        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)
        binding.videoView.setVideoURI(Uri.fromFile(mediaFile))
        binding.videoView.setOnPreparedListener { mp ->
            mp.isLooping = false
            binding.btnPlayPause.visibility = View.VISIBLE
        }
        binding.videoView.setOnCompletionListener {
            binding.btnPlayPause.visibility = View.VISIBLE
        }

        binding.btnPlayPause.setOnClickListener {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
                binding.btnPlayPause.visibility = View.VISIBLE
            } else {
                binding.videoView.start()
                binding.btnPlayPause.visibility = View.GONE
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete ${mediaFile.name}?")
            .setPositiveButton("Delete") { _, _ ->
                if (mediaFile.delete()) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Could not delete file", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareMedia() {
        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", mediaFile
        )
        val mime = if (isVideo) "video/mp4" else "image/jpeg"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    companion object {
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_IS_VIDEO = "extra_is_video"
    }
}
