# 🚀 API Gateway & Rate Limiter

Ce projet contient l’infrastructure Docker pour le service de gestion des flux et de sécurité.

```
+-----------------------------------------------------------------------+
| ⚠️  ATTENTION : ÉTAPE PRÉALABLE OBLIGATOIRE                            |
+-----------------------------------------------------------------------+
| Avant de lancer le "docker-compose up", vous DEVEZ générer l'image    |
| locale du service Spring Boot. Docker Compose ne peut pas construire  |
| l'image automatiquement car elle utilise le Buildpack Spring Boot.    |
+-----------------------------------------------------------------------+
```

---

## 🛠️ Guide de démarrage rapide

### 1️⃣ Prérequis

* Docker installé
* Docker Compose installé
* Java 17+
* Un fichier `.env` à la racine du projet (voir section **Sécurité**)

---

### 2️⃣ Installation

Suivez strictement cet ordre :

#### Étape 1 — Compiler et créer l’image Docker

(si vous n'avez pas encore générer l'image docker) suivre les étape dans services-src-code/rate-limiting/README.md

#### Étape 2 — Configurer l’environnement

Copiez le fichier d’exemple :

```bash
cp .env.example .env
```

Puis remplissez les valeurs nécessaires.

#### Étape 3 — Lancer l’infrastructure

```bash
docker-compose up -d
```

---

## 🔐 Sécurité & Configuration

L’application utilise une **architecture hybride de configuration** :

### 📦 Fichiers métiers (versionnés Git)

Définissent les règles fonctionnelles :

* longueur minimal du secret jwt
* algorithme de hashage
* paramètres de ratelimiting
* paramètres de hash

Localisation :

```
src/main/resources/config/
```

---

### 🔑 Secrets (non versionnés Git)

Ne doivent **jamais** être commités :

Exemples :

* `JWT_SECRET_KEY`
* `REDIS_PASSWORD`

Ils doivent être définis localement uniquement dans :

```
.env
```

---

## 📁 Structure des fichiers de configuration

```
application.properties
│
├── config/jwt.properties
├── config/ratelimiting.properties
└── config/hash.properties
```

### Rôles

| Fichier                          | Description                         |
| -------------------------------- | ----------------------------------- |
| `application.properties`         | Chef d’orchestre — gère les imports |
| `config/jwt.properties`          | Paramètres tokens + sécurité        |
| `config/ratelimiting.properties` | Seuils et quotas de requêtes        |
| `config/hash.properties`         | Configuration hash                  |

---
