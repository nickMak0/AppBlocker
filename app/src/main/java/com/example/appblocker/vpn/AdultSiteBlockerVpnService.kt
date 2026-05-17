package com.example.appblocker.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.appblocker.utils.DnsFilter
import com.example.appblocker.utils.StatsManager
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
    private lateinit var dnsFilter: DnsFilter

    override fun onCreate() {
        super.onCreate()
        dnsFilter = DnsFilter(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called, isRunning=${isRunning.get()}")
        if (!isRunning.get()) {
            setupVpn()
        }
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .setSession("AppBlocker VPN")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.1")
                .addRoute("10.0.0.0", 24)
                .setMtu(1500)
                .setBlocking(true)

            try { builder.addDisallowedApplication(packageName) } catch (e: Exception) {
                Log.w(TAG, "Could not exclude own app: ${e.message}")
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                return
            }

            isRunning.set(true)
            vpnRunning = true
            Thread { processPackets() }.start()
            Log.d(TAG, "VPN started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "VPN setup failed: ${e.message}", e)
        }
    }

    private fun processPackets() {
        val vpnFd = vpnInterface ?: return
        val vpnInput = FileInputStream(vpnFd.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnFd.fileDescriptor)
        val buffer = ByteArray(32767)

        Log.d(TAG, "Packet processing loop started")

        while (isRunning.get()) {
            try {
                val length = vpnInput.read(buffer)
                if (length <= 0) continue
                val packet = buffer.copyOf(length)
                if (isDnsQuery(packet)) {
                    handleDnsPacket(packet, vpnOutput)
                }
            } catch (e: Exception) {
                if (isRunning.get()) Log.e(TAG, "Packet processing error: ${e.message}")
            }
        }

        Log.d(TAG, "Packet processing loop ended")
    }

    private fun isDnsQuery(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        val version = (packet[0].toInt() and 0xFF) shr 4
        if (version != 4) return false
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false

        if ((packet[16].toInt() and 0xFF) != 10) return false
        if ((packet[17].toInt() and 0xFF) != 0)  return false
        if ((packet[18].toInt() and 0xFF) != 0)  return false
        if ((packet[19].toInt() and 0xFF) != 1)  return false

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (packet.size < ihl + 8) return false
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    private fun handleDnsPacket(packet: ByteArray, output: FileOutputStream) {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        val clientPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dnsPayload = packet.copyOfRange(ihl + 8, packet.size)

        val domain = parseDnsQuery(dnsPayload)
        if (domain == null) {
            Log.w(TAG, "Could not parse DNS domain from query")
            return
        }

        Log.d(TAG, "DNS query for: $domain")

        if (dnsFilter.shouldBlockDomain(domain)) {
            Log.d(TAG, "BLOCKING: $domain")
            StatsManager.incrementSitesBlocked(this)
            val response = buildBlockedResponse(dnsPayload, clientPort)
            synchronized(output) { output.write(response) }
        } else {
            forwardAndRespond(dnsPayload, clientPort, output)
        }
    }

    private fun parseDnsQuery(dnsData: ByteArray): String? {
        return try {
            if (dnsData.size < 12) return null
            val domain = StringBuilder()
            var pos = 12
            while (pos < dnsData.size) {
                val labelLen = dnsData[pos].toInt() and 0xFF
                if (labelLen == 0) break
                if (pos + 1 + labelLen > dnsData.size) return null
                if (domain.isNotEmpty()) domain.append('.')
                pos++
                repeat(labelLen) {
                    domain.append((dnsData[pos + it].toInt() and 0xFF).toChar())
                }
                pos += labelLen
            }
            domain.toString().lowercase().ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildBlockedResponse(dnsQuery: ByteArray, clientPort: Int): ByteArray {
        val dnsResponse = dnsQuery.copyOf()
        dnsResponse[2] = (dnsResponse[2].toInt() or 0x80).toByte()
        dnsResponse[3] = ((dnsResponse[3].toInt() and 0xF0) or 0x03).toByte()
        dnsResponse[6] = 0
        dnsResponse[7] = 0

        return buildIpUdpPacket(
            srcIp   = byteArrayOf(10, 0, 0, 1),
            dstIp   = byteArrayOf(10, 0, 0, 2),
            srcPort = 53,
            dstPort = clientPort,
            payload = dnsResponse
        )
    }

    private fun forwardAndRespond(dnsQuery: ByteArray, clientPort: Int, output: FileOutputStream) {
        Thread {
            try {
                val socket = DatagramSocket()
                protect(socket) // CRITICAL: prevents infinite VPN routing loop

                val upstream = InetAddress.getByName("1.1.1.1")
                socket.soTimeout = 5000
                socket.send(DatagramPacket(dnsQuery, dnsQuery.size, upstream, 53))

                val responseBuf = ByteArray(1024)
                val responsePacket = DatagramPacket(responseBuf, responseBuf.size)
                socket.receive(responsePacket)
                socket.close()

                val dnsResponse = responsePacket.data.copyOf(responsePacket.length)
                val ipPacket = buildIpUdpPacket(
                    srcIp   = byteArrayOf(10, 0, 0, 1),
                    dstIp   = byteArrayOf(10, 0, 0, 2),
                    srcPort = 53,
                    dstPort = clientPort,
                    payload = dnsResponse
                )
                synchronized(output) { output.write(ipPacket) }

            } catch (e: Exception) {
                Log.e(TAG, "DNS forwarding error: ${e.message}")
            }
        }.start()
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val udpLen = 8 + payload.size
        val ipLen  = 20 + udpLen
        val buf    = ByteBuffer.allocate(ipLen)

        buf.put(0x45.toByte())
        buf.put(0x00.toByte())
        buf.putShort(ipLen.toShort())
        buf.putShort(0x0000.toShort())
        buf.putShort(0x4000.toShort())
        buf.put(0x40.toByte())
        buf.put(17.toByte())
        buf.putShort(0x0000.toShort())
        buf.put(srcIp)
        buf.put(dstIp)

        val checksum = ipv4Checksum(buf.array(), 0, 20)
        buf.array()[10] = (checksum shr 8).toByte()
        buf.array()[11] = checksum.toByte()

        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpLen.toShort())
        buf.putShort(0x0000.toShort())
        buf.put(payload)

        return buf.array()
    }

    private fun ipv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
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
        Log.d(TAG, "VPN service destroyed")
    }

    companion object {
        private const val TAG = "AdultSiteBlockerVPN"

        @Volatile
        var vpnRunning: Boolean = false
            private set

        fun prepareIntent(context: Context): Intent? = prepare(context)
    }
}
