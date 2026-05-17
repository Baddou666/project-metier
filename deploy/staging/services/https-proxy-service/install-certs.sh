#!/bin/bash
mkdir -p certs
if [ ! -f ./certs/selfsigned.key ]; then
       MSYS_NO_PATHCONV=1 openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
    -keyout ./certs/selfsigned.key \
    -out ./certs/selfsigned.crt \
    -subj "/C=MA/L=Local/O=DevTeam/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
    echo "Certificats générés avec succès dans ./certs/"
else
    echo "Les certificats existent déjà."
fi