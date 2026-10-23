#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_BIN="$HOME/.gradle/jdks/eclipse_adoptium-22-amd64-linux.2/bin/java"

if [ ! -f "$JAVA_BIN" ]; then
    JAVA_BIN="java"
fi

KOTLIN_STDLIB="/home/baobaojingyi/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.1.20/aa8ca79cd50578314f6d1180c47cbe14c0fee567/kotlin-stdlib-2.1.20.jar"
ANNOTATIONS="/home/baobaojingyi/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar"

# Prioritaskan class hasil compile IntelliJ (out/production/classes)
if [ -f "$SCRIPT_DIR/out/production/classes/MainKt.class" ]; then
    CP="$SCRIPT_DIR/out/production/classes:$KOTLIN_STDLIB:$ANNOTATIONS"
    exec "$JAVA_BIN" -cp "$CP" MainKt "$@"
fi

# Fallback ke build classes gradle
if [ -f "$SCRIPT_DIR/build/classes/kotlin/main/MainKt.class" ]; then
    CP="$SCRIPT_DIR/build/classes/kotlin/main:$KOTLIN_STDLIB:$ANNOTATIONS"
    exec "$JAVA_BIN" -cp "$CP" MainKt "$@"
fi

# Fallback ke Gradle dist jika ada
if [ -f "$SCRIPT_DIR/build/install/PhysLang/bin/PhysLang" ]; then
    export JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-22-amd64-linux.2"
    exec "$SCRIPT_DIR/build/install/PhysLang/bin/PhysLang" "$@"
fi

echo "Belum ada hasil compile. Silakan tekan Ctrl+F9 di IntelliJ sekali!"
exit 1
