package com.example.appblocker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.appblocker.utils.DnsFilter
import com.example.appblocker.utils.StatsManager
import java.io.*
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class WebFilterService : Service() {
    
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private lateinit var dnsFilter: DnsFilter
    
    override fun onCreate() {
        super.onCreate()
        dnsFilter = DnsFilter(this)
        Log.d("WEB_FILTER", "WebFilterService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.get()) {
            startProxy()
        }
        return START_STICKY
    }
    
    private fun startProxy() {
        Thread {
            try {
                serverSocket = ServerSocket(8888)
                isRunning.set(true)
                Log.d("WEB_FILTER", "Proxy server started on port 8888")
                
                while (isRunning.get()) {
                    val clientSocket = serverSocket?.accept()
                    if (clientSocket != null) {
                        Thread { handleClient(clientSocket) }.start()
                    }
                }
            } catch (e: Exception) {
                Log.e("WEB_FILTER", "Proxy server error", e)
            }
        }.start()
    }
    
    private fun handleClient(clientSocket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val output = PrintWriter(clientSocket.getOutputStream(), true)
            
            val requestLine = input.readLine()
            if (requestLine != null) {
                val parts = requestLine.split(" ")
                if (parts.size >= 2) {
                    val url = parts[1]
                    val host = extractHost(url)
                    
                    if (host != null && dnsFilter.shouldBlockDomain(host)) {
                        StatsManager.incrementSitesBlocked(this)
                        Log.d("WEB_FILTER", "Blocked request to: $host")
                        sendBlockedResponse(output)
                    } else {
                        // Forward request to actual server
                        forwardRequest(requestLine, input, output, host)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WEB_FILTER", "Error handling client", e)
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun extractHost(url: String): String? {
        return try {
            if (url.startsWith("http")) {
                URL(url).host
            } else {
                // For CONNECT requests
                url.split(":")[0]
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun sendBlockedResponse(output: PrintWriter) {
        val blockedHtml = """
            <html>
            <head><title>Site Blocked</title></head>
            <body style="text-align:center; font-family:Arial;">
            <h1>🚫 Site Blocked</h1>
            <p>This website has been blocked by AppBlocker.</p>
            </body>
            </html>
        """.trimIndent()
        
        output.println("HTTP/1.1 200 OK")
        output.println("Content-Type: text/html")
        output.println("Content-Length: ${blockedHtml.length}")
        output.println()
        output.println(blockedHtml)
    }
    
    private fun forwardRequest(requestLine: String, input: BufferedReader, output: PrintWriter, host: String?) {
        // Simple implementation - just return a basic response
        // In a full implementation, you would forward to the actual server
        output.println("HTTP/1.1 200 OK")
        output.println("Content-Type: text/html")
        output.println()
        output.println("<html><body><h1>Request forwarded</h1></body></html>")
    }
    
    override fun onDestroy() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        super.onDestroy()
        Log.d("WEB_FILTER", "WebFilterService stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}