#!/usr/bin/env sh

set -eu

mkdir -p ./certs

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required but was not found in PATH" >&2
  exit 1
fi

if [ -f ./certs/jwt-private.pem ]; then
  echo "Refusing to overwrite existing key: ./certs/jwt-private.pem" >&2
  echo "Remove it first if you really want to regenerate it." >&2
  exit 1
fi

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out ./certs/jwt-private.pem

chmod 644 ./certs/jwt-private.pem 2>/dev/null || true

echo "Private key generated at: certs/jwt-private.pem"
