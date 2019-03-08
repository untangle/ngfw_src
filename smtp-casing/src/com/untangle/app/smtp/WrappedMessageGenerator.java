/**
 * $Id$
 */
package com.untangle.app.smtp;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.mime.MIMEMessageTemplateValues;
import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * Class which wraps a MIMEMessage with another, providing templates for the resulting subject and body. <br>
 * <br>
 * This class uses the {@link #com Template} class internally. You can create templates which derefference keys found in
 * any number of TemplateValues, passed-into the {@link #wrap wrap method} <br>
 * <br>
 * Note that the {@link #getBodyTemplate Body} and {@link #getSubjectTemplate Subject} templates may be null. If The
 * subject template is null, the subject will not be modified. If the body template is null, the message will not be
 * wrapped (i.e. disables wrapping).
 * 
 */
public class WrappedMessageGenerator
{

    private final Logger m_logger = Logger.getLogger(getClass());
    private Template m_subjectTemplate;
    private Template m_bodyTemplate;
    private String templateLanguage = "";
    private TemplateTranslator templateTranslator;

    /**
     * Initialize instance of WrappedMessageGenerator.
     * @return Instance of WrappedMessageGenerator
     */
    public WrappedMessageGenerator() {
        this(null, null, null);
    }

    /**
     * Full constructor. Please read the class-docs to understand impact of null templates
     * 
     * @param subjectTemplate
     *            the subject template
     * @param bodyTemplate
     *            the bodyTemplate
     * @param templateTranslator Template translator to use.
     * @return Instance of WrappedMessageGenerator
     */
    public WrappedMessageGenerator(String subjectTemplate, String bodyTemplate, TemplateTranslator templateTranslator) {
        setSubject(subjectTemplate);
        setBody(bodyTemplate);
        UvmContext uvm = UvmContextFactory.context();
        LanguageManager languageManager = uvm.languageManager();
        templateLanguage = languageManager.getLanguageSettings().getLanguage();
        this.templateTranslator = templateTranslator;
    }

    /**
     * Wrap the given MIMEMessage. Only the message itself will provide any substitution values for the body or subject
     * templates.
     * 
     * @param msg
     *            the Message to be wrapped
     * 
     * @return the wrapped message, or the original (possibly with modified subject) if the body template was null.
     */
    public MimeMessage wrap(MimeMessage msg)
    {
        return wrap(msg, new TemplateValuesChain());
    }

    /**
     * Wrap the given MIMEMessage, using the provided TemplateValues objects as sources for any substitution keys found
     * within the {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject} templates.
     * 
     * @param msg
     *            the Message to be wrapped
     * @param values
     *            the source of any substitution values
     * 
     * @return the wrapped message, or the original (possibly with modified subject) if the body template was null.
     */
    public MimeMessage wrap(MimeMessage msg, TemplateValues... values)
    {
        return wrap(msg, new TemplateValuesChain(values));
    }

    /**
     * Wrap the given MIMEMessage, using the provided TemplateValuesChain as the source for any substitution keys found
     * within the {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject} templates.
     * 
     * @param msg
     *            the Message to be wrapped
     * @param values
     *            the source of any substitution values
     * 
     * @return the wrapped message, or the original (possibly with modified subject) if the body template was null.
     */
    public MimeMessage wrap(MimeMessage msg, TemplateValuesChain values)
    {
        checkAndTranslateTemplates();
        
        // Add the original message to the chain
        values.append(new MIMEMessageTemplateValues(msg));

        MimeMessage ret = msg;
        Template bodyTemplate = getBodyTemplate();
        if (bodyTemplate != null) {
            try {
                m_logger.debug("Wrapping body");
                ret = MIMEUtil.simpleWrap(bodyTemplate.format(values), msg);
            } catch (Exception ex) {
                m_logger.error(ex);
            }
        } else {
            m_logger.debug("No template to wrap body");
        }
        Template subjectTemplate = getSubjectTemplate();
        if (subjectTemplate != null) {
            try {
                m_logger.debug("Wrapping subject");
                ret.setSubject(subjectTemplate.format(values));
            } catch (Exception ex) {
                m_logger.error(ex);
            }
        } else {
            m_logger.debug("No template for new subject");
        }
        return ret;
    }
    
    /**
     * Checks if the language settings have changed, and recompile the templates if necessary
     */
    private void checkAndTranslateTemplates()
    {
        UvmContext uvm = UvmContextFactory.context();
        LanguageManager languageManager = uvm.languageManager();
        String language = languageManager.getLanguageSettings().getLanguage();
        if (!language.equalsIgnoreCase(templateLanguage)) {
            String subjectTemplate = templateTranslator.getTranslatedSubjectTemplate();
            String bodyTemplate = templateTranslator.getTranslatedBodyTemplate();
            setSubject(subjectTemplate);
            setBody(bodyTemplate);
            templateLanguage = language;
        }
    }

    /**
     * Get subject
     * @return String of subject.
     */
    public String getSubject()
    {
        return m_subjectTemplate == null ? null : m_subjectTemplate.getTemplate();
    }

    /**
     * Set the subject template.
     * 
     * @param template
     *            the template (or null to declare that subjects should not be modified).
     */
    public void setSubject(String template)
    {
        if (template == null) {
            m_subjectTemplate = null;
        }
        if (m_subjectTemplate == null) {
            m_subjectTemplate = new Template(template, false);
        } else {
            m_subjectTemplate.setTemplate(template);
        }
    }

    /**
     * Get message body.
     * @return String of message body.
     */
    public String getBody()
    {
        return m_bodyTemplate == null ? null : m_bodyTemplate.getTemplate();
    }

    /**
     * Set the body template.
     * 
     * @param template
     *            the template (or null to declare that body should not be wrapped).
     */
    public void setBody(String template)
    {
        if (template == null) {
            m_bodyTemplate = null;
        }
        if (m_bodyTemplate == null) {
            m_bodyTemplate = new Template(template, false);
        } else {
            m_bodyTemplate.setTemplate(template);
        }
    }

    /**
     * For subclasses to access the internal body Template Object (or null if not set).
     * @return Template of message body template.
     */
    protected Template getBodyTemplate()
    {
        return m_bodyTemplate;
    }

    /**
     * For subclasses to access the internal subject Template Object (or null if not set).
     * @return Template of message subject template.
     */
    protected Template getSubjectTemplate()
    {
        return m_subjectTemplate;
    }

}
