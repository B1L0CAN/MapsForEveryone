package com.bilocan.mapsforeveryone.ui.favorites

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bilocan.mapsforeveryone.R
import com.bilocan.mapsforeveryone.data.FavoriteLocation

class AddFavoriteDialog : DialogFragment() {
    private var listener: OnFavoriteAddedListener? = null
    private lateinit var nameEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    interface OnFavoriteAddedListener {
        fun onFavoriteAdded(name: String, address: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as? OnFavoriteAddedListener
            if (listener == null) {
                throw ClassCastException("Parent fragment must implement OnFavoriteAddedListener")
            }
        } catch (e: ClassCastException) {
            throw ClassCastException("Parent fragment must implement OnFavoriteAddedListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_add_favorite, null)

            nameEditText = view.findViewById(R.id.nameEditText)
            addressEditText = view.findViewById(R.id.addressEditText)
            saveButton = view.findViewById(R.id.saveButton)
            cancelButton = view.findViewById(R.id.cancelButton)

            // İsim alanına odaklan
            nameEditText.requestFocus()

            saveButton.setOnClickListener {
                val name = nameEditText.text.toString().trim()
                val address = addressEditText.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(context, "Lütfen bir isim girin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (address.isEmpty()) {
                    Toast.makeText(context, "Lütfen bir adres girin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                listener?.onFavoriteAdded(name, address)
                dismiss()
            }

            cancelButton.setOnClickListener {
                dismiss()
            }

            builder.setView(view)
            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_favorite, container, false)
    }
} 