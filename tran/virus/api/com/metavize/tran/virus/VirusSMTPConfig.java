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

package com.metavize.tran.virus;

import java.io.Serializable;
import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;
import com.metavize.tran.mail.papi.smtp.SmtpNotifyMessageGenerator;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_SMTP_CONFIG"
 */
public class VirusSMTPConfig
  extends VirusMailConfig
  implements Serializable
{

    private static final long serialVersionUID = 7520156745253589007L;

    /* settings */
    private SMTPVirusMessageAction zMsgAction = SMTPVirusMessageAction.REMOVE;
    private SMTPNotifyAction zNotifyAction = SMTPNotifyAction.NEITHER;
    private transient SmtpNotifyMessageGenerator m_notifier;
    private String m_notifySubjectTemplate;
    private String m_notifyBodyTemplate;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusSMTPConfig() {}

    public VirusSMTPConfig(boolean bScan,
      SMTPVirusMessageAction zMsgAction,
      SMTPNotifyAction zNotifyAction,
      String zNotes,
      String subjectTemplate,
      String bodyTemplate,
      String notifySubjectTemplate,
      String notifyBodyTemplate)
    {
        super(bScan, zNotes, subjectTemplate, bodyTemplate);    
        this.zMsgAction = zMsgAction;
        this.zNotifyAction = zNotifyAction;
        m_notifySubjectTemplate = notifySubjectTemplate;
        m_notifyBodyTemplate = notifyBodyTemplate;
    }

    // business methods ------------------------------------------------------


    public String getNotifySubjectTemplate() {
      return m_notifySubjectTemplate;
    }
  
    public void setNotifySubjectTemplate(String template) {
      m_notifySubjectTemplate = template;
      ensureNotifyMessageGenerator();
      m_notifier.setSubject(template);
    }
  
    public String getNotifyBodyTemplate() {
      return m_notifyBodyTemplate;
    }
  
    public void setNotifyBodyTemplate(String template) {
      m_notifyBodyTemplate = template;
      ensureNotifyMessageGenerator();
      m_notifier.setBody(template);
    }
  
    /**
      * Access the helper object, for sending notifications
      * based on the {@link #getSubjectWrapperTemplate subject}
      * and {@link #getBodyWrapperTemplate body} templates.
      *
      *
      */
    public SmtpNotifyMessageGenerator getNotificationMessageGenerator() {
      ensureNotifyMessageGenerator();
      return m_notifier;
    }
  
    private void ensureNotifyMessageGenerator() {
      if(m_notifier == null) {
        m_notifier = new SmtpNotifyMessageGenerator(
          getNotifySubjectTemplate(),
          getNotifyBodyTemplate(),
          false);
      }
    }    
    

    /**
     * messageAction: a string specifying a response if a message
     * contains virus (defaults to CLEAN) one of CLEAN, BLOCK, or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="ACTION"
     * type="com.metavize.tran.virus.SMTPVirusMessageActionUserType"
     * not-null="true"
     */
    public SMTPVirusMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(SMTPVirusMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        SMTPVirusMessageAction[] azMsgAction = SMTPVirusMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

    /**
     * notifyAction: a string specifying a response to events if a
     * message containing virus (defaults to NEITHER) one of SENDER,
     * RECEIVER, BOTH, or NEITHER
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="NOTIFY_ACTION"
     * type="com.metavize.tran.mail.papi.smtp.SMTPNotifyActionUserType"
     * not-null="true"
     */
    public SMTPNotifyAction getNotifyAction()
    {
        return zNotifyAction;
    }

    public void setNotifyAction(SMTPNotifyAction zNotifyAction)
    {
        // Guard XXX
        this.zNotifyAction = zNotifyAction;
        return;
    }

    /* for GUI */
    public String[] getNotifyActionEnumeration()
    {
        SMTPNotifyAction[] azNotifyAction = SMTPNotifyAction.getValues();
        String[] azStr = new String[azNotifyAction.length];

        for (int i = 0; i < azNotifyAction.length; i++)
            azStr[i] = azNotifyAction[i].toString();

        return azStr;
    }
}
