# TalentFlow : frontend

Application Angular (composants standalone, signaux, formulaires réactifs) : offres d'emploi publiques,
candidature avec CV, suivi des candidatures, connexion par rôle.

## Développement

```bash
npm install
npm start          # http://localhost:4200, les appels /api sont redirigés vers http://localhost:8080
npm test           # tests unitaires et de composants (Vitest)
npm run build      # build de production dans dist/frontend/browser
```

Le backend doit tourner sur le port 8080 (`docker compose up -d` à la racine du projet).

## Organisation

```
src/app/
  core/       modèles, authentification (service, intercepteur JWT, guards), libellés, formats
  shared/     icônes, frise d'étapes d'une candidature
  features/
    jobs/          liste des offres (filtres dans l'URL), détail d'une offre
    applications/  formulaire de candidature (CV en PDF), mes candidatures
    auth/          connexion, création de compte
    staff/         espace équipe (résumé du recrutement)
```

## Choix notables

- **Session** : le jeton d'accès reste en mémoire ; seul le jeton de renouvellement est conservé
  (`localStorage`) pour survivre au rechargement. L'intercepteur renouvelle le jeton une seule fois sur
  un 401 puis rejoue la requête ; si le renouvellement échoue, retour à la connexion avec la page visée.
  Un cookie httpOnly posé par le backend serait plus résistant au XSS (amélioration possible).
- **Redirections** : `returnUrl` n'accepte que des chemins internes (pas de redirection ouverte).
- **Accessibilité** : lien d'évitement, libellés explicites, erreurs annoncées (`role="alert"`), focus visible,
  parcours d'une candidature en liste ordonnée, `prefers-reduced-motion` respecté.
- **Polices** : Bricolage Grotesque et Instrument Sans, hébergées avec l'application (aucune requête vers un
  service tiers, donc pas de transfert d'adresse IP vers Google Fonts).
- **Production** : nginx sert l'application et relaie `/api` vers le backend (même origine, donc pas de CORS).
