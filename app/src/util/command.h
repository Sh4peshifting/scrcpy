#ifndef SC_COMMAND_H
#define SC_COMMAND_H

#include "common.h"

/**
 * Serialize an argv array for Windows
 *
 * Convert a NULL-terminated argument array into a single escaped string
 * suitable for massing to CreateProcess() on Windows.
 *
 * The returned value must be freed by the caller.
 */
char *
sc_command_serialize_windows(const char *const argv[]);

/**
 * Serialize a server command for adb shell, requesting root through su if
 * adbd is not already running as root. Both shell parsing layers are quoted.
 *
 * The first argument must be an environment assignment (CLASSPATH=...).
 * The returned value must be freed by the caller.
 */
char *
sc_command_serialize_root(const char *const argv[]);

#endif
