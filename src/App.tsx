import { BookOpen } from "lucide-react";

export default function App() {
  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col items-center justify-center p-8 font-sans">
      <div className="max-w-xl w-full bg-gray-900 border border-gray-800 rounded-2xl p-8 shadow-2xl text-center">
        <h1 className="text-3xl font-bold mb-4 tracking-tight">Android Project Ready</h1>
        <p className="text-gray-400 mb-6 font-medium">
          The "Voice Commander" Kotlin Android project has been successfully generated in your workspace.
        </p>
        <div className="bg-gray-800 rounded-xl p-4 text-left font-mono text-sm text-gray-300 mb-8 border border-gray-700">
          <ul className="space-y-2">
            <li>✓ MainActivity.kt</li>
            <li>✓ FloatingService.kt</li>
            <li>✓ CommandParser.kt</li>
            <li>✓ MyAccessibilityService.kt</li>
            <li>✓ AdminReceiver.kt & BootReceiver.kt</li>
            <li>✓ AndroidManifest.xml & Layouts</li>
            <li>✓ build.gradle.kts</li>
          </ul>
        </div>
        <p className="text-gray-400 text-sm flex items-center justify-center gap-2">
          <BookOpen className="w-4 h-4" />
          <span>
            To download this project, click the <b>Project Settings</b> menu (top right) and select <b>Export to ZIP</b> or <b>Export to GitHub</b>, then open it in Android Studio.
          </span>
        </p>
      </div>
    </div>
  );
}
