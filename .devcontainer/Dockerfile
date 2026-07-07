FROM mcr.microsoft.com/devcontainers/base:noble

# bubblewrap/bin required by sandboxed tooling in this workspace.
RUN apt-get update \
    && apt-get install -y --no-install-recommends bubblewrap python3 python3-pip python3-venv python3-dev python-is-python3 python3-rich \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
