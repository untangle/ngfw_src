/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;
import com.metavize.tran.mime.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * Class which wraps a MIMEMessage with another,
 * providing templates for the resulting
 * subject and body.
 * <br><br>
 * This class uses the {@link #com.metavize.tran.util.Template Template}
 * class internally.  You can create templates which derefference
 * keys found in any number of TemplateValues, passed-into the
 * {@link #wrap wrap method}
 * <br><br>
 * Note that the {@link #getBodyTemplate Body}
 * and {@link #getSubjectTemplate Subject}
 * templates may be null.  If The subject template
 * is null, the subject will not be modified.  If the
 * body template is null, the message will not be
 * wrapped (i.e. disables wrapping).
 *
 */
public class WrappedMessageGenerator {

  private final Logger m_logger = Logger.getLogger(WrappedMessageGenerator.class);

  private Template m_subjectTemplate;
  private Template m_bodyTemplate;


  public WrappedMessageGenerator() {
    this(null, null);
  }

  /**
   * Full constructor.  Please read the class-docs
   * to understand impact of null templates
   *
   * @param subjectTemplate the subject template
   * @param bodyTemplate the bodyTemplate
   */
  public WrappedMessageGenerator(String subjectTemplate,
    String bodyTemplate) {
    setSubjectTemplate(subjectTemplate);
    setBodyTemplate(bodyTemplate);
  }  

  
  public String getSubjectTemplate() {
    return m_subjectTemplate==null?
      null:m_subjectTemplate.getTemplate();
  }

  /**
   * Set the subject template.
   *
   * @param template the template (or null to declare
   *        that subjects should not be modified).
   */
  public void setSubjectTemplate(String template) {
    if(template == null) {
      m_subjectTemplate = null;
    }
    if(m_subjectTemplate == null) {
      m_subjectTemplate = new Template(template, false);
    }
    else {
      m_subjectTemplate.setTemplate(template);
    }
  }

  public String getBodyTemplate() {
    return m_bodyTemplate==null?
      null:m_bodyTemplate.getTemplate();
  }

  /**
   * Set the body template.
   *
   * @param template the template (or null to declare
   *        that body should not be wrapped).
   */  
  public void setBodyTemplate(String template) {
    if(template == null) {
      m_bodyTemplate = null;
    }  
    if(m_bodyTemplate == null) {
      m_bodyTemplate = new Template(template, false);
    }
    else {
      m_bodyTemplate.setTemplate(template);
    }
  }  

  /**
   * Wrap the given MIMEMessage.  Only the message
   * itself will provide any substitution values
   * for the body or subject templates.
   *
   * @param msg the Message to be wrapped
   *
   * @return the wrapped message, or the original
   *         (possibly with modified subject) if the body
   *         template was null.
   */  
  public MIMEMessage wrap(MIMEMessage msg) {
    return wrap(msg, new TemplateValuesChain());
  }  

  /**
   * Wrap the given MIMEMessage, using the provided TemplateValues 
   * objects as sources for any substitution keys found within the
   * {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject}
   * templates.  
   *
   * @param msg the Message to be wrapped
   * @param values the source of any substitution values
   *
   * @return the wrapped message, or the original
   *         (possibly with modified subject) if the body
   *         template was null.
   */  
  public MIMEMessage wrap(MIMEMessage msg, TemplateValues... values) {
    return wrap(msg, new TemplateValuesChain(values));
  }

  /**
   * Wrap the given MIMEMessage, using the provided TemplateValuesChain
   * as the source for any substitution keys found within the
   * {@link #getBodyTemplate Body} or {@link #getSubjectTemplate Subject}
   * templates.  
   *
   * @param msg the Message to be wrapped
   * @param values the source of any substitution values
   *
   * @return the wrapped message, or the original
   *         (possibly with modified subject) if the body
   *         template was null.
   */
  public MIMEMessage wrap(MIMEMessage msg, TemplateValuesChain values) {
    //Add the original message to the chain
    values.append(msg);
    
    MIMEMessage ret = msg;
    if(m_bodyTemplate != null) {
      try {
        m_logger.debug("Wrapping body");
        ret = MIMEUtil.simpleWrap(m_bodyTemplate.format(values), msg);
      }
      catch(Exception ex) {
        m_logger.error(ex);
      }
    }
    if(m_subjectTemplate != null) {
      try {
        m_logger.debug("Wrapping subject");
        ret.getMMHeaders().setSubject(m_subjectTemplate.format(values));
      }
      catch(Exception ex) {
        m_logger.error(ex);
      }    
    }
    return ret;
  }  
  
}