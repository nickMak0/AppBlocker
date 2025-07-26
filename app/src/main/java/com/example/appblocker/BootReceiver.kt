package com.example.appblocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BOOT_RECEIVER", "Device rebooted.")

            // Optionally: re-show a notification or toast
            Toast.makeText(context, "AppBlocker: Please make sure Accessibility Service is enabled.", Toast.LENGTH_LONG).show()

            // Or: open MainActivity to let user enable services again
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context?.startActivity(launchIntent)
        }
    }
}
