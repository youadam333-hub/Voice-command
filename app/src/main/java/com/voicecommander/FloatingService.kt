package com.voicecommander

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Locale

class FloatingService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val commandParser = CommandParser()
    private var tts: TextToSpeech? = null
    private var isFlashlightOn = false
    private var recognitionLang = "ar-SA"

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        val prefs = getSharedPreferences("VoiceCommanderPrefs", Context.MODE_PRIVATE)
        val langOption = prefs.getInt("language_pref", 0) // 0=Ar, 1=En, 2=Both
        recognitionLang = when (langOption) {
            1 -> "en-US"
            else -> "ar-SA"
        }

        startForegroundServiceNotification()
        setupFloatingView()
        setupSpeechRecognizer()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = if (recognitionLang == "en-US") Locale.ENGLISH else Locale("ar")
            if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE ||
                tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                tts?.language = locale
            } else {
                tts?.language = Locale.ENGLISH
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "VoiceCommanderChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setSmallIcon(R.drawable.ic_mic)
            .build()
        startForeground(1, notification)
    }

    private fun setupFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null)
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0; params.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        val micIcon = floatingView.findViewById<ImageView>(R.id.mic_icon)
        
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        var isClick = false

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isClick = true; true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.rawX - initialTouchX) > 10 || Math.abs(event.rawY - initialTouchY) > 10) {
                        isClick = false
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        if (isListening) stopListening() else startListening()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    floatingView.findViewById<ImageView>(R.id.mic_icon).setColorFilter(android.graphics.Color.WHITE)
                }
                override fun onError(error: Int) {
                    isListening = false
                    floatingView.findViewById<ImageView>(R.id.mic_icon).setColorFilter(android.graphics.Color.WHITE)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    floatingView.findViewById<ImageView>(R.id.mic_icon).setColorFilter(android.graphics.Color.WHITE)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) executeCommand(matches[0])
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            
            // Allow multiple language fallback if Both is selected
            val prefs = getSharedPreferences("VoiceCommanderPrefs", Context.MODE_PRIVATE)
            if (prefs.getInt("language_pref", 0) == 2) {
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("ar-SA", "en-US"))
            }
        }
        floatingView.findViewById<ImageView>(R.id.mic_icon).setColorFilter(android.graphics.Color.RED)
        speechRecognizer?.startListening(intent)
        isListening = true
    }

    private fun stopListening() {
        floatingView.findViewById<ImageView>(R.id.mic_icon).setColorFilter(android.graphics.Color.WHITE)
        speechRecognizer?.stopListening()
        isListening = false
    }

    private fun executeCommand(text: String) {
        val parsed = commandParser.parse(text)
        when (parsed.action) {
            CommandParser.ActionType.OPEN_APP -> openApp(parsed.target)
            CommandParser.ActionType.TURN_ON_WIFI -> openSettings(Settings.ACTION_WIFI_SETTINGS, getString(R.string.cmd_wifi))
            CommandParser.ActionType.TURN_ON_BLUETOOTH -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, getString(R.string.cmd_bluetooth))
            CommandParser.ActionType.TURN_ON_FLASHLIGHT -> toggleFlashlight(true)
            CommandParser.ActionType.TURN_OFF_FLASHLIGHT -> toggleFlashlight(false)
            CommandParser.ActionType.LOCK_SCREEN -> lockScreen()
            CommandParser.ActionType.TURN_OFF_SCREEN -> turnOffScreen()
            CommandParser.ActionType.CLOSE_APP -> closeCurrentApp()
            CommandParser.ActionType.POWER_OFF -> showPowerMenu()
            CommandParser.ActionType.WAKE_UP -> wakeUpScreen()
            CommandParser.ActionType.HELP -> speak(getString(R.string.cmd_help_spoken))
            CommandParser.ActionType.UNKNOWN -> speak(getString(R.string.cmd_unknown) + " " + text)
        }
    }

    private fun openApp(appName: String?) {
        if (appName == null) return
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        var foundPackage: String? = null
        
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
        
        for (resolveInfo in resolveInfoList) {
            val name = resolveInfo.loadLabel(pm).toString().lowercase()
            if (name.contains(appName.lowercase())) {
                foundPackage = resolveInfo.activityInfo.packageName
                break
            }
        }
        
        if (foundPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(foundPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                speak(getString(R.string.cmd_opening) + " " + appName)
            } else {
                speak(getString(R.string.cmd_not_opened))
            }
        } else {
            speak(getString(R.string.cmd_app_not_found) + " " + appName)
        }
    }

    private fun openSettings(action: String, msg: String) {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        speak(msg)
    }

    private fun toggleFlashlight(status: Boolean) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList[0]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.setTorchMode(cameraId, status)
                isFlashlightOn = status
                speak(if(status) getString(R.string.cmd_flashlight_on) else getString(R.string.cmd_flashlight_off))
            }
        } catch (e: Exception) { speak(getString(R.string.cmd_flashlight_err)) }
    }

    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))) {
            speak(getString(R.string.cmd_locking)); dpm.lockNow()
        } else speak(getString(R.string.cmd_admin_req))
    }

    private fun turnOffScreen() {
        speak(getString(R.string.cmd_screen_off))
        MyAccessibilityService.instance?.turnOffScreen()
    }

    private fun wakeUpScreen() {
        speak(getString(R.string.cmd_wake_up))
        MyAccessibilityService.instance?.wakeUpScreen()
    }

    private fun closeCurrentApp() {
        speak(getString(R.string.cmd_closing_app))
        MyAccessibilityService.instance?.performGlobalHome()
    }

    private fun showPowerMenu() {
        speak(getString(R.string.cmd_power_menu))
        MyAccessibilityService.instance?.performGlobalPowerDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy(); tts?.stop(); tts?.shutdown()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
