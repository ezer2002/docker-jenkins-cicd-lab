# Custom Dockerfile

A minimal image based on `alpine:3.18` that runs a shell script at startup.

## Build

    docker build -t mon-image:1.0 .

## Run

    docker run mon-image:1.0

## Why Alpine?

Alpine is about 5 MB versus about 77 MB for Ubuntu: smaller images mean faster builds, faster deployments and a smaller attack surface.
