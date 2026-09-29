# Full-stack app: Angular + Spring Boot + MySQL

Containerized with Docker Compose and built/pushed by a Jenkins pipeline.
Application source code originally provided as course material by Alaa Rami (ESPRIT): https://github.com/Alaa-Rami/DevOps-AppGestionDesProjets

## Services

| Service | Image | Port |
|---------|-------|------|
| `db` | mysql:8.0 (persistent volume `mysql-data`) | internal |
| `backend` | Spring Boot 4.1 / Java 17 | 8083 |
| `frontend` | Angular 22 served by nginx (proxies `/api/` to the backend) | 4200 |

## Run

    cp .env.example .env
    docker compose up -d --build

## Pipeline

The `Jenkinsfile` runs four stages: Checkout, Build Backend Image, Build Frontend Image, Push DockerHub.
Docker Hub credentials are stored in Jenkins (ID `dockerhub-credentials`), never in the repo.
