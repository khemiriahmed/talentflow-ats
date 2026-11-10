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
