package com.example.appblocker

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

class SiteBlockerVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private lateinit var dnsFilter: DnsFilter
    
    override fun onCreate() {
        super.onCreate()
        dnsFilter = DnsFilter(this)
        Log.d("VPN_SERVICE", "SiteBlockerVpnService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning.get()) return START_STICKY
        startVpn()
        return START_STICKY
    }
    
    private fun startVpn() {
        try {
            val builder = Builder()
                .setSession("AppBlocker DNS Filter")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.1")
                .addRoute("0.0.0.0", 0)
                .setBlocking(false)
            
            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e("VPN_SERVICE", "Failed to establish VPN interface")
                return
            }
            
            isRunning.set(true)
            Thread { runDnsServer() }.start()
            Thread { processPackets() }.start()
            Log.d("VPN_SERVICE", "VPN started successfully")
        } catch (e: Exception) {
            Log.e("VPN_SERVICE", "Error starting VPN", e)
        }
    }
    
    private fun runDnsServer() {
        try {
            val socket = DatagramSocket(53, InetAddress.getByName("10.0.0.1"))
            val buffer = ByteArray(512)
            
            while (isRunning.get()) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                
                val domain = parseDnsQuery(packet.data, packet.length)
                if (domain != null) {
                    Log.d("VPN_SERVICE", "DNS query for: $domain")
                    
                    if (dnsFilter.shouldBlockDomain(domain)) {
                        StatsManager.incrementSitesBlocked(this)
                        Log.d("VPN_SERVICE", "Blocked DNS query for: $domain")
                        sendBlockedDnsResponse(socket, packet, domain)
                    } else {
                        forwardDnsQuery(socket, packet)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VPN_SERVICE", "DNS server error", e)
        }
    }
    
    private fun parseDnsQuery(data: ByteArray, length: Int): String? {
        try {
            if (length < 12) return null
            
            val domain = StringBuilder()
            var pos = 12
            
            while (pos < length) {
                val labelLength = data[pos].toInt() and 0xFF
                if (labelLength == 0) break
                
                if (domain.isNotEmpty()) domain.append('.')
                pos++
                
                for (i in 0 until labelLength) {
                    if (pos >= length) break
                    domain.append((data[pos].toInt() and 0xFF).toChar())
                    pos++
                }
            }
            
            return if (domain.isNotEmpty()) domain.toString() else null
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun sendBlockedDnsResponse(socket: DatagramSocket, originalPacket: DatagramPacket, domain: String) {
        try {
            val response = createBlockedDnsResponse(originalPacket.data, originalPacket.length)
            val responsePacket = DatagramPacket(
                response, response.size,
                originalPacket.address, originalPacket.port
            )
            socket.send(responsePacket)
        } catch (e: Exception) {
            Log.e("VPN_SERVICE", "Error sending blocked DNS response", e)
        }
    }
    
    private fun createBlockedDnsResponse(query: ByteArray, length: Int): ByteArray {
        val response = query.copyOf(length + 16)
        
        // Set response flag
        response[2] = (response[2].toInt() or 0x80).toByte()
        
        // Set answer count to 1
        response[6] = 0
        response[7] = 1
        
        // Add answer section pointing to localhost (127.0.0.1)
        var pos = length
        response[pos++] = 0xC0.toByte() // Pointer to domain name
        response[pos++] = 0x0C.toByte()
        response[pos++] = 0x00.toByte() // Type A
        response[pos++] = 0x01.toByte()
        response[pos++] = 0x00.toByte() // Class IN
        response[pos++] = 0x01.toByte()
        response[pos++] = 0x00.toByte() // TTL
        response[pos++] = 0x00.toByte()
        response[pos++] = 0x00.toByte()
        response[pos++] = 0x3C.toByte()
        response[pos++] = 0x00.toByte() // Data length
        response[pos++] = 0x04.toByte()
        response[pos++] = 127.toByte()  // 127.0.0.1
        response[pos++] = 0.toByte()
        response[pos++] = 0.toByte()
        response[pos++] = 1.toByte()
        
        return response.copyOf(pos)
    }
    
    private fun forwardDnsQuery(socket: DatagramSocket, packet: DatagramPacket) {
        try {
            // Forward to Google DNS
            val forwardSocket = DatagramSocket()
            val forwardPacket = DatagramPacket(
                packet.data, packet.length,
                InetAddress.getByName("8.8.8.8"), 53
            )
            forwardSocket.send(forwardPacket)
            
            // Get response
            val responseBuffer = ByteArray(512)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            forwardSocket.receive(responsePacket)
            
            // Send back to client
            val clientResponse = DatagramPacket(
                responsePacket.data, responsePacket.length,
                packet.address, packet.port
            )
            socket.send(clientResponse)
            forwardSocket.close()
        } catch (e: Exception) {
            Log.e("VPN_SERVICE", "Error forwarding DNS query", e)
        }
    }
    
    private fun processPackets() {
        val vpnInput = FileInputStream(vpnInterface!!.fileDescriptor)
        val vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)
        
        while (isRunning.get()) {
            try {
                val length = vpnInput.read(packet.array())
                if (length > 0) {
                    // Forward all non-DNS packets normally
                    vpnOutput.write(packet.array(), 0, length)
                    packet.clear()
                }
            } catch (e: Exception) {
                Log.e("VPN_SERVICE", "Packet processing error", e)
                break
            }
        }
    }
    
    override fun onDestroy() {
        isRunning.set(false)
        vpnInterface?.close()
        super.onDestroy()
        Log.d("VPN_SERVICE", "VPN stopped")
    }
}