# Docker Compose: MySQL + Adminer

Two-container stack: a MySQL 8.0 database with a persistent named volume, and Adminer as a web UI.

## Setup

    cp .env.example .env
    # edit .env and set your own passwords
    docker compose up -d

## Access

Adminer: http://<vm-ip>:8082 (server: `mysql-db`)

## Persistence test

Data lives in the `mysql-data` volume, so it survives `docker compose down`.
Use `docker compose down -v` to also delete the volume.
