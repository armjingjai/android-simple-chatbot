package com.armstyle.customchatbot

import ai.api.AIConfiguration
import ai.api.AIListener
import ai.api.android.AIService
import ai.api.model.AIError
import ai.api.model.AIResponse
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_voice.*


class VoiceActivity : AppCompatActivity() , AIListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)

        val config = ai.api.android.AIConfiguration(
            "access token",
            AIConfiguration.SupportedLanguages.English,
            ai.api.android.AIConfiguration.RecognitionEngine.System
        )

        var aiService: AIService? = null
        aiService = AIService.getService(this, config)
        aiService.setListener(this)

        voiceProcess.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this, permissions,0)
            } else {
                aiService.startListening()
            }

        }
    }

    @SuppressLint("SetTextI18n")
    override fun onResult(result: AIResponse?) {
        // Show results in TextView.
        if (result != null) {
            textContent.text = """
                            Query:${result.result.resolvedQuery}
                            Action: ${result.result.action}
                            Parameters: ${result.result.parameters.keys} , ${result.result.parameters.values}
                            Result: ${result.result.fulfillment.speech}
                            """.trimIndent()
        }
    }
    override fun onListeningStarted() {
    }
    override fun onAudioLevel(level: Float) {
    }
    override fun onError(error: AIError?) {
        textContent.text = error.toString()
    }
    override fun onListeningCanceled() {
    }
    override fun onListeningFinished() {
    }
}
