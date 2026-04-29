# Configuration Loki et Grafana pour Rate Limiter

## Vue d'ensemble

Les logs du service `rate-limiter` sont formatés en **JSON structuré** avec des champs clés pour permettre une intégration facile avec **Loki** (agrégateur de logs) et **Grafana** (visualisation).

## Format des Logs JSON

Tous les logs sont émis en JSON structuré via l'encoder Logstash avec les champs suivants :

```json
{
  "timestamp": "2026-04-18T10:00:00.000Z",
  "level": "INFO",
  "logger": "aidetector.apigateway.controller.TokenController",
  "thread": "http-nio-8080-exec-1",
  "message": "Token generation requested",
  "app": "rate-limiter",
  "version": "1.0.0",
  "environment": "prod",
  "service": "rate-limiter-service",
  "event_type": "TOKEN_REQUEST",
  "source_ip": "192.168.1.100",
  "user_id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "counter_value": 5,
  "rate_limit": 10,
  "detail": "Additional context"
}
```

## Champs Structurés Disponibles

| Champ | Description | Exemples |
|-------|-------------|----------|
| `event_type` | Type d'événement métier | `TOKEN_REQUEST`, `RATE_LIMIT_CHECK`, `IP_VALIDATION`, `TOKEN_VERIFIED` |
| `source_ip` | IP source du client | `192.168.1.1`, `10.0.0.5` |
| `user_id` | ID utilisateur unique | UUID généré |
| `status` | Statut de l'opération | `SUCCESS`, `FAILED`, `DENIED_EXISTING_TOKEN`, `RATE_LIMIT_REACHED` |
| `counter_value` | Valeur du compteur Redis | Nombre de tentatives/tokens |
| `rate_limit` | Limite configurée | Max attempts ou tokens par IP |
| `limit_reached` | Boolean si limite atteinte | `true` ou `false` |
| `redis_key` | Clé Redis utilisée | `attempts:userId`, `token-count:ip` |
| `detail` | Contexte supplémentaire | Message libre |
| `exception_message` | Message d'exception en cas d'erreur | Description de l'erreur |

## Fichier Log

**Localisation** : `logs/rate-limiter.log` (à l'intérieur du conteneur ou via volume Docker)

**Rotation** : Logs rotatés par taille (100 MB) et par date. Rétention : 30 jours, max 3 GB.

## Configuration Loki

### 1. Installation de Loki avec Promtail

Créez un fichier Docker Compose pour Loki + Promtail (sans Grafana) :

```yaml
version: '3.8'

services:
  loki:
    image: grafana/loki:2.9.0
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yml:/etc/loki/local-config.yml
    command: -config.file=/etc/loki/local-config.yml

  promtail:
    image: grafana/promtail:2.9.0
    volumes:
      - /var/log:/var/log
      - ./promtail-config.yml:/etc/promtail/config.yml
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock
    command: -config.file=/etc/promtail/config.yml
    depends_on:
      - loki

networks:
  monitoring:
    name: monitoring-network
    driver: bridge
```

Créez ensuite un second fichier Docker Compose dédié à Grafana :

```yaml
version: '3.8'

services:
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      - monitoring

networks:
  monitoring:
    name: monitoring-network
    driver: bridge

volumes:
  grafana-data:
```

### 2. Configuration Promtail (promtail-config.yml)

Configuration pour collecter les logs du rate-limiter avec des labels stables uniquement :

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /var/lib/promtail/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push
    batchwait: 5s
    batchsize: 1048576

scrape_configs:
  - job_name: rate-limiter
    static_configs:
      - targets:
          - localhost
        labels:
          job: rate-limiter
          __path__: /logs/rate-limiting/*.log

    pipeline_stages:
      - json:
          expressions:
            level: level
            app: app
            environment: environment
            service: service
            event_type: context.event_type
            status: context.status
            route_id: context.route_id
            limit_reached: context.limit_reached

      # Labels stables uniquement. Garder source_ip, user_id, message,
      # redis_key, detail et compteurs dans le JSON pour eviter
      # une cardinalite Loki trop elevee.
      - labels:
          level:
          app:
          environment:
          service:
          event_type:
          status:
          route_id:
          limit_reached:
```
### 3. Configuration Loki (loki-config.yml)

Minimal loki-config.yml :

```yaml
auth_enabled: false

ingester:
  chunk_idle_period: 3m
  max_chunk_age: 6h
  chunk_retain_period: 1m

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  max_entries_limit_per_query: 5000
  max_query_series: 500

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema:
        version: v11
        index:
          prefix: index_
          period: 24h

server:
  http_listen_port: 3100
  log_level: info

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/boltdb-shipper-active
    cache_location: /loki/boltdb-shipper-cache
    shared_store: filesystem
  filesystem:
    directory: /loki/chunks
```

## Configuration Docker Compose du Rate Limiter

Ajoutez des volumes et labels pour Loki :

```yaml
rate-limiter:
  image: ghcr.io/baddou666/rate-limiter:1.0.0
  ports:
    - "8080:8080"
  environment:
    - REDIS_HOST=redis
    - REDIS_PORT=6379
    - JWT_SECRET_KEY=your-secret-key
    - HASH_SALT=your-salt
    - ENVIRONMENT=prod
    - LOG_LEVEL=INFO
  volumes:
    - rate-limiter-logs:/app/logs  # Persister les logs
  labels:
    - "app=rate-limiter"
    - "service=api-gateway"
  logging:
    driver: "json-file"
    options:
      max-size: "10m"
      max-file: "3"

volumes:
  rate-limiter-logs:
```

## Queries Loki pour Grafana

### 1. Tous les logs du rate-limiter

```loki
{app="rate-limiter"}
```

### 2. Filtrer par type d'événement

```loki
{app="rate-limiter"} | json | event_type="TOKEN_REQUEST"
```

### 3. Limites de débit atteintes

```loki
{app="rate-limiter"} | json | status="RATE_LIMIT_REACHED"
```

### 4. Tokens générés avec succès

```loki
{app="rate-limiter"} | json | event_type="TOKEN_GENERATED" | status="SUCCESS"
```

### 5. Erreurs par IP

```loki
{app="rate-limiter", level="ERROR"} | json | source_ip != ""
```

### 6. Requêtes échouées (authentification)

```loki
{app="rate-limiter"} | json | event_type="TOKEN_VERIFY_FAILED"
```

### 7. Compteur d'activités par utilisateur

```loki
{app="rate-limiter"} | json | user_id != "" 
| stats count() as total by user_id
```

### 8. Distribution des IPs source

```loki
{app="rate-limiter"} | json | source_ip != "" 
| stats count() as requests by source_ip
```

### 9. Statistiques des limites de débit

```loki
{app="rate-limiter"} | json | rate_limit_reached="true"
| stats count() as blocked by source_ip
```

## Dashboard Grafana - Recommandé

### Panels à créer :

1. **Token Requests** (Counter)
   ```loki
   {app="rate-limiter"} | json | event_type="TOKEN_REQUEST" | stats count()
   ```

2. **Rate Limit Violations** (Graph/Timeseries)
   ```loki
   {app="rate-limiter"} | json | status="RATE_LIMIT_REACHED" | stats count() by source_ip
   ```

3. **Erreurs** (Stat)
   ```loki
   {app="rate-limiter", level="ERROR"} | stats count()
   ```

4. **Top IPs** (Table)
   ```loki
   {app="rate-limiter"} | json | source_ip != "" | stats count() by source_ip | sort
   ```

5. **Événements en temps réel** (Logs)
   ```loki
   {app="rate-limiter"} | json
   ```

6. **Uptime/Health** (Gauge)
   ```loki
   {app="rate-limiter", event_type="HEALTH_CHECK"} | stats count()
   ```

## Points d'Accès

| Service | URL |
|---------|-----|
| Grafana | http://localhost:3000 |
| Loki API | http://localhost:3100 |
| Promtail | http://localhost:9080 |
| Rate Limiter API | http://localhost:8080 |

**Credentials Grafana** :
- Username: `admin`
- Password: `admin` (à changer en prod)

## Troubleshooting

### Logs ne remontent pas à Loki
1. Vérifiez que le volume `rate-limiter-logs` est bien monté
2. Vérifiez les logs du Promtail : `docker logs promtail`
3. Vérifiez la configuration Promtail (chemin du fichier log)

### Champs JSON non parsés
1. Vérifiez que le JSON est valide : `cat logs/rate-limiter.log | python -m json.tool`
2. Vérifiez la pipeline Promtail pour les correspondances de champs

### Performance dégradée
1. Réduisez la fréquence des scrapes Promtail
2. Limitez les labels Promtail aux champs stables et ajustez les limites Loki (`max_entries_limit_per_query`, `max_query_series`)
3. Vérifiez l'espace disque pour les chunks Loki

## Intégration avec Alerting

Exemples d'alertes Grafana :

```yaml
# Trop de violations de limite
- alert: HighRateLimitViolations
  expr: count({app="rate-limiter"} |= "RATE_LIMIT_REACHED") > 100
  for: 5m
  labels:
    severity: warning

# Taux d'erreur élevé
- alert: HighErrorRate
  expr: count({app="rate-limiter", level="ERROR"}) > 50
  for: 5m
  labels:
    severity: critical
```

## Notes

- **Pas d'authentification** : Configuration de base sans authentification (à ajouter en prod)
- **Rétention** : Ajustez selon vos besoins (par défaut 30 jours)
- **Volume** : Adaptez la taille des volumes selon la charge attendue
- **Labeling** : Les labels permettent des requêtes rapides et des agrégations
