package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.concurrent.TimeUnit

class SongAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.tvTitle.text    = song.title
        holder.tvArtist.text   = song.artist
        holder.tvDuration.text = song.duration.formatAsTime()
        holder.itemView.setOnClickListener { onSongClick(holder.adapterPosition) }
    }

    override fun getItemCount() = songs.size

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView    = itemView.findViewById(R.id.tv_song_title)
        val tvArtist: TextView   = itemView.findViewById(R.id.tv_song_artist)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_song_duration)
    }
}

fun Long.formatAsTime(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
