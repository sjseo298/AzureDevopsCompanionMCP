FROM mcr.microsoft.com/devcontainers/base:noble

# bubblewrap/bin required by sandboxed tooling in this workspace.
RUN apt-get update \
    && apt-get install -y --no-install-recommends bubblewrap \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
