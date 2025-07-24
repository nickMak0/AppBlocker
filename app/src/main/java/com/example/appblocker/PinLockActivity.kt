package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.utils.PinUtils

class PinLockActivity : AppCompatActivity() {

    private lateinit var pinEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var pinTitle: TextView

    private var isSettingUp = false
    private var firstPinEntry: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppBlocker)
        setContentView(R.layout.activity_pin_lock)

        pinEditText = findViewById(R.id.pinEditText)
        submitButton = findViewById(R.id.confirmPinButton)
        pinTitle = findViewById(R.id.pinTitle)

        isSettingUp = !PinUtils.isPinSetup(this)

        if (isSettingUp) {
            pinTitle.text = "Set a 4-digit PIN"
        } else {
            pinTitle.text = "Enter your PIN"
        }

        submitButton.setOnClickListener {
            val pin = pinEditText.text.toString()

            if (pin.length != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSettingUp) {
                handlePinSetup(pin)
            } else {
                handlePinVerification(pin)
            }
        }
    }

    private fun handlePinSetup(pin: String) {
        if (firstPinEntry == null) {
            firstPinEntry = pin
            pinEditText.text.clear()
            pinTitle.text = "Confirm your PIN"
        } else {
            if (pin == firstPinEntry) {
                PinUtils.savePin(this, pin)
                Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                finishWithSuccess()
            } else {
                Toast.makeText(this, "PINs do not match. Try again.", Toast.LENGTH_SHORT).show()
                firstPinEntry = null
                pinTitle.text = "Set a 4-digit PIN"
                pinEditText.text.clear()
            }
        }
    }

    private fun handlePinVerification(pin: String) {
        if (PinUtils.verifyPin(this, pin)) {
            Toast.makeText(this, "Access granted", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Saved PIN. isPinSetup = ${PinUtils.isPinSetup(this)}", Toast.LENGTH_LONG).show()
            finishWithSuccess()
        } else {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            pinEditText.text.clear()
        }
    }

    private fun finishWithSuccess() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val PIN_REQUEST_CODE = 101

        fun launch(context: Context, activity: AppCompatActivity) {
            val intent = Intent(context, PinLockActivity::class.java)
            activity.startActivityForResult(intent, PIN_REQUEST_CODE)
        }
    }
}
