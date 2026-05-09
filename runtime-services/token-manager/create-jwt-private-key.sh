#!/usr/bin/env sh

set -eu

mkdir -p ./certs

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl is required but was not found in PATH" >&2
  exit 1
fi

if [ -f ./certs/jwt-private.pem ]; then
  echo "already existing key in : ./certs/jwt-private.pem"
  exit
fi

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out ./certs/jwt-private.pem

chmod 644 ./certs/jwt-private.pem 2>/dev/null || true

echo "Private key generated at: certs/jwt-private.pem"
