package com.example.appblocker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.databinding.ActivityBlockScreenBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class BlockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockScreenBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var isStrictMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set window flags immediately for faster blocking
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        hideSystemUI()
        
        // Check if strict mode is enabled (for display purposes only)
        isStrictMode = intent.getBooleanExtra("strict_mode", false)
        
        // Always hide strict mode text
        binding.strictModeText.visibility = View.GONE
        
        // Always show close button immediately
        binding.closeButton.visibility = View.VISIBLE
        binding.closeButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }

        // Load quote asynchronously to avoid blocking UI
        loadQuoteAsync()
    }

    private fun loadQuoteAsync() {
        executor.execute {
            val quote = loadRandomQuote()
            runOnUiThread {
                binding.motivationQuote.text = quote
            }
        }
    }

    private fun loadRandomQuote(): String {
        return try {
            val inputStream = resources.openRawResource(R.raw.motivation_quotes)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val quotes = reader.readLines().filter { it.isNotBlank() }
            quotes.randomOrNull() ?: "Stay focused. Stay disciplined."
        } catch (e: Exception) {
            "Stay focused. Stay disciplined."
        }
    }

    override fun onBackPressed() {
        // Always allow back to home screen
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
