#!/bin/bash
mkdir -p certs
if [ ! -f ./certs/selfsigned.key ]; then
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout ./certs/selfsigned.key \
      -out ./certs/selfsigned.crt \
      -subj "/C=MA/L=Local/O=DevTeam/CN=localhost"
    echo "Certificats générés avec succès dans ./certs/"
else
    echo "Les certificats existent déjà."
fi