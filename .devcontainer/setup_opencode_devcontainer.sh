#!/usr/bin/env bash
set -euo pipefail

DIST_DIR="/opt/opencode/packages/opencode/dist"
EXPECTED_X64_BIN="${DIST_DIR}/opencode-linux-x64/bin/opencode"

if [[ -d "${DIST_DIR}" ]]; then
  OPENCODE_BIN="$(find "${DIST_DIR}" -type f -path '*/bin/opencode' -print 2>/dev/null | head -n1 || true)"
else
  OPENCODE_BIN=""
fi

if [[ -n "${OPENCODE_BIN}" ]]; then
  sudo ln -sf "${OPENCODE_BIN}" /usr/local/bin/opencode

  # Compatibility shim for tools that still use the hardcoded x64 path.
  if [[ "${OPENCODE_BIN}" != "${EXPECTED_X64_BIN}" ]]; then
    sudo mkdir -p "$(dirname "${EXPECTED_X64_BIN}")"
    sudo ln -sf "${OPENCODE_BIN}" "${EXPECTED_X64_BIN}"
  fi

  echo "[setup-opencode] Linked opencode: ${OPENCODE_BIN}"
else
  echo "[setup-opencode] WARNING: no opencode binary found under ${DIST_DIR}"
fi

if [[ -f /opt/opencode/scripts/opencodeplus ]]; then
  sudo ln -sf /opt/opencode/scripts/opencodeplus /usr/local/bin/opencodeplus
  sudo chmod +x /opt/opencode/scripts/opencodeplus
  if [[ -f /opt/opencode/scripts/opencodeplus.py ]]; then
    sudo chmod +x /opt/opencode/scripts/opencodeplus.py
  fi
  echo "[setup-opencode] Linked opencodeplus"
else
  echo "[setup-opencode] WARNING: opencodeplus not found at /opt/opencode/scripts/opencodeplus"
fi
