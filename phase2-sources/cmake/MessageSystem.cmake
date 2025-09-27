# MessageSystem.cmake
# CMake configuration for SecondLife Message System
# Original from: https://github.com/secondlife/viewer/blob/main/indra/llmessage/CMakeLists.txt
# Translated to support both C++ reference and Kotlin implementation

cmake_minimum_required(VERSION 3.16)

# Project configuration
project(MessageSystem VERSION 1.0.0 LANGUAGES CXX)

# C++ Standard requirements
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Platform detection
if(WIN32)
    set(PLATFORM_WINDOWS TRUE)
elseif(APPLE)
    set(PLATFORM_DARWIN TRUE)
elseif(UNIX)
    set(PLATFORM_LINUX TRUE)
endif()

# Build configuration
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "Build type" FORCE)
endif()

# Source file groups
set(MESSAGESYSTEM_HEADER_FILES
    llassetstorage.h
    llavatarname.h
    llavatarnamecache.h
    llblowfishcipher.h
    llbuffer.h
    llbufferstream.h
    llcachename.h
    llchainio.h
    llcipher.h
    llcircuit.h
    llclassifiedflags.h
    llcoproceduremanager.h
    llcorehttputil.h
    lldatapacker.h
    lldispatcher.h
    lleventflags.h
    llexperiencecache.h
    llextendedstatus.h
    llfollowcamparams.h
    llgenericstreamingmessage.h
    llhost.h
    llhttpnode.h
    llhttpnodeadapter.h
    llhttpsdhandler.h
    llinstantmessage.h
    llinvite.h
    lliobuffer.h
    lliohttpserver.h
    lliopipe.h
    lliosocket.h
    llioutil.h
    llloginflags.h
    llmail.h
    llmessage.h
    llmessagebuilder.h
    llmessageconfig.h
    llmessagereader.h
    llmessagesenderinterface.h
    llmessagetemplate.h
    llmessagetemplateparser.h
    llmessagethrottle.h
    llmsgvariabletype.h
    llnamevalue.h
    llnullcipher.h
    llpacketack.h
    llpacketbuffer.h
    llpacketring.h
    llpartdata.h
    llproxy.h
    llpumpio.h
    llqueryflags.h
    llregionflags.h
    llregionhandle.h
    llsdappservices.h
    llsdhttpserver.h
    llsdmessagebuilder.h
    llsdmessagereader.h
    llservice.h
    llservicebuilder.h
    llstoredmessage.h
    lltaskname.h
    llteleportflags.h
    lltemplatemessagebuilder.h
    lltemplatemessagedispatcher.h
    lltemplatemessagereader.h
    llthrottle.h
    lltransfermanager.h
    lltransfersourceasset.h
    lltransfersourcefile.h
    lltransfertargetfile.h
    lltransfertargetvfile.h
    lltrustedmessageservice.h
    lluseroperation.h
    llvehicleparams.h
    llxfer.h
    llxfer_file.h
    llxfer_mem.h
    llxfer_vfile.h
    llxfermanager.h
    llxorcipher.h
    machine.h
    mean_collision_data.h
    message.h
    message_prehash.h
    net.h
    partsyspacket.h
    patch_code.h
    patch_dct.h
    sound_ids.h
)

set(MESSAGESYSTEM_SOURCE_FILES
    llassetstorage.cpp
    llavatarname.cpp
    llavatarnamecache.cpp
    llblowfishcipher.cpp
    llbuffer.cpp
    llbufferstream.cpp
    llcachename.cpp
    llchainio.cpp
    llcircuit.cpp
    llclassifiedflags.cpp
    llcoproceduremanager.cpp
    llcorehttputil.cpp
    lldatapacker.cpp
    lldispatcher.cpp
    llexperiencecache.cpp
    llgenericstreamingmessage.cpp
    llhost.cpp
    llhttpnode.cpp
    llhttpsdhandler.cpp
    llinstantmessage.cpp
    lliobuffer.cpp
    lliohttpserver.cpp
    lliopipe.cpp
    lliosocket.cpp
    llioutil.cpp
    llmail.cpp
    llmessagebuilder.cpp
    llmessageconfig.cpp
    llmessagereader.cpp
    llmessagetemplate.cpp
    llmessagetemplateparser.cpp
    llmessagethrottle.cpp
    llnamevalue.cpp
    llnullcipher.cpp
    llpacketack.cpp
    llpacketbuffer.cpp
    llpacketring.cpp
    llpartdata.cpp
    llproxy.cpp
    llpumpio.cpp
    llsdappservices.cpp
    llsdhttpserver.cpp
    llsdmessagebuilder.cpp
    llsdmessagereader.cpp
    llservice.cpp
    llservicebuilder.cpp
    llstoredmessage.cpp
    lltemplatemessagebuilder.cpp
    lltemplatemessagedispatcher.cpp
    lltemplatemessagereader.cpp
    llthrottle.cpp
    lltransfermanager.cpp
    lltransfersourceasset.cpp
    lltransfersourcefile.cpp
    lltransfertargetfile.cpp
    lltransfertargetvfile.cpp
    lltrustedmessageservice.cpp
    lluseroperation.cpp
    llxfer.cpp
    llxfer_file.cpp
    llxfer_mem.cpp
    llxfer_vfile.cpp
    llxfermanager.cpp
    llxorcipher.cpp
    machine.cpp
    message.cpp
    message_prehash.cpp
    message_string_table.cpp
    net.cpp
    partsyspacket.cpp
    patch_code.cpp
    patch_dct.cpp
    patch_idct.cpp
    sound_ids.cpp
)

# Create library
add_library(messagesystem STATIC
    ${MESSAGESYSTEM_HEADER_FILES}
    ${MESSAGESYSTEM_SOURCE_FILES}
)

# Include directories
target_include_directories(messagesystem
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/../llcommon
        ${CMAKE_CURRENT_SOURCE_DIR}/../llmath
        ${CMAKE_CURRENT_SOURCE_DIR}/../llvfs
        ${CMAKE_CURRENT_SOURCE_DIR}/../llxml
)

# Platform-specific configurations
if(PLATFORM_WINDOWS)
    target_compile_definitions(messagesystem PRIVATE
        LL_WINDOWS=1
        UNICODE
        _UNICODE
        WINVER=0x0601
        _WIN32_WINNT=0x0601
    )
    
    # Windows-specific libraries
    target_link_libraries(messagesystem
        PRIVATE
            ws2_32
            winmm
            advapi32
            shell32
            ole32
    )
elseif(PLATFORM_DARWIN)
    target_compile_definitions(messagesystem PRIVATE
        LL_DARWIN=1
    )
    
    # macOS-specific frameworks
    find_library(CARBON_FRAMEWORK Carbon)
    find_library(COREFOUNDATION_FRAMEWORK CoreFoundation)
    target_link_libraries(messagesystem
        PRIVATE
            ${CARBON_FRAMEWORK}
            ${COREFOUNDATION_FRAMEWORK}
    )
elseif(PLATFORM_LINUX)
    target_compile_definitions(messagesystem PRIVATE
        LL_LINUX=1
    )
endif()

# Compiler-specific flags
if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    target_compile_options(messagesystem PRIVATE
        -Wall
        -Wextra
        -Wno-unused-parameter
        -Wno-sign-compare
        -Wno-unused-variable
    )
endif()

# Dependencies
find_package(Threads REQUIRED)
target_link_libraries(messagesystem 
    PRIVATE 
        Threads::Threads
)

# OpenSSL for encryption
find_package(OpenSSL REQUIRED)
target_link_libraries(messagesystem 
    PRIVATE 
        OpenSSL::SSL
        OpenSSL::Crypto
)

# Boost libraries
find_package(Boost REQUIRED COMPONENTS 
    system 
    filesystem 
    thread 
    regex
)
target_link_libraries(messagesystem 
    PRIVATE 
        Boost::system
        Boost::filesystem
        Boost::thread
        Boost::regex
)

# cURL for HTTP
find_package(CURL REQUIRED)
target_link_libraries(messagesystem 
    PRIVATE 
        CURL::libcurl
)

# Installation
install(TARGETS messagesystem
    ARCHIVE DESTINATION lib
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)

install(FILES ${MESSAGESYSTEM_HEADER_FILES}
    DESTINATION include/messagesystem
)