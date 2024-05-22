package ali.emir.merhaba.grmeengelliuygulama

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class LocationInputActivity : AppCompatActivity() {
    private lateinit var editTextLocation: EditText
    private lateinit var buttonNavigate: Button
    private lateinit var buttonVoiceMessage: Button
    private val REQUEST_CODE_SPEECH_INPUT = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_input)


        editTextLocation = findViewById(R.id.editTextLocation)
        buttonNavigate = findViewById(R.id.buttonNavigate)
        buttonVoiceMessage = findViewById(R.id.buttonVoiceMessage)


        buttonNavigate.setOnClickListener {
            startNavigation()
        }


        buttonVoiceMessage.setOnClickListener {
            startVoiceRecognition()
        }
    }


    private fun startNavigation() {
        val location = editTextLocation.text.toString()
        Log.d("LocationInputActivity", "Selected location: $location") // Log ekle
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("destination", location)
        }
        startActivity(intent)
    }


    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Konuşmaya başlayın")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                editTextLocation.setText(result[0])
                startNavigation()
            }
        }
    }
}
