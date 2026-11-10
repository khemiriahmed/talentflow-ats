package com.talentflow.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/** Rend un modèle Thymeleaf en HTML (les variables sont échappées automatiquement). */
@Component
@RequiredArgsConstructor
public class EmailRenderer {

    private final TemplateEngine templateEngine;

    public String render(EmailTemplate template, Map<String, Object> variables) {
        Context context = new Context(Locale.FRENCH);
        context.setVariables(variables);
        return templateEngine.process(template.templateName(), context);
    }
}
