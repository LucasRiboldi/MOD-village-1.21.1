#!/usr/bin/env sh
set -eu
if [ -z "${PYTHON:-}" ]; then
    if command -v python3 >/dev/null 2>&1; then
        PYTHON=python3
    else
        PYTHON=python
    fi
fi
exec "$PYTHON" "$(dirname "$0")/test_lab.py" "$@"
