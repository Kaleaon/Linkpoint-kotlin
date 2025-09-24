#!/bin/bash

# Protocol Demo Build Script
# Builds and runs the Next Development Phase demonstration

echo "Building Linkpoint-kotlin Protocol Demo..."
echo "Showcasing documented, readable code from the next development phase"
echo

# Create build directory
mkdir -p build/classes

# Compile core module
echo "📦 Compiling core module..."
kotlinc -d build/classes \
    core/src/main/kotlin/com/linkpoint/core/events/EventSystem.kt \
    core/src/main/kotlin/com/linkpoint/core/SimpleViewerCore.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile core module"
    exit 1
fi

# Compile protocol module
echo "📡 Compiling protocol module..."
kotlinc -cp build/classes -d build/classes \
    protocol/src/main/kotlin/com/linkpoint/protocol/data/WorldEntities.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/LoginSystem.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/UDPMessageSystem.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/RLVProcessor.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile protocol module"
    exit 1
fi

# Compile protocol demo application
echo "🚀 Compiling protocol demo..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/ProtocolDemo.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile protocol demo"
    exit 1
fi

echo "✅ Build complete! Starting protocol demonstration..."
echo

# Run the protocol demo
kotlin -cp build/classes com.linkpoint.ProtocolDemoKt