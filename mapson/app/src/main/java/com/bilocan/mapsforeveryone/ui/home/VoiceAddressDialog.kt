package com.bilocan.mapsforeveryone.ui.home

import android.app.Dialog
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bilocan.mapsforeveryone.R

class VoiceAddressDialog(
    private val address: String,
    private val tts: TextToSpeech?,
    private val onApprove: (String) -> Unit,
    private val onRetry: () -> Unit
) : DialogFragment() {
    private var lastClickTimeApprove = 0L
    private var lastClickTimeRetry = 0L
    private var lastClickedApprove: View? = null
    private var lastClickedRetry: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_voice_address, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val title = view.findViewById<TextView>(R.id.dialogTitle)
        val message = view.findViewById<TextView>(R.id.dialogMessage)
        val btnApprove = view.findViewById<Button>(R.id.btnApprove)
        val btnRetry = view.findViewById<Button>(R.id.btnRetry)

        title.text = address
        message.text = "$address adresini söylediniz, onaylıyor musunuz?"
        speak("$address adresini söylediniz, onaylıyor musunuz?")

        btnApprove.setOnClickListener { v ->
            val now = System.currentTimeMillis()
            if (lastClickedApprove == v && now - lastClickTimeApprove < 500) {
                onApprove(address)
                dismiss()
            } else {
                speak("Onayla butonu. Çift tıklayarak onaylayabilirsiniz.")
                lastClickTimeApprove = now
                lastClickedApprove = v
            }
        }
        btnRetry.setOnClickListener { v ->
            val now = System.currentTimeMillis()
            if (lastClickedRetry == v && now - lastClickTimeRetry < 500) {
                onRetry()
                dismiss()
            } else {
                speak("Bir daha komut ver butonu. Çift tıklayarak tekrar deneyebilirsiniz.")
                lastClickTimeRetry = now
                lastClickedRetry = v
            }
        }
        return dialog
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
} 