# Docker & Jenkins CI/CD Lab

Hands-on lab covering containerization and continuous delivery:

- Docker basics (run, ps, stop, rm, images)
- Custom Dockerfile (Alpine-based image)
- Multi-container stacks with Docker Compose (MySQL + Adminer)
- Full-stack app (frontend + backend + database) with a Jenkins pipeline that builds and pushes images to Docker Hub

## Structure

| Folder | Content |
|--------|---------|
| `01-docker-basics` | Essential Docker commands |
| `02-custom-dockerfile` | Custom image built from a Dockerfile |
| `03-compose-mysql-adminer` | Docker Compose stack: MySQL + Adminer |
| `04-fullstack-app-pipeline` | Full-stack app, Compose file and Jenkinsfile |

## Environment

- Ubuntu 22.04 VM (VMware Workstation)
- Docker Engine 29.x and Docker Compose v5
- Jenkins 2.568 (LTS)
