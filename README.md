# docker-jenkins-cicd-lab

Chaîne DevOps complète construite pas à pas pendant le module DevOps (ESPRIT, 2026-2027) : conteneurisation, intégration continue, qualité du code, orchestration et supervision d'une application Spring Boot / Angular / MySQL.

## Vue d'ensemble

    Git/GitHub --> Jenkins --> Maven (build + tests) --> SonarQube (Quality Gate)
                                        |
                                        v
                              Docker (images) --> Docker Compose / Minikube
                                        |
                                        v
              Prometheus <-- scrape -- Node Exporter / Jenkins / Spring Boot
                  |            
                  +--> Grafana (dashboard)     +--> Alertmanager --> notification (webhook)

## Contenu du dépôt

| Dossier | Contenu |
|---|---|
| `01-docker-basics` | Premiers pas avec Docker : images et conteneurs |
| `02-custom-dockerfile` | Écriture et construction d'un Dockerfile personnalisé |
| `03-compose-mysql-adminer` | Docker Compose : MySQL et Adminer |
| `04-fullstack-app-pipeline` | Application fil rouge (Spring Boot, Angular, MySQL) lancée avec Compose, et pipeline Jenkins associé |
| `05-sonarqube-pipeline` | Pipeline Jenkins avec analyse SonarQube et Quality Gate, couverture JaCoCo |
| `06-minikube-architecture` | Cluster Kubernetes local avec Minikube : architecture et composants |
| `09-monitoring` | Supervision : Prometheus, Grafana, Node Exporter, Alertmanager |

Chaque dossier contient son propre `README.md` avec les commandes et les explications détaillées.

## Technologies

| Domaine | Outils |
|---|---|
| Application | Spring Boot 4.1.0 (Java 17), Angular, MySQL 8.0 |
| Build et tests | Maven, JUnit, JaCoCo |
| CI | Jenkins 2.580 |
| Qualité du code | SonarQube Community |
| Conteneurs | Docker, Docker Compose |
| Orchestration | Minikube 1.39.0 (Kubernetes 1.37.0) |
| Supervision | Prometheus, Grafana, Node Exporter, Alertmanager, Micrometer |

## Ports utilisés sur la VM

| Service | Port |
|---|---|
| Jenkins | 8080 |
| SonarQube | 9000 |
| Backend Spring Boot | 8083 |
| Frontend Angular | 4200 |
| Adminer | 8082 |
| MySQL | 3306 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Alertmanager | 9093 |
| Node Exporter | 9100 |

## Démarrage rapide

Application fil rouge :

    cd 04-fullstack-app-pipeline
    docker compose up -d --build

Supervision :

    cd 09-monitoring
    docker compose up -d

## Pipelines Jenkins

- **Pipeline fullstack** (`04-fullstack-app-pipeline/Jenkinsfile`) : construction des images Docker et déploiement avec Docker Compose.
- **Pipeline SonarQube** (`05-sonarqube-pipeline/Jenkinsfile`) : GIT, Build, Tests, SonarQube, Quality Gate, Package. Le pipeline échoue si le Quality Gate n'est pas respecté.

## Supervision et alertes

Prometheus collecte les métriques de la VM, de Jenkins et de l'application Spring Boot. Grafana affiche un dashboard unique à trois niveaux. Trois règles d'alerte (CPU de la VM, exécuteurs Jenkins, erreurs HTTP 5xx) sont envoyées par Alertmanager vers un webhook. Détails dans `09-monitoring/README.md`.

## Environnement

Machine virtuelle Ubuntu 22.04 avec Docker et Jenkins installés. Les secrets, comme le token SonarQube, sont stockés dans les Credentials Jenkins.
