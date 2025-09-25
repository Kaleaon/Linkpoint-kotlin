#!/bin/bash

# Desktop UI Demo Script
# Demonstrates the desktop windowed interface with traditional virtual world viewer patterns

echo "Linkpoint Kotlin - Desktop UI Demo"
echo "==================================="
echo "Compiling desktop UI demonstration..."

# Compile the desktop UI demo
kotlinc -cp ".:ui/src/main/kotlin:core/src/main/kotlin" \
        -d desktop-ui-demo.jar \
        src/main/kotlin/com/linkpoint/DesktopUIDemo.kt \
        ui/src/main/kotlin/com/linkpoint/ui/*.kt \
        core/src/main/kotlin/com/linkpoint/core/events/EventSystem.kt

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Running Desktop UI Demo..."
    echo "=========================="
    
    # Run the desktop UI demo
    kotlin -cp "desktop-ui-demo.jar:." com.linkpoint.DesktopUIDemoKt
else
    echo "❌ Compilation failed!"
    exit 1
fi