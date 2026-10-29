#include "command.h"

#include <stdlib.h>

#include "util/strbuf.h"

static bool
sc_command_append_shell_arg(struct sc_strbuf *buf, const char *arg) {
    if (!sc_strbuf_append_char(buf, '\'')) {
        return false;
    }
    for (const char *p = arg; *p; ++p) {
        bool ok = *p == '\''
                ? sc_strbuf_append_staticstr(buf, "'\\''")
                : sc_strbuf_append_char(buf, *p);
        if (!ok) {
            return false;
        }
    }
    return sc_strbuf_append_char(buf, '\'');
}

char *
sc_command_serialize_root(const char *const argv[]) {
    struct sc_strbuf inner;
    if (!sc_strbuf_init(&inner, 1024)) {
        return NULL;
    }
    if (!sc_strbuf_append_staticstr(&inner, "exec env")) {
        free(inner.s);
        return NULL;
    }
    for (const char *const *arg = argv; *arg; ++arg) {
        if (!sc_strbuf_append_char(&inner, ' ')
                || !sc_command_append_shell_arg(&inner, *arg)) {
            free(inner.s);
            return NULL;
        }
    }

    struct sc_strbuf outer;
    if (!sc_strbuf_init(&outer, 2048)) {
        free(inner.s);
        return NULL;
    }
    bool ok = sc_strbuf_append_staticstr(&outer,
                    "if [ $(id -u) = 0 ]; then ")
           && sc_strbuf_append_str(&outer, inner.s)
           && sc_strbuf_append_staticstr(&outer, "; else exec su -c ")
           && sc_command_append_shell_arg(&outer, inner.s)
           && sc_strbuf_append_staticstr(&outer, "; fi");
    free(inner.s);
    if (!ok) {
        free(outer.s);
        return NULL;
    }
    return outer.s;
}

char *
sc_command_serialize_windows(const char *const argv[]) {
    // <https://learn.microsoft.com/en-us/cpp/c-language/parsing-c-command-line-arguments>
    // <https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessw>

    struct sc_strbuf buf;
    bool ok = sc_strbuf_init(&buf, 1024);
    if (!ok) {
        return NULL;
    }

#define BUF_PUSH(C) \
    do { \
        if (!sc_strbuf_append_char(&buf, C)) { \
            goto end; \
        } \
    } while (0)

    while (*argv) {
        const char *arg = *argv;

        BUF_PUSH('"');

        // <https://learn.microsoft.com/en-us/cpp/c-language/parsing-c-command-line-arguments>
        //
        // """
        // If an even number of backslashes is followed by a double quote mark,
        // then one backslash (\) is placed in the argv array for every pair of
        // backslashes (\\), and the double quote mark (") is interpreted as a
        // string delimiter.
        //
        // If an odd number of backslashes is followed by a double quote mark,
        // then one backslash (\) is placed in the argv array for every pair of
        // backslashes (\\). The double quote mark is interpreted as an escape
        // sequence by the remaining backslash, causing a literal double quote
        // mark (") to be placed in argv.
        // """
        //
        // To produce correct escaping according to what the parser will do, we
        // must count the number of successive backslashes.
        unsigned backslashes = 0;

        for (const char *c = arg; *c; c++) {
            switch (*c) {
                case '"':
                    while (backslashes) {
                        // Double all backslashes before a quote
                        BUF_PUSH('\\');
                        BUF_PUSH('\\');
                        --backslashes;
                    }
                    BUF_PUSH('\\');
                    BUF_PUSH('"');
                    backslashes = 0;
                    break;
                case '\\':
                    ++backslashes;
                    break;
                default:
                    while (backslashes) {
                        // Put all backslashes as literals
                        BUF_PUSH('\\');
                        --backslashes;
                    }
                    BUF_PUSH(*c);
                    break;
            }
        }

        while (backslashes) {
            // Double all backslashes before a quote
            BUF_PUSH('\\');
            BUF_PUSH('\\');
            --backslashes;
        }

        BUF_PUSH('"');

        ++argv;

        // Argument separator
        if (*argv) {
            BUF_PUSH(' ');
        }
    }

    return buf.s;

end:
    free(buf.s);
    return NULL;
}
