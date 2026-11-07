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
