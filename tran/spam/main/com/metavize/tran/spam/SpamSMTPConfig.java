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

package com.metavize.tran.spam;

import java.io.Serializable;
import com.metavize.tran.mail.papi.smtp.SmtpNotifyMessageGenerator;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_SMTP_CONFIG"
 */
public class SpamSMTPConfig extends SpamProtoConfig
{
    private static final long serialVersionUID = 7520156745253589107L;

    /* settings */
    private SMTPSpamMessageAction zMsgAction = SMTPSpamMessageAction.QUARANTINE;
    private SpamSMTPNotifyAction zNotifyAction = SpamSMTPNotifyAction.NEITHER;
    private transient SmtpNotifyMessageGenerator m_notifyMsgGenerator;
    private String m_notifySubjectWrapperTemplate;
    private String m_notifyBodyWrapperTemplate;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamSMTPConfig() {}

    public SpamSMTPConfig(boolean bScan,
        SMTPSpamMessageAction zMsgAction,
        SpamSMTPNotifyAction zNotifyAction,
        int strength,
        String zNotes,
        String subjectTemplate,
        String bodyTemplate,
        String headerName,
        String isSpamHeaderValue,
        String isHamHeaderValue,
        String notifySubjectTemplate,
        String notifyBodyTemplate)
    {
        super(bScan,
          strength,
          zNotes,
          subjectTemplate,
          bodyTemplate,
          headerName,
          isSpamHeaderValue,
          isHamHeaderValue);
        this.zMsgAction = zMsgAction;   
        this.zNotifyAction = zNotifyAction;
        m_notifySubjectWrapperTemplate = notifySubjectTemplate;
        m_notifyBodyWrapperTemplate =  notifyBodyTemplate;
    }

    // accessors --------------------------------------------------------------



    /**
    * Get the tempalte used to create the subject
    * for a notification message.
    */
    public String getNotifySubjectTemplate() {
      return m_notifySubjectWrapperTemplate;
    }
  
    public void setNotifySubjectTemplate(String template) {
      m_notifySubjectWrapperTemplate = template;
      ensureNotifyMessageGenerator();
      m_notifyMsgGenerator.setSubject(template);
    }
  
    /**
    * Get the tempalte used to create the body
    * for a notification message.
    */
    public String getNotifyBodyTemplate() {
      return m_notifyBodyWrapperTemplate;
    }
  
    public void setNotifyBodyTemplate(String template) {
      m_notifyBodyWrapperTemplate = template;
      ensureNotifyMessageGenerator();
      m_notifyMsgGenerator.setBody(template);
    }
  
    /**
      * Access the helper object, for producing
      * notifications based on the values of the
      * {@link #getNotifySubjectTemplate subject} and
      * {@link #getNotifyBodyTemplate body} templates.
      *
      *
      */
    public SmtpNotifyMessageGenerator getNotifyMessageGenerator() {
      ensureNotifyMessageGenerator();
      return m_notifyMsgGenerator;
    }
  
    private void ensureNotifyMessageGenerator() {
      if(m_notifyMsgGenerator == null) {
        m_notifyMsgGenerator = new SmtpNotifyMessageGenerator(
          getNotifySubjectTemplate(),
          getNotifyBodyTemplate(),
          false);
      }
    }

    /**
     * messageAction: a string specifying a response if a message contains spam (defaults to MARK)
     * one of BLOCK, MARK, or PASS
     *
     * @return the action to take if a message is judged to be spam.
     * @hibernate.property
     * column="MSG_ACTION"
     * type="com.metavize.tran.spam.SMTPSpamMessageActionUserType"
     * not-null="true"
     */
    public SMTPSpamMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(SMTPSpamMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        SMTPSpamMessageAction[] azMsgAction = SMTPSpamMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

    /**
     * notifyAction: a string specifying a response to events if a message containing spam (defaults to NEITHER)
     * one of SENDER, RECEIVER, BOTH, or NEITHER
     *
     * @return the action to take if a message is judged to be spam.
     * @hibernate.property
     * column="NOTIFY_ACTION"
     * type="com.metavize.tran.spam.SpamSMTPNotifyActionUserType"
     * not-null="true"
     */
    public SpamSMTPNotifyAction getNotifyAction()
    {
        return zNotifyAction;
    }

    public void setNotifyAction(SpamSMTPNotifyAction zNotifyAction)
    {
        // Guard XXX
        this.zNotifyAction = zNotifyAction;
        return;
    }

    /* for GUI */
    public String[] getNotifyActionEnumeration()
    {
        SpamSMTPNotifyAction[] azNotifyAction = SpamSMTPNotifyAction.getValues();
        String[] azStr = new String[azNotifyAction.length];

        for (int i = 0; i < azNotifyAction.length; i++)
            azStr[i] = azNotifyAction[i].toString();

        return azStr;
    }
}
