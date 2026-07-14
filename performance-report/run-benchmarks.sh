#!/bin/bash
# Run all benchmarks and capture results
set -e
REPORT_DIR="performance-report"
mkdir -p $REPORT_DIR
echo "=== Building ===" && ./gradlew :app:assembleDebug :macros-benchmark:assemble
echo "=== Installing ===" && adb install -r app/build/outputs/apk/debug/app-debug.apk
echo "=== Waiting for idle ===" && sleep 5
PID=$(adb shell ps -A | grep aistudio | awk '{print $2}')
echo "=== GPU Rendering Profile ===" && adb shell dumpsys gfxinfo $PID > $REPORT_DIR/gfxinfo-after.txt
echo "=== Frame Stats ===" && adb shell dumpsys gfxinfo $PID framestats > $REPORT_DIR/framestats-after.txt
echo "=== Memory ===" && adb shell dumpsys meminfo $PID > $REPORT_DIR/meminfo-after.txt
echo "=== Done ==="
