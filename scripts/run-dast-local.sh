#!/bin/bash
set -e

./scripts/prepare-dast.sh

docker run --network host \
  -v $(pwd)/zap-reports:/zap/wrk/:rw \
  -e "_JAVA_OPTIONS=-Xmx4g" \
  -t ghcr.io/zaproxy/zaproxy:stable \
  zap.sh -cmd -autorun /zap/wrk/zap.yaml
