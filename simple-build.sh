#!/bin/bash

# Simple build script for Linkpoint-kotlin viewer
# Builds and runs the viewer with core functionality

echo "Building Linkpoint-kotlin viewer..."

# Create build directory
mkdir -p build/classes

# Compile core module (SimpleViewerCore that works without dependencies)
echo "Compiling core module..."
if ! kotlinc -d build/classes core/src/main/kotlin/com/linkpoint/core/SimpleViewerCore.kt 2>/dev/null; then
    echo "⚠️ Core module compilation failed"
fi

# Compile protocol module  
echo "Compiling protocol module..."
if ! kotlinc -cp build/classes -d build/classes \
    protocol/src/main/kotlin/com/linkpoint/protocol/data/SimpleWorldEntities.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/LoginSystem.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/UDPMessageSystem.kt \
    protocol/src/main/kotlin/com/linkpoint/protocol/RLVProcessor.kt 2>/dev/null; then
    echo "⚠️ Protocol module compilation failed (likely due to missing dependencies)"
fi

# Compile main application
echo "Compiling main application..."
if ! kotlinc -cp build/classes -d build/classes src/main/kotlin/com/linkpoint/SimpleMain.kt; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build complete!"

echo "Running application..."
if ! kotlin -cp build/classes com.linkpoint.SimpleMainKt; then
    echo "❌ Application failed to start"
    exit 1
fi