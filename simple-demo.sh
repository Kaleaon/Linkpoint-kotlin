#!/bin/bash

# Simple demo script for Linkpoint-kotlin project
# This demonstrates the project without complex dependencies

echo "Building Linkpoint-kotlin viewer demo..."

# Create build directory
mkdir -p build/classes

# Compile the simplified core module
echo "Compiling simplified core..."
kotlinc -d build/classes \
    core/src/main/kotlin/com/linkpoint/core/SimpleViewerCore.kt

if [ $? -ne 0 ]; then
    echo "Failed to compile core module"
    exit 1
fi

# Compile main demo application
echo "Compiling demo application..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/SimpleMain.kt

if [ $? -ne 0 ]; then
    echo "Failed to compile demo application"
    exit 1
fi

echo "Build complete! Running demonstration..."
echo

# Run the demo
kotlin -cp build/classes com.linkpoint.SimpleMainKt