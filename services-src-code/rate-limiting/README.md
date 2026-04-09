# 📦 Guide de Génération Locale de l'Image Docker

> [!NOTE]
> Ce guide explique comment transformer le code source en une image Docker prête à l'emploi sans passer par un registre externe.

### 🛠️ Procédure de Build
Pour générer l'image sur votre machine, utilisez la commande suivante à la racine du projet (ou éxecuter le script qui se trouve à la racine du projet `create-update-image(OS).(bat/sh)` selon votre OS):

```bash
# Pour Windows (PowerShell/CMD)
./mvnw clean spring-boot:build-image -DskipTests

# Pour Linux/Mac
chmod +x mvnw
./mvnw clean spring-boot:build-image -DskipTests
```

---

### 🔍 Vérification de l'image
Une fois le build terminé (cela peut prendre 1 à 2 minutes la première fois), vérifiez la présence de l'image :

```bash
docker images
```


### ⚠️ Troubleshooting (Lombok & Build)
Si le build échoue avec des erreurs de "Symbol not found" (Lombok) :
1. Assurez-vous d'avoir un **JDK 17+** installé (`java -version`).
2. Lancez impérativement un `clean` avant le build :
   ```bash
   ./mvnw clean spring-boot:build-image -DskipTests
   ```
3. Docker Desktop doit être **actif** durant toute l'opération.