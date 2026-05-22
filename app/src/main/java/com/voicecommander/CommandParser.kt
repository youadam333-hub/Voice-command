package com.voicecommander

class CommandParser {
    enum class ActionType {
        OPEN_APP, TURN_ON_WIFI, TURN_ON_BLUETOOTH, TURN_ON_FLASHLIGHT, TURN_OFF_FLASHLIGHT,
        LOCK_SCREEN, TURN_OFF_SCREEN, CLOSE_APP, POWER_OFF, WAKE_UP, HELP, UNKNOWN
    }
    data class ParsedCommand(val action: ActionType, val target: String? = null)

    fun parse(text: String): ParsedCommand {
        val lowerText = text.lowercase().trim()
        
        if (lowerText.startsWith("افتح ") || lowerText.startsWith("شغل ") || lowerText.startsWith("شغّل ") || 
            lowerText.startsWith("open ") || lowerText.startsWith("start ")) {
            val appName = lowerText.replaceFirst("افتح ", "").replaceFirst("شغل ", "").replaceFirst("شغّل ", "")
                                 .replaceFirst("open ", "").replaceFirst("start ", "").trim()
            return ParsedCommand(ActionType.OPEN_APP, appName)
        }

        if (lowerText.contains("wi-fi") || lowerText.contains("واي فاي") || lowerText.contains("wifi")) return ParsedCommand(ActionType.TURN_ON_WIFI)
        if (lowerText.contains("bluetooth") || lowerText.contains("بلوتوث")) return ParsedCommand(ActionType.TURN_ON_BLUETOOTH)
        if (lowerText.contains("turn on flashlight") || lowerText.contains("شغل الكشاف") || lowerText.contains("شغّل الكشاف") || lowerText.contains("شغل الفلاش")) return ParsedCommand(ActionType.TURN_ON_FLASHLIGHT)
        if (lowerText.contains("turn off flashlight") || lowerText.contains("طفي الكشاف") || lowerText.contains("اطفي الكشاف") || lowerText.contains("طفي الفلاش")) return ParsedCommand(ActionType.TURN_OFF_FLASHLIGHT)
        if (lowerText.contains("lock screen") || lowerText.contains("قفل الشاشة") || lowerText.contains("قفل الجوال")) return ParsedCommand(ActionType.LOCK_SCREEN)
        if (lowerText.contains("turn off screen") || lowerText.contains("اطفي الشاشة") || lowerText.contains("إيقاف الشاشة") || lowerText.contains("طفي الشاشة")) return ParsedCommand(ActionType.TURN_OFF_SCREEN)
        if (lowerText.contains("wake up") || lowerText.contains("صحّي الشاشة") || lowerText.contains("شغل الشاشة") || lowerText.contains("اصحي")) return ParsedCommand(ActionType.WAKE_UP)
        if (lowerText.contains("close app") || lowerText.contains("أغلق التطبيق") || lowerText.contains("اغلق التطبيق") || lowerText.contains("سكر التطبيق")) return ParsedCommand(ActionType.CLOSE_APP)
        if (lowerText.contains("power off") || lowerText.contains("أغلق الهاتف") || lowerText.contains("إيقاف التشغيل") || lowerText.contains("طفي الجهاز")) return ParsedCommand(ActionType.POWER_OFF)
        if (lowerText.contains("help") || lowerText.contains("مساعدة")) return ParsedCommand(ActionType.HELP)

        return ParsedCommand(ActionType.UNKNOWN)
    }
}
