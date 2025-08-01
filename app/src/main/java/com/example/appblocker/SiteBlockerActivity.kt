package com.example.appblocker

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appblocker.adapters.BlockedSitesAdapter
import com.example.appblocker.utils.DnsFilter
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import android.widget.ImageView

class SiteBlockerActivity : AppCompatActivity() {
    
    private lateinit var adultSitesSwitch: SwitchMaterial
    private lateinit var customDomainInput: TextInputEditText
    private lateinit var addDomainButton: MaterialButton
    private lateinit var blockedSitesRecycler: RecyclerView
    private lateinit var dnsFilter: DnsFilter
    private lateinit var adapter: BlockedSitesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_blocker)
        
        initViews()
        setupRecyclerView()
        loadSettings()
        setupListeners()
    }
    
    private fun initViews() {
        adultSitesSwitch = findViewById(R.id.adultSitesSwitch)
        customDomainInput = findViewById(R.id.customDomainInput)
        addDomainButton = findViewById(R.id.addDomainButton)
        blockedSitesRecycler = findViewById(R.id.blockedSitesRecycler)
        dnsFilter = DnsFilter(this)
    }
    
    private fun setupRecyclerView() {
        adapter = BlockedSitesAdapter(
            domains = dnsFilter.getBlockedDomains().toMutableList(),
            onRemove = { domain ->
                dnsFilter.removeBlockedDomain(domain)
                refreshList()
            }
        )
        blockedSitesRecycler.layoutManager = LinearLayoutManager(this)
        blockedSitesRecycler.adapter = adapter
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("SiteBlockerPrefs", Context.MODE_PRIVATE)
        adultSitesSwitch.isChecked = prefs.getBoolean("block_adult_sites", true)
    }
    
    private fun setupListeners() {
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        adultSitesSwitch.setOnCheckedChangeListener { _, isChecked ->
            val prefs = getSharedPreferences("SiteBlockerPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("block_adult_sites", isChecked).apply()
            refreshList()
        }
        
        addDomainButton.setOnClickListener {
            val domain = customDomainInput.text.toString().trim()
            if (domain.isNotEmpty() && isValidDomain(domain)) {
                dnsFilter.addBlockedDomain(domain)
                customDomainInput.text?.clear()
                refreshList()
                Toast.makeText(this, "Domain added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Invalid domain", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isValidDomain(domain: String): Boolean {
        return domain.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9]\\.[a-zA-Z]{2,}$"))
    }
    
    private fun refreshList() {
        adapter.updateDomains(dnsFilter.getBlockedDomains().toList())
    }
}