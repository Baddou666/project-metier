# Staging Deployment

Ce dossier assemble la stack Docker Swarm de staging a partir des compose unitaires dans `deploy/staging/services`.

## Placement Swarm

- `backend` : proxy HTTPS, frontend, rate limiter, token manager, Redis et Promtail.
- `ai` : PostgreSQL, model handler, transformer model service et fichiers du modele.
- `monitoring` : Loki et Grafana.
- `manager` : orchestration Swarm et rendu du compose final, sans service applicatif impose.

## Etapes de deploiement

1. Creer ou mettre a jour l'infrastructure Terraform depuis `iac/staging/terraform`.
2. Verifier `iac/staging/ansible/inventory.ini` avec les hotes Tailscale/DNS attendus.
3. Copier `iac/staging/ansible/group_vars.example.yml` vers `iac/staging/ansible/group_vars/all.yml`.
4. Mettre `deploy_stack: true` dans `group_vars/all.yml` et ajuster `project_repo_version`, les URLs publiques et les chemins si besoin.
5. Renseigner `iac/staging/ansible/vault.yml` avec les variables `vault_*` requises : GHCR, Redis, secret gateway, PostgreSQL, JWT et Grafana.
6. Lancer le playbook depuis `iac/staging/ansible` :

```sh
ansible-playbook -i inventory.ini playbook.yml --ask-vault-pass
```

Le playbook installe Docker, initialise Swarm, labellise les noeuds, fait un sparse checkout limite a `deploy/staging/*`, prepare uniquement les fichiers necessaires par noeud, genere les certs/cles locales, telecharge le modele sur le noeud `ai`, rend le `.env`, puis deploie la stack.

## Verification

Depuis le manager Swarm :

```sh
docker node ls
docker stack services ai-detector-staging
docker stack ps ai-detector-staging
```

Tester ensuite le proxy :

```sh
curl -k -X POST https://ai-detect-staging/api/anonym-token/get -H "X-Real-IP: 1.1.1.1"
```

Grafana est expose sur le port configure par `GRAFANA_PORT`.
