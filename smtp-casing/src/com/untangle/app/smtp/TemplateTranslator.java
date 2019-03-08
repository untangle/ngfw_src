/**
 * $Id$
 */
package com.untangle.app.smtp;

/**
 * Interface for TemplateTranslator.
 */
public interface TemplateTranslator {
    /**
     * Return translated subject from template.
     * @return String of translated template.
     */
    String getTranslatedSubjectTemplate();

    /**
     * Return translated body from template.
     * @return String of translated template.
     */
    String getTranslatedBodyTemplate();
}
