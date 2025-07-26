package com.example.appblocker.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log

class AdultSiteBlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting VPN service...")

        if (vpnInterface == null) {
            setupVpn()
        }

        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()

            builder.setSession("AdultSiteBlockerVPN")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32) // Local tunnel address

                // ✅ Cloudflare Family DNS (blocks adult content)
                .addDnsServer("1.1.1.3")
                .addDnsServer("1.0.0.3")

            // ⚠️ Do NOT route all traffic to avoid breaking internet
            // Do not call: .addRoute("0.0.0.0", 0)

            // Optionally, just route DNS traffic if needed:
            //.addRoute("1.1.1.3", 32)
            //.addRoute("1.0.0.3", 32)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(android.system.OsConstants.AF_INET)
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                Log.d(TAG, "VPN interface established successfully.")
            } else {
                Log.e(TAG, "Failed to establish VPN interface.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "VPN setup failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null
        Log.d(TAG, "VPN service destroyed.")
    }

    companion object {
        private const val TAG = "AdultSiteBlockerVPN"

        fun prepareIntent(context: Context): Intent? {
            return prepare(context)
        }
    }
}
