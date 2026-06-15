package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    private val songs = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recycler_view)
        tvEmpty      = findViewById(R.id.tv_empty)

        adapter = SongAdapter(songs) { position ->
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putParcelableArrayListExtra(PlayerActivity.EXTRA_SONGS, ArrayList(songs))
                    .putExtra(PlayerActivity.EXTRA_POSITION, position)
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        requestPermissionAndLoad()
    }

    private fun requestPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                loadSongs()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to access your music library.",
                    Toast.LENGTH_LONG
                ).show()
                showEmpty()
            }
        }
    }

    private fun loadSongs() {
        songs.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                songs += Song(
                    id       = cursor.getLong(idCol),
                    title    = cursor.getString(titleCol),
                    artist   = cursor.getString(artistCol),
                    albumId  = cursor.getLong(albumIdCol),
                    duration = cursor.getLong(durationCol)
                )
            }
        }

        adapter.notifyDataSetChanged()
        if (songs.isEmpty()) showEmpty() else showList()
    }

    private fun showEmpty() {
        recyclerView.visibility = View.GONE
        tvEmpty.visibility      = View.VISIBLE
    }

    private fun showList() {
        recyclerView.visibility = View.VISIBLE
        tvEmpty.visibility      = View.GONE
    }
}
