package com.example.musicplayer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Long,
    val duration: Long
) : Parcelable
