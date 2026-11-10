package com.talentflow.notification;

import com.talentflow.application.Stage;
import com.talentflow.interview.InterviewMode;

/** Libellés français affichés dans les emails et notifications. */
final class EmailLabels {

    private EmailLabels() {
    }

    static String stage(Stage stage) {
        return switch (stage) {
            case RECEIVED -> "Candidature reçue";
            case SCREENING -> "Présélection";
            case HR_INTERVIEW -> "Entretien RH";
            case TECH_INTERVIEW -> "Entretien technique";
            case OFFER -> "Proposition d'embauche";
            case HIRED -> "Embauché(e)";
            case REJECTED -> "Candidature non retenue";
        };
    }

    static String mode(InterviewMode mode) {
        return switch (mode) {
            case ONLINE -> "En ligne (visioconférence)";
            case ON_SITE -> "Sur site";
        };
    }
}
