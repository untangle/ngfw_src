package com.untangle.node.smtp;

public interface TemplateTranslator {
    String getTranslatedSubjectTemplate();

    String getTranslatedBodyTemplate();
}
