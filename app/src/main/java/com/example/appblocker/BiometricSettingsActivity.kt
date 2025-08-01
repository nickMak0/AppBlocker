package com.example.appblocker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.databinding.ActivityBiometricSettingsBinding
import com.example.appblocker.utils.BiometricUtils

class BiometricSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiometricSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiometricSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updateBiometricStatus()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !BiometricUtils.isBiometricAvailable(this)) {
                binding.biometricSwitch.isChecked = false
                Toast.makeText(this, "Biometric authentication not available on this device", Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            
            BiometricUtils.setBiometricEnabled(this, isChecked)
            updateBiometricStatus()
            
            val message = if (isChecked) "Biometric authentication enabled" else "Biometric authentication disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBiometricStatus() {
        val isEnabled = BiometricUtils.isBiometricEnabled(this)
        val isAvailable = BiometricUtils.isBiometricAvailable(this)
        
        binding.biometricSwitch.isChecked = isEnabled
        
        if (isAvailable) {
            binding.biometricStatusText.text = if (isEnabled) "Biometric authentication is enabled" else "Biometric authentication is disabled"
            binding.biometricSwitch.isEnabled = true
        } else {
            binding.biometricStatusText.text = "Biometric authentication not available"
            binding.biometricSwitch.isEnabled = false
        }
    }
}