# Déploiement Kubernetes (Minikube) du projet fil rouge

Déploiement de l'application Spring Boot, Angular et MySQL dans un cluster Minikube. Les images sont récupérées sur Docker Hub (`ezer2002/gestion-projets-backend` et `ezer2002/gestion-projets-frontend`).

## Architecture

    Navigateur --> Service frontend (NodePort 30080) --> Pod frontend (nginx)
                                                              | /api/*  (proxy_pass http://backend:8080/)
                                                              v
                                            Service backend (ClusterIP 8080) --> Pod backend (Spring Boot)
                                                                                      | jdbc:mysql://db:3306
                                                                                      v
                                                    Service db (ClusterIP 3306) --> Pod mysql --> PVC mysql-pvc (1 Gi)

Tout vit dans l'espace de noms `gestion-projets`.

## Fichiers

| Fichier | Contenu |
|---|---|
| `k8s/00-namespace.yaml` | Espace de noms `gestion-projets` |
| `k8s/mysql.yaml` | PersistentVolumeClaim, Deployment MySQL 8.0 et Service `db` |
| `k8s/backend.yaml` | Deployment Spring Boot et Service `backend` |
| `k8s/frontend.yaml` | Deployment Angular/nginx et Service `frontend` (NodePort 30080) |

## Choix de conception

- Les Services s'appellent `db` et `backend`, comme dans Docker Compose : le `nginx.conf` du frontend (`proxy_pass http://backend:8080/`) fonctionne donc sans modification.
- Les identifiants MySQL ne sont pas dans les manifestes : ils viennent d'un **Secret** `mysql-secret`, créé dans le cluster à partir du fichier `.env` local (jamais versionné).
- Un `initContainer` fait attendre le backend tant que MySQL ne répond pas sur le port 3306.
- Sondes de santé : `readinessProbe` et `livenessProbe` du backend sur `/actuator/health`. Sonde de MySQL : `mysqladmin ping`.
- Le backend porte les annotations `prometheus.io/*` pour la supervision.

## Déploiement manuel

    export KUBECONFIG=~/.kube/minikube.yaml
    minikube start
    kubectl create namespace gestion-projets
    kubectl create secret generic mysql-secret -n gestion-projets --from-env-file=../04-fullstack-app-pipeline/.env
    kubectl apply -f k8s/
    kubectl rollout status deployment/backend -n gestion-projets --timeout=420s
    kubectl get pods,svc -n gestion-projets

## Accès et tests

Depuis la VM, par le NodePort de Minikube :

    curl http://192.168.49.2:30080/
    curl http://192.168.49.2:30080/api/entreprise/all

Depuis un navigateur extérieur à la VM (port-forward sur le port 8085) :

    kubectl port-forward --address 0.0.0.0 -n gestion-projets svc/frontend 8085:80

Puis ouvrir `http://IP_VM:8085`.

## Suite

Étape de déploiement automatique dans le Jenkinsfile, puis Prometheus et Grafana dans le cluster.
