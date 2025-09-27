/**
 * @file llviewermessage.h
 * @brief Header for UDP message handling for simulator communication
 * 
 * Original SecondLife Viewer component
 * Source: https://github.com/secondlife/viewer
 * This is a reference implementation for translation to Kotlin MessageProcessor
 */

#ifndef LLVIEWERMESSAGE_H
#define LLVIEWERMESSAGE_H

#include <cstdint>
#include <cstddef>

class LLViewerMessage;

/**
 * Initialize the global message system
 */
bool initViewerMessage();

/**
 * Get global message system instance
 */
LLViewerMessage* getViewerMessage();

/**
 * Cleanup global message system
 */
void shutdownViewerMessage();

#endif // LLVIEWERMESSAGE_H