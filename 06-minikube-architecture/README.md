# Atelier 7 - Architecture Kubernetes avec Minikube

Installation d'un cluster Kubernetes local avec Minikube (driver Docker) et observation des composants du Control Plane et du Worker Node vus en cours.

## Environnement

| Élément | Détail |
|---|---|
| VM | Ubuntu 22.04, 8 CPU, 16 Go de RAM (VMware) |
| Minikube | v1.39.0 (Kubernetes v1.37.0, runtime containerd 2.3.4) |
| kubectl | v1.35.9 (installé via snap) |
| Docker | 29.9.1, utilisé comme driver |

La VM héberge déjà un cluster k3s utilisé pour un autre projet. Il n'a pas été modifié : Minikube travaille avec un fichier de configuration `kubectl` séparé (voir ci-dessous).

## 1. Installer et démarrer Minikube

```bash
cp -p ~/.kube/config ~/.kube/config.k3s-backup      # sauvegarde de la configuration existante
cd /tmp
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64
cd ~
export KUBECONFIG=$HOME/.kube/minikube.yaml         # isole Minikube du cluster k3s
minikube start --driver=docker --cpus=2 --memory=2048 --disk-size=20g
```

`KUBECONFIG` n'est défini que dans le terminal courant : dans un autre terminal, `kubectl` continue de parler à k3s. Minikube écrit son contexte dans `minikube.yaml`, et non dans `~/.kube/config`.

Vérifications :

```bash
minikube status
kubectl get nodes
kubectl cluster-info
```

Résultat : un seul nœud `minikube`, rôle `control-plane`, état `Ready`. Il cumule les rôles Control Plane et Worker, ce qui est la particularité de Minikube.

## 2. Explorer l'architecture

```bash
kubectl get pods -n kube-system
kubectl describe node minikube
minikube ssh -- "hostname; systemctl is-active kubelet containerd"
minikube ssh -- "sudo crictl ps"
docker ps --filter name=minikube
```

Constats :

- `etcd`, `kube-apiserver`, `kube-scheduler` et `kube-controller-manager` sont les composants du Control Plane ; `kube-proxy` est un composant du Worker Node. `coredns` (DNS interne), `storage-provisioner` (propre à Minikube) et `kindnet` (réseau) complètent la liste.
- Le `kubelet` et `containerd` sont des services du nœud, pas des pods : ils n'apparaissent pas dans `kubectl get pods`.
- Le nœud est un conteneur Docker de la VM (image `kicbase`), pas une machine virtuelle : il partage le noyau de la VM (même version de noyau affichée par `minikube ssh`).
- Le conteneur du nœud est plafonné à 2 Gio de mémoire (`docker inspect minikube`). `kubectl describe node` affiche en revanche la capacité vue par le système, soit celle de toute la VM.

## 3. Observer le Control Plane en action

```bash
kubectl run test-pod --image=nginx --restart=Never
kubectl get pods -o wide
kubectl describe pod test-pod
kubectl delete pod test-pod
```

La section `Events` de `kubectl describe pod` retrace le parcours de la demande :

1. `Scheduled` : le kube-scheduler assigne le Pod au nœud `minikube` (un seul choix possible).
2. `Pulling` puis `Pulled` : le kubelet fait télécharger l'image `nginx` par le runtime.
3. `Created` puis `Started` : le kubelet fait lancer le conteneur.

La condition `PodScheduled: True` confirme l'assignation. Le kube-apiserver et etcd interviennent avant (réception et enregistrement de la demande) mais ne figurent pas dans les événements.

## 4. Problème rencontré : nœud `NotReady`

Après `minikube start`, le nœud est resté `NotReady` et le pod `kube-proxy` en `CrashLoopBackOff`. Diagnostic :

- Condition du nœud : `container runtime network not ready ... cni plugin not initialized`.
- Logs de `kube-proxy` : `fsnotify watcher init: too many open files`.
- Limite inotify du noyau : `fs.inotify.max_user_instances = 128`, alors que 178 descripteurs inotify étaient ouverts sur la VM (nombreux conteneurs en parallèle).

Correction (réglage du noyau de la VM, rendu permanent) :

```bash
printf "fs.inotify.max_user_instances=1024\nfs.inotify.max_user_watches=524288\n" | sudo tee /etc/sysctl.d/99-inotify.conf
sudo sysctl --system
kubectl delete pod -n kube-system <pod kube-proxy>   # recréé aussitôt par son DaemonSet
```

Après la correction, `kube-proxy` démarre et le nœud passe à `Ready`. Le pod recréé porte un autre nom : un Pod recréé n'est jamais le même Pod.

## 5. Différences avec le support

- Le support a été réalisé avec Minikube 1.38.1 (Kubernetes 1.35.1, runtime Docker). Minikube 1.39.0 utilise Kubernetes 1.37.0 et containerd par défaut, ce qui correspond à ce que dit le cours.
- Le client `kubectl` (1.35) a deux versions mineures d'écart avec le serveur (1.37), au-delà de la tolérance officielle d'une version : un avertissement s'affiche, sans effet sur les commandes de l'atelier.

## 6. Vérification du cluster k3s existant

Après l'atelier : le fichier `~/.kube/config` est identique à sa sauvegarde, et la liste des pods k3s contient les mêmes pods qu'avant.

## Choix de la solution

Minikube est adapté à l'apprentissage et au test local (simple, officiel, léger). Il n'est pas adapté à la production : un seul nœud, donc aucune haute disponibilité. Pour un petit cluster réel on choisirait kubeadm, et pour la production un service managé (EKS, GKE, AKS).
