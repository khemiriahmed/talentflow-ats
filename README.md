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

Le frontend Angular sera ajouté ensuite (profil `full`).

## Structure

```
backend/    API Spring Boot (Java 21, Maven)
frontend/   Application Angular (à venir)
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
