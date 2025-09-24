# Linkpoint-kotlin

A modern Kotlin-based virtual world viewer that imports and modernizes concepts from established Second Life viewers.

## Overview

Linkpoint-kotlin aims to create a contemporary virtual world viewer by importing and modernizing functionality from:

- **SecondLife Viewer** - Official Linden Lab viewer with core virtual world functionality
- **Firestorm Viewer** - Popular third-party viewer with advanced features and optimizations  
- **Restrained Love Viewer (RLV)** - Specialized viewer with additional protocol extensions

## Architecture

The project is structured as a multi-module Kotlin application:

### Core Modules

- **`:core`** - Core viewer systems, event handling, and lifecycle management
- **`:protocol`** - Virtual world protocol implementation (SecondLife/OpenSim compatible)
- **`:graphics`** - 3D rendering engine and graphics pipeline
- **`:ui`** - User interface components and interaction handling
- **`:audio`** - Audio processing and spatial sound systems
- **`:assets`** - Asset management, caching, and streaming

### Key Components Being Imported

#### From SecondLife Viewer
- Core virtual world protocol (UDP messaging, XMLRPC login)
- Basic avatar and object rendering
- Chat and instant messaging systems
- Inventory management
- Physics and movement systems

#### From Firestorm Viewer  
- Advanced rendering optimizations
- Enhanced UI/UX features
- Extended search and discovery features
- Media streaming improvements
- Performance monitoring and debugging tools

#### From Restrained Love Viewer
- Extended protocol messages and capabilities
- Additional avatar control mechanisms
- Enhanced scripting interfaces
- Specialized interaction modes

## Development Status

- [x] Initial project structure created
- [x] Core module architecture defined
- [x] Event system framework implemented
- [x] Basic viewer lifecycle management
- [x] Protocol module skeleton created
- [x] Graphics engine foundation laid
- [ ] Implement XMLRPC login system
- [ ] Add UDP message handling for simulator communication
- [ ] Create basic 3D scene rendering
- [ ] Implement chat and IM systems
- [ ] Add avatar movement and physics
- [ ] Create asset management system
- [ ] Build user interface components
- [ ] Add audio processing capabilities

## Building and Running

The project uses Gradle for build management:

```bash
./gradlew build
./gradlew run
```

## Import Strategy

### Phase 1: Core Infrastructure
1. Establish basic viewer architecture
2. Implement core event system
3. Create modular component structure
4. Set up build and testing framework

### Phase 2: Protocol Implementation
1. Import and modernize login system from SL viewer
2. Implement UDP messaging for simulator communication
3. Add basic object and avatar data handling
4. Create foundation for extended protocols (RLV)

### Phase 3: Rendering System
1. Import 3D rendering concepts from SL/Firestorm
2. Modernize graphics pipeline with current OpenGL/Vulkan
3. Implement efficient scene management
4. Add support for mesh, textures, and animations

### Phase 4: User Interface
1. Create modern UI framework inspired by Firestorm
2. Implement chat, inventory, and world interaction
3. Add search and discovery features
4. Create preferences and settings management

### Phase 5: Advanced Features
1. Import RLV protocol extensions
2. Add media streaming capabilities
3. Implement advanced avatar controls
4. Create scripting and automation interfaces

## Contributing

This project aims to honor the work of the original viewer developers while creating a modern, maintainable codebase in Kotlin. All imported concepts will be properly attributed to their original implementations.

## License

TBD - Will respect licenses of imported concepts and components.