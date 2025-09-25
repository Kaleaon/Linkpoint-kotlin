#!/bin/bash

# Mobile UI Demo Script
# Demonstrates the mobile-optimized interface inspired by Lumiya Viewer

echo "Linkpoint Kotlin - Mobile UI Demo"
echo "=================================="
echo "Compiling mobile UI demonstration..."

# Compile the mobile UI demo
kotlinc -cp ".:ui/src/main/kotlin:core/src/main/kotlin" \
        -d mobile-ui-demo.jar \
        src/main/kotlin/com/linkpoint/MobileUIDemo.kt \
        ui/src/main/kotlin/com/linkpoint/ui/*.kt \
        core/src/main/kotlin/com/linkpoint/core/events/EventSystem.kt

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Running Mobile UI Demo..."
    echo "========================="
    
    # Run the mobile UI demo
    kotlin -cp "mobile-ui-demo.jar:." com.linkpoint.MobileUIDemoKt
else
    echo "❌ Compilation failed!"
    exit 1
fi