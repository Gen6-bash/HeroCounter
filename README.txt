==========================================================
  HERO COUNTER - Setup Instructions
==========================================================

BEFORE opening in Android Studio, you need ONE file:
  gradle/wrapper/gradle-wrapper.jar

HOW TO GET IT (pick one):

OPTION A - Run the setup script (easiest):
  Windows: Double-click setup.bat
  Mac/Linux: Run ./setup.sh in Terminal

OPTION B - Copy from Android Studio (always works):
  Windows:
    Copy from: C:\Program Files\Android\Android Studio\plugins\gradle\lib\gradle-wrapper.jar
    Paste to:  gradle\wrapper\gradle-wrapper.jar

  Mac:
    Copy from: /Applications/Android Studio.app/Contents/plugins/gradle/lib/gradle-wrapper.jar
    Paste to:  gradle/wrapper/gradle-wrapper.jar

OPTION C - Download manually:
  Go to: https://services.gradle.org/distributions/
  Download: gradle-8.4-bin.zip
  Run in this folder: gradlew wrapper

AFTER getting the jar:
  1. Open Android Studio
  2. File -> Open -> select THIS folder (HeroCounter)
  3. Click Trust
  4. Wait for Gradle sync to complete
  5. Build -> Build Bundle(s)/APK(s) -> Build APK(s)

==========================================================
