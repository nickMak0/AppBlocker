package com.example.appblocker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.databinding.ActivityPinSettingsBinding
import com.example.appblocker.utils.PinUtils

class PinSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPinSettingsBinding
    
    private val pinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            updatePinStatus()
            Toast.makeText(this, "PIN updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updatePinStatus()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.setPinButton.setOnClickListener {
            val intent = Intent(this, PinLockActivity::class.java)
            pinLauncher.launch(intent)
        }
        
        binding.changePinButton.setOnClickListener {
            val intent = Intent(this, PinLockActivity::class.java)
            pinLauncher.launch(intent)
        }
        
        binding.disablePinButton.setOnClickListener {
            showDisablePinDialog()
        }
    }

    private fun updatePinStatus() {
        val isPinSetup = PinUtils.isPinSetup(this)
        
        if (isPinSetup) {
            binding.pinStatusText.text = "PIN is enabled"
            binding.setPinButton.visibility = android.view.View.GONE
            binding.changePinButton.visibility = android.view.View.VISIBLE
            binding.disablePinButton.visibility = android.view.View.VISIBLE
        } else {
            binding.pinStatusText.text = "PIN is disabled"
            binding.setPinButton.visibility = android.view.View.VISIBLE
            binding.changePinButton.visibility = android.view.View.GONE
            binding.disablePinButton.visibility = android.view.View.GONE
        }
    }

    private fun showDisablePinDialog() {
        AlertDialog.Builder(this)
            .setTitle("Disable PIN")
            .setMessage("Are you sure you want to disable PIN protection? This will remove security from accessing app settings.")
            .setPositiveButton("Disable") { _, _ ->
                disablePin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun disablePin() {
        val prefs = getSharedPreferences("AppBlockerPrefs", MODE_PRIVATE)
        prefs.edit()
            .remove("pin_code")
            .putBoolean("pin_setup_done", false)
            .apply()
        
        updatePinStatus()
        Toast.makeText(this, "PIN disabled", Toast.LENGTH_SHORT).show()
    }
}