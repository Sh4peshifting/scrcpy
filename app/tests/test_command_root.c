#include "util/command.h"

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[]) {
    if (argc > 1) {
        // Used by the shell round-trip test with real adb/su parsing layers.
        char *command = sc_command_serialize_root((const char *const *) argv + 1);
        assert(command);
        puts(command);
        free(command);
        return 0;
    }

    const char *args[] = {"CLASSPATH=/tmp/server.jar", "app_process", "", "a'b", NULL};
    char *command = sc_command_serialize_root(args);
    assert(command);
    assert(strstr(command, "exec env 'CLASSPATH=/tmp/server.jar' 'app_process' '' 'a'\\''b'"));
    assert(strstr(command, "else exec su -c 'exec env "));
    free(command);
    return 0;
}
