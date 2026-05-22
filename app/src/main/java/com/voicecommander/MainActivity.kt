package com.voicecommander

import android.Manifest
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voicecommander.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("VoiceCommanderPrefs", Context.MODE_PRIVATE)

        setupButtons()
        setupSpinner()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupButtons() {
        binding.btnReqMic.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
        binding.btnReqCam.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 102)
        }
        binding.btnReqOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
        binding.btnReqA11y.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, getString(R.string.accessibility_instruction), Toast.LENGTH_LONG).show()
            }
        }
        binding.btnReqAdmin.setOnClickListener {
            if (!isDeviceAdminEnabled()) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this@MainActivity, AdminReceiver::class.java))
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_description))
                }
                startActivity(intent)
            }
        }

        binding.btnToggleService.setOnClickListener {
            if (isServiceRunning(FloatingService::class.java)) {
                stopService(Intent(this, FloatingService::class.java))
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, getString(R.string.overlay_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val intent = Intent(this, FloatingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            binding.root.postDelayed({ updateStatus() }, 500)
        }
    }

    private fun setupSpinner() {
        val options = arrayOf("Arabic (العربية)", "English", "Both (Arabic First)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.spinnerLanguage.adapter = adapter

        val savedLang = sharedPrefs.getInt("language_pref", 0)
        binding.spinnerLanguage.setSelection(savedLang)

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPrefs.edit().putInt("language_pref", position).apply()
                // Restart service if running so it picks up the new language immediately
                if (isServiceRunning(FloatingService::class.java)) {
                    stopService(Intent(this@MainActivity, FloatingService::class.java))
                    val intent = Intent(this@MainActivity, FloatingService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateStatus() {
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val hasA11y = isAccessibilityEnabled()
        val hasAdmin = isDeviceAdminEnabled()

        binding.btnReqMic.text = if (hasMic) getString(R.string.status_granted) else getString(R.string.btn_grant)
        binding.btnReqMic.isEnabled = !hasMic

        binding.btnReqCam.text = if (hasCam) getString(R.string.status_granted) else getString(R.string.btn_grant)
        binding.btnReqCam.isEnabled = !hasCam

        binding.btnReqOverlay.text = if (hasOverlay) getString(R.string.status_granted) else getString(R.string.btn_grant)
        binding.btnReqOverlay.isEnabled = !hasOverlay

        binding.btnReqA11y.text = if (hasA11y) getString(R.string.status_enabled) else getString(R.string.btn_enable)
        binding.btnReqA11y.isEnabled = !hasA11y

        binding.btnReqAdmin.text = if (hasAdmin) getString(R.string.status_enabled) else getString(R.string.btn_enable)
        binding.btnReqAdmin.isEnabled = !hasAdmin

        val isRunning = isServiceRunning(FloatingService::class.java)
        if (isRunning) {
            binding.tvServiceStatus.text = getString(R.string.service_running)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            binding.btnToggleService.text = getString(R.string.stop_service)
        } else {
            binding.tvServiceStatus.text = getString(R.string.service_stopped)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.btnToggleService.text = getString(R.string.start_service)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) { e.printStackTrace() }
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                return settingValue.contains(packageName + "/" + MyAccessibilityService::class.java.canonicalName)
            }
        }
        return false
    }

    private fun isDeviceAdminEnabled(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, AdminReceiver::class.java)
        return dpm.isAdminActive(adminName)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
