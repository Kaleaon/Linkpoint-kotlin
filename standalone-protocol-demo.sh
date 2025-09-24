#!/bin/bash

# Standalone Protocol Demo Build Script
# Builds and runs the Next Development Phase demonstration without external dependencies

echo "Building Linkpoint-kotlin Standalone Protocol Demo..."
echo "Next Development Phase: Documented, Readable Code Implementation"
echo

# Create build directory
mkdir -p build/classes

# Compile core module (simplified version only)
echo "📦 Compiling core module..."
kotlinc -d build/classes \
    core/src/main/kotlin/com/linkpoint/core/SimpleViewerCore.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile core module"
    exit 1
fi

# Compile standalone protocol demo
echo "🚀 Compiling standalone protocol demo..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/StandaloneProtocolDemo.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile standalone protocol demo"
    exit 1
fi

echo "✅ Build complete! Starting Next Development Phase demonstration..."
echo

# Run the standalone protocol demo
kotlin -cp build/classes com.linkpoint.StandaloneProtocolDemoKt