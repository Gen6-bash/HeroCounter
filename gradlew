#!/bin/sh
##############################################################################
# Gradle wrapper script - uses system gradle if wrapper jar is missing
##############################################################################
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Try wrapper jar first
if [ -f "$GRADLE_WRAPPER_JAR" ]; then
    exec java -jar "$GRADLE_WRAPPER_JAR" "$@"
fi

# Fall back to system gradle
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

echo "ERROR: gradle-wrapper.jar not found and no system gradle available."
echo "Please download gradle-wrapper.jar and place it in gradle/wrapper/"
exit 1
