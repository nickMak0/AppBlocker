package com.example.appblocker

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.databinding.ActivityBreakModeBinding

class BreakModeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreakModeBinding
    private var countDownTimer: CountDownTimer? = null
    private val breakDuration = 5 * 60 * 1000L // 5 minutes in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreakModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        startBreakTimer()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.endBreakButton.setOnClickListener {
            endBreakMode()
        }
    }

    private fun startBreakTimer() {
        countDownTimer = object : CountDownTimer(breakDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
                binding.progressBar.progress = ((breakDuration - millisUntilFinished) * 100 / breakDuration).toInt()
            }

            override fun onFinish() {
                endBreakMode()
            }
        }.start()
    }

    private fun endBreakMode() {
        countDownTimer?.cancel()
        
        // Re-enable app blocking
        val prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("break_mode_active", false).apply()
        
        android.widget.Toast.makeText(this, "Break time ended - App blocking resumed", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}