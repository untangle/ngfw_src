package com.untangle.app.smtp;

public interface TemplateTranslator {
    String getTranslatedSubjectTemplate();

    String getTranslatedBodyTemplate();
}
