#!/bin/bash

# Graphics Pipeline Demo Build Script
# Builds and runs the Phase 3 Graphics Implementation demonstration

echo "Building Linkpoint-kotlin Graphics Pipeline Demo..."
echo "Phase 3: 3D Rendering System Implementation"
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

# Compile protocol module data structures
echo "📡 Compiling protocol data structures..."
kotlinc -cp build/classes -d build/classes \
    protocol/src/main/kotlin/com/linkpoint/protocol/data/WorldEntities.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile protocol data structures"
    exit 1
fi

# Compile graphics modules
echo "🎨 Compiling graphics pipeline..."
kotlinc -cp build/classes -d build/classes \
    graphics/src/main/kotlin/com/linkpoint/graphics/rendering/OpenGLRenderer.kt \
    graphics/src/main/kotlin/com/linkpoint/graphics/cameras/ViewerCamera.kt \
    graphics/src/main/kotlin/com/linkpoint/graphics/shaders/ShaderManager.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile graphics modules"
    exit 1
fi

# Compile graphics demo application
echo "🚀 Compiling graphics demo..."
kotlinc -cp build/classes -d build/classes \
    src/main/kotlin/com/linkpoint/GraphicsDemo.kt

if [ $? -ne 0 ]; then
    echo "❌ Failed to compile graphics demo"
    exit 1
fi

echo "✅ Build complete! Starting Graphics Pipeline demonstration..."
echo

# Run the graphics demo
kotlin -cp build/classes com.linkpoint.GraphicsDemoKt