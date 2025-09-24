#!/bin/bash

# Simple Graphics Pipeline Demo Build Script
# Builds and runs the Phase 3 Graphics Implementation demonstration without dependencies

echo "Building Linkpoint-kotlin Simple Graphics Pipeline Demo..."
echo "Phase 3: 3D Rendering System Architecture Implementation"
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

# Compile protocol module simple data structures
echo "📡 Compiling protocol data structures..."
kotlinc -cp build/classes -d build/classes \
    protocol/src/main/kotlin/com/linkpoint/protocol/data/SimpleWorldEntities.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile protocol data structures"
    exit 1
fi

# Compile simple graphics demo application
echo "🚀 Compiling simple graphics demo..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/SimpleGraphicsDemo.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile simple graphics demo"
    exit 1
fi

echo "✅ Build complete! Starting Graphics Pipeline Architecture demonstration..."
echo

# Run the simple graphics demo
kotlin -cp build/classes com.linkpoint.SimpleGraphicsDemoKt