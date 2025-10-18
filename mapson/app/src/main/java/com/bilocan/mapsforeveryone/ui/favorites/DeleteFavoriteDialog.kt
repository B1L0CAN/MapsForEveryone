package com.bilocan.mapsforeveryone.ui.favorites

import android.app.Dialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.data.FavoriteLocation

class DeleteFavoriteDialog(
    private val favorite: FavoriteLocation,
    private val onDelete: () -> Unit,
    private val tts: TextToSpeech?
) : DialogFragment() {

    private var deleteClickCount = 0
    private var cancelClickCount = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_delete_favorite, null)

        val messageTextView = view.findViewById<TextView>(R.id.delete_message)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        messageTextView.text = "${favorite.name} favori noktasını silmek istediğinize emin misiniz?"

        deleteButton.setOnClickListener {
            deleteClickCount++
            if (deleteClickCount == 1) {
                tts?.speak("Favori noktayı silmek için tekrar tıklayın", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (deleteClickCount == 2) {
                onDelete()
                dismiss()
                deleteClickCount = 0
            }
        }

        cancelButton.setOnClickListener {
            cancelClickCount++
            if (cancelClickCount == 1) {
                tts?.speak("İptal etmek için tekrar tıklayın", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (cancelClickCount == 2) {
                dismiss()
                cancelClickCount = 0
            }
        }

        // 2 saniye içinde ikinci tıklama yapılmazsa sayaçları sıfırla
        view.postDelayed({
            deleteClickCount = 0
            cancelClickCount = 0
        }, 2000)

        builder.setView(view)
        return builder.create()
    }
} 