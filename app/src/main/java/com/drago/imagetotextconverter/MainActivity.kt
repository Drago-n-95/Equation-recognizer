package com.drago.imagetotextconverter

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.drago.imagetotextconverter.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    lateinit var binding: ActivityMainBinding

    private val REQUEST_IMAGE_CAPTURE=1

    private var imageBitmap: Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.apply {

            captureImage.setOnClickListener {
                takeImage()

                textView.text = ""
             }

            detectTextImageBtn.setOnClickListener{
                processImage()
            }
        }

    }

    private fun processImage() {
        if (imageBitmap!=null){
            val image=imageBitmap?.let{
                InputImage.fromBitmap(it, 0)
            }

            image?.let{
                recognizer.process(it).addOnSuccessListener { visionText ->
                    //binding.textView.text=it.text
                    val recognizedText = visionText.text
                    sendTextToChatGPT(recognizedText)
                }.addOnFailureListener {
                    Toast.makeText(this, "Nothing to show", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else{
            Toast.makeText(this, "Please select image first", Toast.LENGTH_SHORT).show()
        }

    }

    private fun performGoogleSearch(query: String) {
        // Create a Google search URL
        val googleSearchUrl = "https://www.google.com/search?q=${query.replace(" ", "+")}"

        // Open the URL in a web browser or WebView
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(googleSearchUrl)
        startActivity(intent)
    }

    // Import the khttp library in your build.gradle file
// implementation 'io.github.rybalkinsd:kohttp:0.14.0'

    private fun sendTextToChatGPT(text: String) {
        val apiKey = "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"
        //AIzaSyDCvz20aFOHAuAReh0TL1NHy-JPw2Ovbfs
        val endpoint = "https://api.openai.com/v1/engines/davinci-codex/completions"
        val maxTokens = 50 // Adjust as needed

        val client = OkHttpClient()

        // Create a JSON request body
        val json = """
        {
            "prompt": "$text",
            "max_tokens": $maxTokens
        }
    """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())

        // Create a POST request with headers
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                // Handle the response data here
                binding.textView.text = responseData
            } else {
                // Handle the error or non-successful response here
                Toast.makeText(this, "ChatGPT request failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            // Handle exceptions here (e.g., network errors)
        }
    }



    private fun takeImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try{
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
        catch(e:Exception){}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK){
            val extras: Bundle?=data?.extras

            imageBitmap = extras?.get("data") as Bitmap

            if(imageBitmap!=null) {
                binding.imageView.setImageBitmap(imageBitmap)
            }
        }
    }
}