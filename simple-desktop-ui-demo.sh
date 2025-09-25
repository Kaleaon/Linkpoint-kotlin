#!/bin/bash

# Simple Desktop UI Demo Script  
# Demonstrates desktop windowed interface with traditional virtual world viewer patterns
# No external dependencies required

echo "Linkpoint Kotlin - Simple Desktop UI Demo"
echo "=========================================="
echo "Compiling desktop UI demonstration..."

# Compile the simple desktop UI demo
kotlinc -d simple-desktop-ui-demo.jar \
        src/main/kotlin/com/linkpoint/SimpleDesktopUIDemo.kt

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo ""
    echo "Running Simple Desktop UI Demo..."
    echo "=================================="
    
    # Run the simple desktop UI demo
    kotlin -cp "simple-desktop-ui-demo.jar:." com.linkpoint.SimpleDesktopUIDemoKt
else
    echo "❌ Compilation failed!"
    exit 1
fi