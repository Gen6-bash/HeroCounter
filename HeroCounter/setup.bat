@echo off
echo Downloading Gradle wrapper...
powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo Success! gradle-wrapper.jar downloaded.
    echo Now open this folder in Android Studio.
) else (
    echo Download failed. Trying alternative...
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/gradle/gradle-distributions/raw/main/gradle-8.4-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)
pause
