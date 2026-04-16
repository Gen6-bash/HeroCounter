#!/bin/bash
echo "Downloading Gradle wrapper..."
curl -L "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
     -o "gradle/wrapper/gradle-wrapper.jar"
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Success! Now open this folder in Android Studio."
else
    echo "Direct download failed, trying via gradle..."
    gradle wrapper --gradle-version 8.4
fi
