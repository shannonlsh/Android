package com.example.camera_example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(
    private val items: List<MediaItem>,
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgMedia)
        val videoIcon: ImageView = itemView.findViewById(R.id.imgVideoIcon)
        val duration: TextView = itemView.findViewById(R.id.tvDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        // Square cells
        view.layoutParams.height = parent.width / 3
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        Glide.with(holder.img.context)
            .load(item.file)
            .centerCrop()
            .into(holder.img)

        holder.videoIcon.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        holder.duration.visibility = View.GONE

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}
