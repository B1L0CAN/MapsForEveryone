package com.bilocan.mapsforeveryone.ui.favorites

import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.data.FavoriteLocation

class FavoritesAdapter(
    private val onEditClick: (FavoriteLocation) -> Unit,
    private val onDeleteClick: (FavoriteLocation) -> Unit,
    private val onNavigateClick: (FavoriteLocation) -> Unit,
    private val tts: TextToSpeech?
) : ListAdapter<FavoriteLocation, FavoritesAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = getItem(position)
        holder.bind(favorite)
    }

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.favoriteName)
        private val addressTextView: TextView = itemView.findViewById(R.id.favoriteAddress)
        private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        
        fun bind(favorite: FavoriteLocation) {
            nameTextView.text = favorite.name
            addressTextView.text = favorite.address

            // Kartın tamamına tıklama - doğrudan onNavigateClick çağırıyoruz,
            // çünkü FavoritesFragment'ta handleDoubleClick yöntemi zaten uygulanıyor
            itemView.setOnClickListener {
                onNavigateClick(favorite) 
            }

            // Düzenle butonu
            editButton.setOnClickListener {
                onEditClick(favorite)
            }

            // Sil butonu
            deleteButton.setOnClickListener {
                onDeleteClick(favorite)
            }
        }
    }

    private class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteLocation>() {
        override fun areItemsTheSame(oldItem: FavoriteLocation, newItem: FavoriteLocation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoriteLocation, newItem: FavoriteLocation): Boolean {
            return oldItem == newItem
        }
    }
} 