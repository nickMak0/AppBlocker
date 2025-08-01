package com.example.appblocker.utils

import android.content.Context
import android.util.Log

class DnsFilter(private val context: Context) {
    
    private val blockedDomains = mutableSetOf<String>()
    private val adultSiteDomains = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com",
        "youporn.com", "tube8.com", "spankbang.com", "xhamster.com",
        "sex.com", "porn.com", "xxx.com", "adult.com"
    )
    
    init {
        loadBlockedDomains()
    }
    
    private fun loadBlockedDomains() {
        val prefs = context.getSharedPreferences("SiteBlockerPrefs", Context.MODE_PRIVATE)
        val customDomains = prefs.getStringSet("blocked_domains", emptySet()) ?: emptySet()
        val blockAdultSites = prefs.getBoolean("block_adult_sites", true)
        
        blockedDomains.clear()
        blockedDomains.addAll(customDomains)
        
        if (blockAdultSites) {
            blockedDomains.addAll(adultSiteDomains)
        }
        
        Log.d("DnsFilter", "Loaded ${blockedDomains.size} blocked domains")
    }
    
    fun shouldBlockDomain(domain: String): Boolean {
        loadBlockedDomains() // Refresh domains
        val isBlocked = isBlocked(domain)
        if (isBlocked) {
            Log.d("DnsFilter", "Blocking domain: $domain")
        }
        return isBlocked
    }
    
    private fun isBlocked(domain: String): Boolean {
        return blockedDomains.any { blockedDomain ->
            domain.equals(blockedDomain, ignoreCase = true) ||
            domain.endsWith(".$blockedDomain", ignoreCase = true)
        }
    }
    
    fun addBlockedDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
        saveBlockedDomains()
        Log.d("DnsFilter", "Added blocked domain: $domain")
    }
    
    fun removeBlockedDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
        saveBlockedDomains()
        Log.d("DnsFilter", "Removed blocked domain: $domain")
    }
    
    private fun saveBlockedDomains() {
        val prefs = context.getSharedPreferences("SiteBlockerPrefs", Context.MODE_PRIVATE)
        val customDomains = blockedDomains - adultSiteDomains
        prefs.edit().putStringSet("blocked_domains", customDomains).apply()
    }
    
    fun getBlockedDomains(): Set<String> = blockedDomains.toSet()
}