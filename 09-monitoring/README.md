# Atelier 9 — Monitoring avec Prometheus et Grafana

Supervision du projet fil rouge à trois niveaux : la **VM hôte**, **Jenkins** et l'application **Spring Boot**.

## Architecture

    VM hôte ....... Node Exporter :9100  /metrics                 --+
    Jenkins ....... plugin Prometheus :8080  /prometheus            +--> Prometheus :9090 --> Grafana :3000
    Spring Boot ... Actuator + Micrometer :8083 /actuator/prometheus --+   (scrape, TSDB, PromQL)  (dashboard)

Prometheus fonctionne en mode **pull** : il interroge lui-même chaque cible toutes les 15 secondes.
Prometheus et Grafana tournent dans Docker avec `network_mode: host`, ce qui leur permet de joindre `localhost:9100`, `localhost:8080` et `localhost:8083` sans configuration réseau supplémentaire.

## Contenu du dossier

| Fichier | Rôle |
|---|---|
| `docker-compose.yml` | Lance Prometheus et Grafana |
| `prometheus/prometheus.yml` | Cibles à scraper (4 jobs) |
| `grafana/provisioning/datasources/prometheus.yml` | Datasource Prometheus déclarée par fichier |
| `grafana/provisioning/dashboards/dashboards.yml` | Chargement automatique des dashboards |
| `grafana/dashboards/devops-unifie.json` | Dashboard unique, 8 panels (un groupe par niveau) |

## Lancement

    docker compose up -d
    docker compose ps

| Interface | URL |
|---|---|
| Prometheus | http://IP_VM:9090 |
| Grafana | http://IP_VM:3000 (identifiants par défaut `admin` / `admin`, à changer) |

## Ce qui a été mis en place

1. **Prometheus et Grafana** via Docker (images `prom/prometheus` et `grafana/grafana`).
2. **VM hôte** : paquet Ubuntu `prometheus-node-exporter` (service systemd, port 9100).
3. **Jenkins** : plugin *Prometheus metrics*, endpoint `/prometheus` (préfixe des métriques : `default_jenkins_`).
4. **Spring Boot** : dépendances `spring-boot-starter-actuator` et `micrometer-registry-prometheus` ajoutées au backend (`04-fullstack-app-pipeline/backend/pom.xml`), endpoint exposé dans `application.properties` :

        management.endpoints.web.exposure.include=health,info,prometheus
        management.metrics.tags.application=backend

   L'image du backend doit être reconstruite : `docker compose up -d --build backend` dans `04-fullstack-app-pipeline`.
5. **Grafana** : datasource et dashboard déployés par *provisioning* (fichiers versionnés, reproductibles).

## Vérifications

Cibles Prometheus (elles doivent toutes être "up") :

    curl -s 'localhost:9090/api/v1/targets' | python3 -c "import sys,json; [print(t['labels']['job'], t['health']) for t in json.load(sys.stdin)['data']['activeTargets']]"

Endpoints de métriques :

    curl -s localhost:9100/metrics | grep -m1 node_memory_MemAvailable_bytes
    curl -s localhost:8080/prometheus/ | grep -m1 default_jenkins_up
    curl -s localhost:8083/actuator/prometheus | grep -m1 jvm_memory_used_bytes

## Requêtes PromQL utilisées

| Niveau | Requête | Type de métrique |
|---|---|---|
| VM | `node_memory_MemAvailable_bytes` | Gauge |
| VM | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)` | Counter + `rate()` |
| Jenkins | `default_jenkins_executors_available` | Gauge |
| Jenkins | `default_jenkins_builds_duration_milliseconds_summary_count` | Summary (nombre de builds par job) |
| Spring Boot | `jvm_memory_used_bytes{application="backend",area="heap"}` | Gauge |
| Spring Boot | `sum(rate(http_server_requests_seconds_count{application="backend"}[1m]))` | Counter + `rate()` |

## Points à retenir

- **Pull vs push** : Prometheus va chercher les métriques sur `/metrics` (ou le chemin configuré avec `metrics_path`). Ici : `/prometheus` pour Jenkins et `/actuator/prometheus` pour Spring Boot.
- **Counter vs Gauge** : un Counter ne fait qu'augmenter, on l'exploite avec `rate()`. Un Gauge monte et descend librement.
- **Rechargement de la configuration** : `docker kill --signal=HUP prometheus` recharge `prometheus.yml` sans arrêter le conteneur.
- **Cette version du plugin Jenkins** n'expose pas de compteurs de builds réussis/échoués. On utilise donc le nombre de builds par job et le résultat du dernier build (`default_jenkins_builds_last_build_result_ordinal`, `0` = SUCCESS).
- **Provisioning Grafana** : la datasource et le dashboard sont décrits par fichiers, donc reproductibles et versionnés.

## Alertes (Alertmanager)

Fichiers : `prometheus/alert.rules.yml`, `alertmanager/alertmanager.yml`, `webhook/receiver.py`.

| Niveau | Règle | Condition |
|---|---|---|
| VM hôte | `VMHoteCPUElevee` | CPU > 80 % pendant 2 min |
| Jenkins | `JenkinsAucunExecuteurDisponible` | `default_jenkins_executors_available == 0` pendant 5 min |
| Spring Boot | `SpringBootTauxErreurs5xxEleve` | taux de réponses HTTP 5xx > 5 % pendant 5 min |

Chaîne : Prometheus évalue les règles, pousse l'alerte à Alertmanager (port 9093), qui la regroupe et l'envoie au récepteur webhook local (port 5001). Le récepteur affiche chaque alerte dans ses logs :

    docker logs webhook

Choix : un webhook plutôt qu'un e-mail, car l'e-mail exige des identifiants SMTP (secrets à ne pas versionner).

## Test de bout en bout

Charge CPU artificielle sur tous les cœurs (arrêt automatique après 7 minutes) :

    for i in $(seq 1 $(nproc)); do timeout 420 yes > /dev/null & done

Résultat observé : `VMHoteCPUElevee` passe de inactive à pending puis firing (page `/alerts` de Prometheus), et le webhook reçoit `[firing] VMHoteCPUElevee`. Quand la charge s'arrête, l'alerte repasse à `[resolved]`.
