/**
 * @file llappviewer.h
 * @brief Header for main application class for Second Life viewer
 * 
 * Original SecondLife Viewer component
 * Source: https://github.com/secondlife/viewer
 * This is a reference implementation for translation to Kotlin ViewerCore
 */

#ifndef LLAPPVIEWER_H
#define LLAPPVIEWER_H

/**
 * Global initialization function
 */
bool initViewer();

/**
 * Global start function
 */
bool startViewer();

/**
 * Global run function
 */
void runViewer();

/**
 * Global shutdown function
 */
void shutdownViewer();

#endif // LLAPPVIEWER_H