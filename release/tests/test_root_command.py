#!/usr/bin/env python3
"""Exercise both root launch paths through real shell parsers."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile


with tempfile.TemporaryDirectory() as directory:
    bindir = Path(directory)
    scripts = {
        "id": '#!/bin/sh\nprintf "%s\\n" "$TEST_UID"\n',
        "su": '#!/bin/sh\n[ "$1" = -c ] || exit 2\nexec /bin/sh -c "$2"\n',
        "app_process": '#!/usr/bin/env python3\nimport json, os, sys\n'
                       'print(json.dumps([os.environ["CLASSPATH"], sys.argv[1:]]))\n',
    }
    for name, source in scripts.items():
        path = bindir / name
        path.write_text(source)
        path.chmod(0o755)
    values = ["", "space value", "a'b", 'a"b', "$HOME", "$(exit 9)",
              "`exit 9`", "a;b", "a\\b", "a\nb", "*", "中文"]
    classpath = "/tmp/server ' \" $HOME.jar"
    args = ["CLASSPATH=" + classpath, "app_process", *values]
    command = subprocess.check_output([sys.argv[1], *args], text=True)
    for uid in ("0", "2000"):
        env = dict(os.environ, PATH=str(bindir) + ":" + os.environ["PATH"], TEST_UID=uid)
        result = subprocess.check_output(["/bin/sh", "-c", command], env=env, text=True)
        assert json.loads(result) == [classpath, values], result
    # A denied root request must propagate failure, without running the server.
    (bindir / "su").write_text("#!/bin/sh\nexit 37\n")
    result = subprocess.run(["/bin/sh", "-c", command], env=env, capture_output=True)
    assert result.returncode == 37 and not result.stdout
print("Root command round-trip and denial tests passed")
