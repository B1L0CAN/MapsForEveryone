package com.bilocan.mapsforeveryone.ui.favorites

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.data.FavoriteLocation

class EditFavoriteDialog(
    private val favorite: FavoriteLocation,
    private val onSave: (FavoriteLocation) -> Unit,
    private val tts: TextToSpeech?
) : DialogFragment() {

    private var nameClickCount = 0
    private var notesClickCount = 0
    private var saveClickCount = 0
    private var cancelClickCount = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_favorite, null)

        val nameEditText = view.findViewById<EditText>(R.id.edit_name)
        val notesEditText = view.findViewById<EditText>(R.id.edit_notes)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        nameEditText.setText(favorite.name)
        notesEditText.setText(favorite.address)

        nameEditText.setOnClickListener {
            nameClickCount++
            if (nameClickCount == 1) {
                tts?.speak("İsim alanını düzenlemek için tekrar tıklayın", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (nameClickCount == 2) {
                nameEditText.isEnabled = true
                nameEditText.requestFocus()
                nameClickCount = 0
            }
        }

        notesEditText.setOnClickListener {
            notesClickCount++
            if (notesClickCount == 1) {
                tts?.speak("Notlar alanını düzenlemek için tekrar tıklayın", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (notesClickCount == 2) {
                notesEditText.isEnabled = true
                notesEditText.requestFocus()
                notesClickCount = 0
            }
        }

        saveButton.setOnClickListener {
            saveClickCount++
            if (saveClickCount == 1) {
                tts?.speak("Değişiklikleri kaydetmek için tekrar tıklayın", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (saveClickCount == 2) {
                val updatedFavorite = favorite.copy(
                    name = nameEditText.text.toString(),
                    address = notesEditText.text.toString()
                )
                onSave(updatedFavorite)
                dismiss()
                saveClickCount = 0
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
            nameClickCount = 0
            notesClickCount = 0
            saveClickCount = 0
            cancelClickCount = 0
        }, 2000)

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
} 