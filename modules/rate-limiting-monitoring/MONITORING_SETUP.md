# Logs JSON Structurés pour Loki/Grafana - Rate Limiter

## Résumé des Modifications

Le service **rate-limiter** a été configuré pour générer des **logs JSON structurés** collectés par **Loki** et visualisés dans **Grafana**. Cette architecture permet un monitoring avancé et des alertes en temps réel.

### ✅ Modifications Effectuées

1. **Dépendance Logstash Encoder** (pom.xml)
   - Ajout de `logstash-logback-encoder` pour encoder les logs en JSON

2. **Configuration Logback** (logback-spring.xml)
   - Formatage JSON avec champs métier structurés
   - Rotation des logs (100 MB, 30 jours, 3 GB max)
   - Console + fichier pour prod

3. **Classe LogContext** (utils/LogContext.java)
   - Utilitaire pour ajouter des champs MDC aux logs
   - Constantes pour les types d'événements métier
   - Gestion automatique du contexte

4. **Logs Restructurés**
   - TokenController, RateLimitingService, TokenJwtService, etc.
   - Utilisation de LogContext pour les champs structurés
   - Niveaux log cohérents (INFO métier, DEBUG technique, WARN risques, ERROR erreurs)

5. **Fichiers de Configuration**
   - `loki-config.yml` : Configuration Loki
   - `promtail-config.yml` : Pipeline de collecte/parsing JSON
   - `prometheus.yml` : Scrape optionnel des métriques
   - `modules/rate-limiting-monitoring/docker-compose.yml` : Compose d'assemblage du module monitoring
   - `backend-services/loki-service/docker-compose.loki.yml` : Services Loki + Promtail + Prometheus
   - `backend-services/grafana-service/docker-compose.grafana.yml` : Service Grafana dédié

6. **Documentation**
   - `LOKI_GRAFANA_SETUP.md` : Guide complet d'intégration
   - Queries Loki prêtes à l'emploi
   - Dashboard Grafana recommandé

## Champs JSON Disponibles

| Champ | Exemple | Utilité |
|-------|---------|---------|
| `event_type` | TOKEN_REQUEST, RATE_LIMIT_REACHED | Filtrer par type d'événement |
| `source_ip` | 192.168.1.1 | Tracker les abus par IP |
| `user_id` | UUID | Identifier les utilisateurs |
| `status` | SUCCESS, RATE_LIMIT_REACHED | État de l'opération |
| `counter_value` | 5 | Nombre de tentatives |
| `rate_limit` | 10 | Limite configurée |
| `limit_reached` | true/false | Dépassement de limite |
| `exception_message` | Error msg | Contexte d'erreur |

## Démarrage Rapide

### 1. Build du Rate Limiter

```bash
cd services-src-code/rate-limiting
./mvnw clean package -DskipTests
```

### 2. Démarrer la Stack Monitoring (Loki + Promtail + Prometheus + Grafana)

```bash
# À la racine du projet
docker compose -f modules/rate-limiting-monitoring/docker-compose.yml up -d
```

### 3. Vérifier le Démarrage

```bash
# Vérifier les logs du rate-limiter
docker logs -f rate-limiter

# Vérifier Loki reçoit les logs
curl http://localhost:3100/loki/api/v1/query?query='{app="rate-limiter"}'

# Accéder aux dashboards
# - Grafana: http://localhost:3000 (admin / admin)
# - Loki API: http://localhost:3100
# - Promtail: http://localhost:9080
```

### 4. Créer des Dashboards dans Grafana

1. Aller à **Connections** → **Add Data Source**
2. Sélectionner **Loki** et connecter à `http://loki:3100`
3. Créer des panels avec les queries proposées dans `LOKI_GRAFANA_SETUP.md`

## Queries Loki - Exemples

### Tous les logs
```loki
{app="rate-limiter"}
```

### Limites atteintes
```loki
{app="rate-limiter"} | json | status="RATE_LIMIT_REACHED"
```

### Tokens générés
```loki
{app="rate-limiter"} | json | event_type="TOKEN_GENERATED"
```

### Erreurs
```loki
{app="rate-limiter", level="ERROR"}
```

### Activité par IP
```loki
{app="rate-limiter"} | json | source_ip != "" 
| stats count() by source_ip
```

## Fichier Log

**Localisation** : `logs/rate-limiter.log` à l'intérieur du conteneur

**Exemple de ligne** :
```json
{"timestamp":"2026-04-18T10:00:00.000Z","level":"INFO","logger":"aidetector.apigateway.controller.TokenController","thread":"http-nio-8080-exec-1","message":"Token generated successfully","app":"rate-limiter","version":"1.0.0","event_type":"TOKEN_GENERATED","source_ip":"192.168.1.1","user_id":"550e8400-e29b-41d4-a716-446655440000","status":"SUCCESS"}
```

## Architecture

```
┌─────────────────┐
│ Rate Limiter    │ → logs/rate-limiter.log (JSON)
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Promtail        │ → Parse JSON + Labels
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Loki            │ → Index + Storage
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Grafana         │ → Dashboard + Alertes
└─────────────────┘
```

## Monitoring Recommandé

### Métriques Clés
- Requêtes de token générées par minute
- Nombre de violations de limite par IP
- Taux d'erreur (JWT, Payload, etc.)
- Latence de génération de token
- Top IPs consommatrices

### Alertes
- Plus de 100 violations/5min → **Warning**
- Plus de 50 erreurs/5min → **Critical**
- Plus de 10 IPs bloquées → **Info**

## Points d'Accès

| Service | URL | Credentials |
|---------|-----|-------------|
| Rate Limiter API | http://localhost:8080 | N/A |
| Grafana | http://localhost:3000 | admin / admin |
| Loki API | http://localhost:3100 | N/A |
| Prometheus | http://localhost:9090 | N/A |
| Promtail | http://localhost:9080 | N/A |

## Troubleshooting

### Les logs ne remontent pas
1. Vérifier le volume : `docker volume ls | grep rate`
2. Vérifier Promtail : `docker logs promtail`
3. Vérifier les permissions de fichier

### Champs JSON mal parsés
1. Tester : `cat logs/rate-limiter.log | head -1 | python -m json.tool`
2. Vérifier la pipeline Promtail

### Loki consomme trop de ressources
1. Réduire `max_streams_per_user` dans loki-config.yml
2. Réduire la fréquence Promtail
3. Augmenter la rétention dans les chunks

## Prochaines Étapes

- [ ] Configurer les alertes Grafana
- [ ] Activer l'authentification Loki/Grafana
- [ ] Intégrer Prometheus pour les métriques
- [ ] Créer des dashboards personnalisés
- [ ] Configurer les notifications (Slack, Email)
- [ ] Tester la charge avec des outils comme JMeter

## Notes

- **Pas de haute disponibilité** : Config single-node appropriée pour dev/staging
- **Rétention** : 30 jours par défaut, adaptable dans loki-config.yml
- **Scalabilité** : Pour la prod, utiliser Loki en mode distribuée avec S3 backend
