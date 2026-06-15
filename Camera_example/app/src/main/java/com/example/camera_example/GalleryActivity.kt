package com.example.camera_example

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.camera_example.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Gallery"
        }

        loadMedia()
    }

    private fun loadMedia() {
        val pictures = File(getExternalFilesDir(null), "Pictures")
        val videos = File(getExternalFilesDir(null), "Movies")

        val items = mutableListOf<MediaItem>()
        pictures.listFiles()?.forEach { items.add(MediaItem(it, false)) }
        videos.listFiles()?.forEach { items.add(MediaItem(it, true)) }
        items.sortByDescending { it.file.lastModified() }

        if (items.isEmpty()) {
            Toast.makeText(this, "No media found", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerGallery.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerGallery.adapter = GalleryAdapter(items) { item ->
            val intent = Intent(this, MediaViewerActivity::class.java).apply {
                putExtra(MediaViewerActivity.EXTRA_PATH, item.file.absolutePath)
                putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, item.isVideo)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedia()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
