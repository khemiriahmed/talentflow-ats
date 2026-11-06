# TalentFlow ATS

#En cours de développement

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
| MinIO console | http://localhost:9001 |
| pgAdmin (profil dev) | http://localhost:5050 |

Le frontend Angular sera ajouté ensuite (profil `full`).

## Structure

```
backend/    API Spring Boot (Java 21, Maven)
frontend/   Application Angular (à venir)
docker-compose.yml
```
