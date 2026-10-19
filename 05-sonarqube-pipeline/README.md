# Atelier 6 - Intégration de SonarQube dans un pipeline Jenkins

Analyse continue de la qualité du code du backend Spring Boot (projet de gestion de projets) avec SonarQube Community Build et Jenkins, sur une VM Ubuntu 22.04.

## Architecture

Développeur (git push) -> Jenkins (build, tests) -> SonarScanner for Maven (`sonar:sonar`) -> SonarQube Server -> Quality Gate -> Package.

| Composant | Version / détail |
|---|---|
| SonarQube | Community Build 26.9.0, conteneur Docker `sonarqube:community`, port 9000, base H2 intégrée (évaluation uniquement) |
| Jenkins | 2.568.3, plugin SonarQube Scanner 2.19.0 |
| Plugin Maven | `sonar-maven-plugin` 5.8.0.7211 (version fixée dans le `pom.xml`) |
| Couverture | JaCoCo 0.8.13 |
| Outils sur l'hôte | Java 21 et Maven 3.6.3 ; le projet compile en Java 17 (`java.version` du `pom.xml`) |

## 1. Lancer SonarQube

Prérequis : Elasticsearch (intégré à SonarQube) exige `vm.max_map_count >= 262144` (65530 par défaut sur Ubuntu). Les conteneurs partagent le noyau de l'hôte, donc le réglage se fait sur la VM :

```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee /etc/sysctl.d/99-sonarqube.conf
```

```bash
docker pull sonarqube:community
docker run -d --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_extensions:/opt/sonarqube/extensions \
  -v sonarqube_logs:/opt/sonarqube/logs \
  --restart unless-stopped \
  sonarqube:community
```

Volumes nommés : les projets et l'historique survivent à la suppression du conteneur. Pour le relancer : `docker start sonarqube` (ne pas refaire `docker run`).

## 2. Configuration dans Jenkins

- Credential `sonarqube-token` (type *Secret text*) : le token d'analyse SonarQube y est stocké. Il n'apparaît dans aucun fichier du dépôt.
- *Administrer Jenkins > System > SonarQube servers* : nom `SonarQube`, URL du serveur, token `sonarqube-token`, case *Environment variables* cochée.
- *Administrer Jenkins > Tools* : installation `SonarScanner`, ainsi que les outils `JAVA_HOME` (JDK 21) et `M2_HOME` (Maven) déjà définis à l'atelier 3.
- Webhook SonarQube vers Jenkins (*Administration > Configuration > Webhooks*) : `http://<IP-de-la-VM>:8080/sonarqube-webhook/`. Sans lui, `waitForQualityGate` reste bloqué jusqu'au timeout.
- Job `Pipeline-SonarQube` : *Pipeline script from SCM*, branche `*/main`, Script Path `05-sonarqube-pipeline/Jenkinsfile`.

## 3. Le pipeline

Le [Jenkinsfile](Jenkinsfile) contient les 6 stages demandés :

1. **GIT** : `checkout scm`.
2. **Build** : `mvn clean compile`.
3. **Tests** : `mvn test jacoco:report` (tests unitaires et rapport de couverture).
4. **SonarQube** : `mvn sonar:sonar` dans `withSonarQubeEnv('SonarQube')`.
5. **Quality Gate** : `waitForQualityGate abortPipeline: true`.
6. **Package** : `mvn package -DskipTests`, exécuté seulement si le Quality Gate passe.

Le `pom.xml` du backend contient la clé du projet, le nom, l'URL de SonarQube et le chemin du rapport JaCoCo. Le token est fourni par Jenkins à l'exécution (`-Dsonar.token=$SONAR_AUTH_TOKEN`), jamais écrit dans le dépôt.

## 4. Résultats

Première analyse (build #2) : 623 lignes de code.

| Question | Réponse |
|---|---|
| Quality Gate | Passed |
| Bugs | 0 (note A) |
| Vulnerabilities | 12 problèmes de sécurité (note D) |
| Code Smells | 0 (note A) |
| Coverage | 1,4 % |
| Duplications | 0,0 % |
| Dette technique | 2 j 1 h |

Pourquoi le Quality Gate « Sonar way » est passé malgré 12 problèmes et 1,4 % de couverture : toutes ses conditions portent sur le **nouveau code** (0 nouveau problème, couverture du nouveau code >= 80 %, duplications <= 3 %, hotspots revus). À la première analyse, il n'existe pas de nouveau code à juger ; l'onglet *New Code* se remplit à partir de la deuxième analyse. Le principe est « Clean as You Code » : on n'exige pas que l'ancien code soit parfait, on exige que chaque ajout soit propre.

## 5. Analyse des 12 problèmes

| Règle | Nombre | Sévérité | Effort |
|---|---|---|---|
| Replace this persistent entity with a simple POJO or DTO object | 8 | High | 10 min chacun |
| Make sure that enabling CORS is safe here | 4 | Medium | 4 h chacun |

- **Entités persistantes en `@RequestBody`** : le JSON du client est converti directement en entité JPA (risque de *mass assignment* : le client peut imposer l'`id` ou d'autres champs gérés par le serveur). Correction : un DTO limité aux champs autorisés.
- **`@CrossOrigin("*")`** : zone à vérifier (Security Hotspot), l'API est ouverte à toutes les origines. Non corrigé ici : le comportement dépend de la configuration du proxy nginx.

## 6. Correction et cycle de contrôle qualité

Correction appliquée à `EntrepriseController` : `POST /add` et `PUT /update` reçoivent un `EntrepriseDTO` (`id`, `nom`, `adresse`) au lieu de l'entité. L'`id` envoyé par le client est ignoré à la création, et la modification d'une entreprise inexistante renvoie 404. Vérification avec le vrai jar : un `POST` avec `"id":999` a créé l'enregistrement avec un `id` généré par la base. Trois tests unitaires ont été ajoutés.

| Build | Résultat | Explication |
|---|---|---|
| #4 | Quality Gate **ERROR**, `Package` ignoré | 1 nouveau problème dans le code ajouté (règle : une seule invocation pouvant lever une exception dans la lambda de `assertThrows`) |
| #5 | Quality Gate **OK**, `Package` exécuté | Test corrigé. Couverture du nouveau code : 92,9 % |

Le nombre de problèmes est passé de 12 à 10.

## 7. Écarts volontaires par rapport au support

- Le token n'est pas écrit dans le `pom.xml` (`<sonar.login>`) : le dépôt est public. Il est lu depuis un credential Jenkins, et la propriété s'appelle désormais `sonar.token` (`sonar.login` est dépréciée).
- L'adresse de SonarQube est celle de la VM, et non l'adresse d'exemple du support.
- Le job utilise *Pipeline script from SCM* (et non un script collé) car le stage GIT exécute `checkout scm` ; le Jenkinsfile est ainsi versionné.
- Le bonus « introduire un bug volontairement » n'a pas été réalisé ; le blocage par le Quality Gate est démontré par le build #4.

## 8. Limites connues

- La base H2 intégrée convient à l'évaluation ; la production demande PostgreSQL.
- La Community Build ne détecte pas les injections SQL et XSS (bandeau « Limited security analysis »).
- Le test `contextLoads` démarre toute l'application et dépend d'un MySQL joignable sur `localhost:3306`.
- 10 problèmes hérités restent à traiter : 6 entités persistantes (Equipe, Projet, ProjetDetaille) et 4 CORS.
