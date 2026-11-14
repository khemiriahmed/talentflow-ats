# TalentFlow ATS

Application de gestion RH et de recrutement (Applicant Tracking System).
Stack : Spring Boot, Angular, PostgreSQL, Redis, Docker, AWS / GCP.

## Démarrage rapide

```bash
cp .env.example .env
docker compose --profile dev up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MailHog | http://localhost:8025 |
| S3 mock | http://localhost:9090 |
| pgAdmin (profil dev) | http://localhost:5050 |

Frontend Angular (profil `full`) : `docker compose --profile full up --build`, puis http://localhost:4200.

## Structure

```
backend/    API Spring Boot (Java 21, Maven)
frontend/   Application Angular (voir frontend/README.md)
docker-compose.yml
```

## Authentification (JWT)

| Endpoint | Accès | Description |
|---|---|---|
| `POST /api/v1/auth/register` | public | Inscription candidat (retourne les tokens) |
| `POST /api/v1/auth/login` | public | Connexion |
| `POST /api/v1/auth/refresh` | public | Nouveau couple access/refresh token |
| `GET /api/v1/auth/me` | authentifié | Profil de l'utilisateur courant |
| `/api/v1/admin/users` | ADMIN | Liste, création, changement de rôle, activation/désactivation |

Dans Swagger UI : faire `login`, copier `accessToken`, cliquer sur **Authorize** et le coller.

Comptes de démo (profil `dev` uniquement) :

| Rôle | Email | Mot de passe |
|---|---|---|
| ADMIN | admin@talentflow.local | Admin123! |
| HR | hr@talentflow.local | Hr123456! |
| MANAGER | manager@talentflow.local | Manager123! |

## Comptes, sécurité et RGPD

| Endpoint | Accès | Description |
|---|---|---|
| `PATCH /api/v1/auth/me` | authentifié | Mettre à jour prénom, nom, téléphone, LinkedIn, photo |
| `DELETE /api/v1/auth/me` | candidat | Suppression RGPD : candidatures et CV effacés, compte anonymisé |
| `POST /api/v1/auth/verify-email` | public | Vérifier l'adresse email avec le jeton reçu par email (48 h) |
| `POST /api/v1/auth/verify-email/resend` | public | Renvoyer un lien de vérification (réponse identique si le compte existe ou non) |
| `POST /api/v1/auth/forgot-password` | public | Demander un lien de réinitialisation (2 h, réponse neutre) |
| `POST /api/v1/auth/reset-password` | public | Définir un nouveau mot de passe avec le jeton reçu |
| `GET /api/v1/staff` | RH, Admin, Manager | Annuaire des comptes équipe (sélection des participants aux entretiens) |
| `GET /api/v1/admin/audit` | ADMIN | Journal d'audit filtrable (`action`, `entityType`, `actorId`) |

- Les jetons de vérification et de réinitialisation sont générés avec `SecureRandom` ; seul leur condensat
  SHA-256 est stocké en base. Ils sont à usage unique et expirent automatiquement.
- `REQUIRE_EMAIL_VERIFICATION=true` impose une adresse vérifiée à la connexion. Par défaut `false` en local
  pour ne pas bloquer les démonstrations et les tests.
- Limitation de débit sur `login`, `register`, `forgot-password`, `reset-password` et `verify-email/resend`
  (`AUTH_RATE_LIMIT_PER_MINUTE`, 100 par défaut et par IP).
- Audit : création/modification d'offres, changements d'étape, exports CSV, gestion des utilisateurs,
  réinitialisations de mot de passe et anonymisations RGPD.

## Offres et candidatures

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/v1/jobs` | public | Offres publiées et ouvertes (`keyword`, `location`, `contractType`, `page`, `size`) |
| `GET /api/v1/jobs/{id}` | public | Détail d'une offre publiée |
| `GET /api/v1/jobs/manage[/{id}]` | RH, Admin, Manager | Toutes les offres (le Manager ne voit que les siennes) |
| `POST /api/v1/jobs` | RH, Admin | Créer une offre (brouillon) |
| `PUT /api/v1/jobs/{id}` | RH, Admin | Modifier |
| `PATCH /api/v1/jobs/{id}/status` | RH, Admin | DRAFT, PUBLISHED, CLOSED, ARCHIVED |
| `DELETE /api/v1/jobs/{id}` | RH, Admin | Supprimer (seulement sans candidature) |
| `POST /api/v1/jobs/{id}/applications` | Candidat | Postuler (multipart : `cv` PDF 5 Mo max, `coverLetter`, `source`) |
| `GET /api/v1/applications/mine` | Candidat | Mes candidatures |
| `DELETE /api/v1/applications/{id}` | Candidat | Retirer ma candidature |
| `GET /api/v1/applications` | RH, Admin, Manager | Liste filtrable (`jobOfferId`, `stage`, `q`) |
| `GET /api/v1/applications/{id}` | selon rôle | Détail |
| `GET /api/v1/applications/{id}/cv` | selon rôle | Télécharger le CV |

## Stockage des CV

`StorageService` (implémentation S3) :
- Local : mock S3 du `docker-compose.yml` (`STORAGE_ENDPOINT=http://s3:9090`).
- AWS : `STORAGE_ENDPOINT=` (vide), `STORAGE_REGION`, `STORAGE_BUCKET`, credentials par rôle IAM.
- GCP : Google Cloud Storage en mode interopérabilité S3 (`STORAGE_ENDPOINT=https://storage.googleapis.com` + clés HMAC).

## Pipeline, notes et entretiens

| Endpoint | Accès | Description |
|---|---|---|
| `PATCH /api/v1/applications/{id}/stage` | RH, Admin | Changer d'étape (`stage`, `reason`) : motif obligatoire pour REJECTED, HIRED seulement depuis OFFER |
| `GET /api/v1/applications/{id}/history` | RH, Admin, Manager | Historique des étapes (qui, quand, motif) |
| `POST/GET /api/v1/applications/{id}/notes` | RH, Admin, Manager | Notes internes (jamais visibles du candidat) |
| `DELETE /api/v1/applications/{id}/notes/{noteId}` | auteur ou Admin | Supprimer une note |
| `POST /api/v1/applications/{id}/interviews` | RH, Admin | Planifier (refus si un participant est déjà pris) |
| `GET /api/v1/applications/{id}/interviews` | RH, Admin, Manager | Entretiens d'une candidature |
| `GET /api/v1/interviews?from=&to=` | RH, Admin, Manager | Calendrier (92 jours max ; le Manager voit les siens) |
| `PATCH /api/v1/interviews/{id}/cancel` | RH, Admin | Annuler |
| `POST/GET /api/v1/interviews/{id}/feedback` | participants / staff | Un feedback par entretien, après son début |

## Notifications et emails

Les emails et notifications sont déclenchés par des événements métier, traités **après commit** et
**en asynchrone** : un SMTP lent ou en panne ne peut jamais faire échouer une action.

| Événement | Email | Notification in-app (staff) |
|---|---|---|
| Candidature envoyée | confirmation au candidat | créatrice de l'offre + manager |
| Changement d'étape | candidat (jamais le motif de refus) | manager + créatrice, sauf l'auteur |
| Entretien planifié | invitation au candidat et aux participants | participants, sauf l'auteur |
| Entretien annulé | candidat et participants | participants, sauf l'auteur |
| Feedback déposé | | RH qui a planifié l'entretien |
| 24 h avant l'entretien | rappel candidat + participants | participants |

- Modèles HTML modifiables sans toucher au Java : `backend/src/main/resources/templates/email/` (Thymeleaf).
- En local, tous les emails arrivent dans MailHog : http://localhost:8025
- Fuseau d'affichage des dates : `APP_TIMEZONE` (défaut `Europe/Paris`). Expéditeur : `MAIL_FROM`.
- Rappels : tâche planifiée toutes les 15 min (`app.reminders.*`), réservation atomique en base (pas de doublon, même à plusieurs instances).

| Endpoint | Description |
|---|---|
| `GET /api/v1/notifications?unreadOnly=&page=&size=` | Mes notifications |
| `GET /api/v1/notifications/unread-count` | Compteur de la cloche |
| `PATCH /api/v1/notifications/{id}/read` | Marquer comme lue |
| `POST /api/v1/notifications/read-all` | Tout marquer comme lu |

## Dashboard, tags et export

| Endpoint | Accès | Description |
|---|---|---|
| `GET /api/v1/dashboard/stats` | RH, Admin | Indicateurs : résumé, répartition par étape, entonnoir de conversion, candidatures dans le temps, offres les plus demandées, sources, délai d'embauche |
| `PUT /api/v1/applications/{id}/tags` | RH, Admin | Remplace les tags (10 max, normalisés en minuscules) |
| `GET /api/v1/applications?tag=&from=&to=` | RH, Admin, Manager | Filtres supplémentaires sur la liste |
| `GET /api/v1/applications/export` | RH, Admin | Export CSV (mêmes filtres que la liste, 10 000 lignes max, tracé dans l'audit) |

Paramètres du dashboard : `from`, `to` (période sur la date de candidature, 90 jours par défaut, 366 max),
`jobOfferId` (une seule offre), `granularity` (`DAY`, `WEEK`, `MONTH`, automatique par défaut).

- **Entonnoir** : nombre de candidatures ayant *atteint* chaque étape (via l'historique), avec le % par rapport aux
  candidatures reçues et à l'étape précédente. Une étape sautée (déplacement libre) n'est pas comptée.
- **Délai d'embauche** : moyenne et médiane en jours entre la candidature et le passage en HIRED.
- **Export CSV** : séparateur `;`, UTF-8 avec BOM (ouverture directe dans Excel), cellules commençant par `= + - @`
  neutralisées contre l'injection de formules. Les tags ne sont jamais visibles du candidat.

## Pages équipe et administration (Angular)

| Page | Accès | Description |
|---|---|---|
| `/equipe/tableau-de-bord` | RH, Admin | Indicateurs, entonnoir, sources, délai d'embauche |
| `/equipe/offres` | RH, Manager, Admin | Liste, filtres, publication, clôture, archivage, suppression |
| `/equipe/offres/nouvelle`, `/equipe/offres/:id/modifier` | RH, Admin | Création et modification d'une offre, affectation d'un manager |
| `/equipe/candidatures` | RH, Manager, Admin | Recherche multicritère, pagination, export CSV |
| `/equipe/candidatures/:id` | RH, Manager, Admin | Dossier complet : CV, parcours, notes, tags, entretiens, feedback |
| `/equipe/pipeline` | RH, Manager, Admin | Vue kanban avec glisser-déposer (RH/Admin) |
| `/equipe/entretiens` | RH, Manager, Admin | Calendrier 7 à 90 jours, annulation, accès au dossier |
| `/admin/utilisateurs` | Admin | Création de comptes, rôles, activation/désactivation |
| `/admin/audit` | Admin | Journal d'audit en lecture seule |
| `/profil` | authentifié | Profil, vérification email, suppression RGPD du compte candidat |
| `/notifications` | authentifié | Centre de notifications avec marquage lu/non lu |
| `/verifier-email`, `/mot-de-passe-oublie`, `/reinitialiser-mot-de-passe` | public | Flux de compte par email |

## Frontend (Angular)

| Page | Accès | Description |
|---|---|---|
| `/offres` | public | Offres ouvertes, filtres dans l'URL (mot-clé, ville, contrat), pagination |
| `/offres/:id` | public | Détail ; formulaire de candidature (CV PDF) pour un compte candidat |
| `/connexion`, `/inscription` | visiteurs | Retour automatique à la page visée après connexion |
| `/mes-candidatures` | candidat | Frise d'avancement de chaque candidature, retrait avec confirmation |
| `/equipe` | RH, Manager, Admin | Espace équipe (résumé du recrutement pour RH et Admin) |

Développement : `cd frontend && npm install && npm start` (http://localhost:4200, proxy vers le backend sur 8080).
En production, nginx sert l'application et relaie `/api` vers le backend (même origine, pas de CORS).

## Tests

```bash
cd backend && mvn verify     # 23 tests, PostgreSQL via Testcontainers
cd frontend && npm test      # 49 tests, build de production avec npm run build
```

Le backend force l'API Docker `1.40` pour Testcontainers (`maven-surefire-plugin`), nécessaire avec les moteurs
récents (Docker Engine 29 exige au minimum l'API 1.40, alors que Testcontainers 1.21 demande 1.32 par défaut).
