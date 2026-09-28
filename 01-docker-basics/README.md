# Docker basics

| Command | Purpose |
|---------|---------|
| `docker run hello-world` | Check that Docker works |
| `docker run -it ubuntu bash` | Interactive container |
| `docker run -d -p 8081:80 --name test-nginx nginx` | Detached container with port mapping |
| `docker ps` / `docker ps -a` | Running containers / all containers |
| `docker stop <name>` | Stop a container |
| `docker rm <name>` | Remove a stopped container |
| `docker images` | List local images |
| `docker rmi <image>` | Remove an image |
