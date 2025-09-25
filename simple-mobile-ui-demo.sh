#!/bin/bash

# Simple Mobile UI Demo Script
# Demonstrates mobile-optimized interface concepts inspired by Lumiya Viewer
# No external dependencies required

echo "Linkpoint Kotlin - Simple Mobile UI Demo"
echo "========================================"
echo "Compiling mobile UI demonstration..."

# Compile the simple mobile UI demo
kotlinc -d simple-mobile-ui-demo.jar \
        src/main/kotlin/com/linkpoint/SimpleMobileUIDemo.kt

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Running Simple Mobile UI Demo..."
    echo "================================="
    
    # Run the simple mobile UI demo
    kotlin -cp "simple-mobile-ui-demo.jar:." com.linkpoint.SimpleMobileUIDemoKt
else
    echo "❌ Compilation failed!"
    exit 1
fi