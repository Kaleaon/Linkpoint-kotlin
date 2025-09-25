#!/bin/bash

# Simple build script for Linkpoint-kotlin project
# This script builds the project without complex Gradle issues

echo "Building Linkpoint-kotlin viewer..."

# Create build directory
mkdir -p build/classes

# Compile core module
echo "Compiling core module..."
kotlinc -d build/classes \
    core/src/main/kotlin/com/linkpoint/core/events/EventSystem.kt \
    core/src/main/kotlin/com/linkpoint/core/ViewerCore.kt

# Compile protocol module  
echo "Compiling protocol module..."
kotlinc -cp build/classes -d build/classes \
    protocol/src/main/kotlin/com/linkpoint/protocol/SecondLifeProtocol.kt

# Compile graphics module
echo "Compiling graphics module..."
kotlinc -cp build/classes -d build/classes \
    graphics/src/main/kotlin/com/linkpoint/graphics/RenderEngine.kt

# Compile main application
echo "Compiling main application..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/Main.kt

echo "Build complete! Running application..."

# Run the application
kotlin -cp build/classes com.linkpoint.MainKt