\# 🔒 Configuration HTTPS Proxy (Nginx)



Ce dépôt contient une configuration de proxy inverse sécurisée par SSL. Pour respecter les bonnes pratiques de sécurité, les certificats ne sont pas inclus dans le dépôt.



Chaque membre de l'équipe doit générer ses propres certificats localement avant de lancer les conteneurs.



\---



\## 1. Génération des certificats



Le script `init-certs.sh` automatise la création du dossier `certs/` et la génération des fichiers `.key` et `.crt`.



\### 🐧 Sur Linux

Ouvrez un terminal à la racine du projet et exécutez :

```bash

chmod +x init-certs.sh

./init-certs.sh

```



\### 🪟 Sur Windows (via Git Bash)

Si vous avez installé Git, c'est la méthode recommandée :

1\. Faites un \*\*clic droit\*\* dans le dossier du projet.

2\. Choisissez \*\*"Git Bash Here"\*\*.

3\. Dans le terminal qui s'ouvre, exécutez :

```bash

sh init-certs.sh

```



\### 🪟 Sur Windows (via PowerShell)

Si vous n'utilisez pas Git Bash, ouvrez PowerShell dans le dossier du projet et exécutez (vous devez installer necessairement openssl):

```powershell

if (!(Test-Path -Path "certs")) { New-Item -ItemType Directory -Path "certs" }

\& "C:\\Chemin\\Vers\\binaire\\openssl.exe" req -x509 -nodes -days 365 -newkey rsa:2048 -keyout ./certs/selfsigned.key -out ./certs/selfsigned.crt -subj "/C=MA/L=Local/O=DevTeam/CN=localhost"

```

