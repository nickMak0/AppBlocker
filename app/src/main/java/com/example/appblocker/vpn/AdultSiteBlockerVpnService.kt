package com.example.appblocker.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class AdultSiteBlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)

    // Cloudflare Family DNS — blocks adult content natively, no custom filtering needed
    private val CLOUDFLARE_FAMILY_PRIMARY   = "1.1.1.3"
    private val CLOUDFLARE_FAMILY_SECONDARY = "1.0.0.3"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        if (!isRunning.get()) setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .setSession("AppBlocker VPN")
                .addAddress("10.0.0.2", 32)
                // Tell Android to use Cloudflare Family DNS
                .addDnsServer(CLOUDFLARE_FAMILY_PRIMARY)
                .addDnsServer(CLOUDFLARE_FAMILY_SECONDARY)
                // Route ONLY the Cloudflare DNS IPs through the tunnel
                // so regular internet traffic is completely unaffected
                .addRoute(CLOUDFLARE_FAMILY_PRIMARY, 32)
                .addRoute(CLOUDFLARE_FAMILY_SECONDARY, 32)
                .setMtu(1500)
                .setBlocking(true)

            try { builder.addDisallowedApplication(packageName) } catch (e: Exception) {}

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN")
                return
            }

            isRunning.set(true)
            vpnRunning = true
            Thread { processPackets() }.start()
            Log.d(TAG, "VPN started — Cloudflare Family DNS active")

        } catch (e: Exception) {
            Log.e(TAG, "VPN setup error: ${e.message}", e)
        }
    }

    /**
     * Reads DNS packets from the tun interface and forwards them to
     * Cloudflare Family DNS (1.1.1.3) via a protected socket.
     * Cloudflare Family automatically blocks adult/malicious content.
     * No custom filtering needed — just proxy the packets.
     */
    private fun processPackets() {
        val input  = FileInputStream(vpnInterface!!.fileDescriptor)
        val output = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buffer = ByteArray(32767)

        while (isRunning.get()) {
            try {
                val length = input.read(buffer)
                if (length <= 0) continue
                val packet = buffer.copyOf(length)
                if (isUdpDnsPacket(packet)) {
                    forwardToCloudflareFamily(packet, output)
                }
            } catch (e: Exception) {
                if (isRunning.get()) Log.e(TAG, "Packet error: ${e.message}")
            }
        }
    }

    /** Check if this is an IPv4 UDP packet on port 53 */
    private fun isUdpDnsPacket(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        if ((packet[0].toInt() and 0xFF) shr 4 != 4) return false  // IPv4
        if (packet[9].toInt() and 0xFF != 17) return false          // UDP
        val ihl = (packet[0].toInt() and 0x0F) * 4
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    /**
     * Forwards the DNS query to Cloudflare Family DNS (1.1.1.3) using a
     * protect()ed socket (bypasses VPN to avoid infinite loop), then writes
     * the response back into the tun interface so Android gets its answer.
     */
    private fun forwardToCloudflareFamily(packet: ByteArray, output: FileOutputStream) {
        Thread {
            try {
                val ihl       = (packet[0].toInt() and 0x0F) * 4
                val srcPort   = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
                val deviceIp  = packet.copyOfRange(12, 16)  // original source IP
                val dnsPayload = packet.copyOfRange(ihl + 8, packet.size)

                val socket = DatagramSocket()
                protect(socket)  // bypass VPN routing — critical!
                socket.soTimeout = 5000

                // Send to Cloudflare Family — it handles adult site blocking
                val cloudflare = InetAddress.getByName(CLOUDFLARE_FAMILY_PRIMARY)
                socket.send(DatagramPacket(dnsPayload, dnsPayload.size, cloudflare, 53))

                val buf = ByteArray(1024)
                val resp = DatagramPacket(buf, buf.size)
                socket.receive(resp)
                socket.close()

                // Wrap response in IP+UDP and send back through tun
                val responseData = resp.data.copyOf(resp.length)
                val cloudflareIp = cloudflare.address  // 1.1.1.3 as bytes
                val ipPacket = buildIpUdpPacket(
                    srcIp   = cloudflareIp,
                    dstIp   = deviceIp,
                    srcPort = 53,
                    dstPort = srcPort,
                    payload = responseData
                )
                synchronized(output) { output.write(ipPacket) }

            } catch (e: Exception) {
                Log.e(TAG, "Forward to Cloudflare failed: ${e.message}")
            }
        }.start()
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray, dstIp: ByteArray,
        srcPort: Int, dstPort: Int, payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val ipLen  = 20 + udpLen
        val buf    = ByteBuffer.allocate(ipLen)

        buf.put(0x45.toByte()); buf.put(0)
        buf.putShort(ipLen.toShort())
        buf.putShort(0); buf.putShort(0x4000.toShort())
        buf.put(0x40.toByte()); buf.put(17)
        buf.putShort(0); buf.put(srcIp); buf.put(dstIp)

        val cs = ipv4Checksum(buf.array(), 0, 20)
        buf.array()[10] = (cs shr 8).toByte()
        buf.array()[11] = cs.toByte()

        buf.putShort(srcPort.toShort()); buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort()); buf.putShort(0)
        buf.put(payload)

        return buf.array()
    }

    private fun ipv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0; var i = offset
        while (i < offset + length - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF); i += 2
        }
        if (length % 2 != 0) sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    override fun onDestroy() {
        isRunning.set(false)
        vpnRunning = false
        try { vpnInterface?.close() } catch (e: Exception) {}
        vpnInterface = null
        super.onDestroy()
        Log.d(TAG, "VPN stopped")
    }

    companion object {
        private const val TAG = "AdultSiteBlockerVPN"

        @Volatile var vpnRunning = false
            private set

        fun prepareIntent(context: Context): Intent? = prepare(context)
    }
}
