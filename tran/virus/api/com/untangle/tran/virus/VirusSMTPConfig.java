/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.virus;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.tran.mail.papi.smtp.SMTPNotifyAction;
import com.untangle.tran.mail.papi.smtp.SmtpNotifyMessageGenerator;
import org.hibernate.annotations.Type;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_virus_smtp_config", schema="settings")
public class VirusSMTPConfig extends VirusMailConfig implements Serializable
{

    private static final long serialVersionUID = 7520156745253589007L;

    /* settings */
    private SMTPVirusMessageAction zMsgAction = SMTPVirusMessageAction.REMOVE;
    private SMTPNotifyAction zNotifyAction = SMTPNotifyAction.NEITHER;
    private transient SmtpNotifyMessageGenerator m_notifier;
    private String m_notifySubjectTemplate;
    private String m_notifyBodyTemplate;

    // constructor ------------------------------------------------------------

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

    @Transient
    public String getNotifySubjectTemplate() {
        return m_notifySubjectTemplate;
    }

    public void setNotifySubjectTemplate(String template) {
        m_notifySubjectTemplate = template;
        ensureNotifyMessageGenerator();
        m_notifier.setSubject(template);
    }

    @Transient
    public String getNotifyBodyTemplate() {
        return m_notifyBodyTemplate;
    }

    public void setNotifyBodyTemplate(String template) {
        m_notifyBodyTemplate = template;
        ensureNotifyMessageGenerator();
        m_notifier.setBody(template);
    }

    /**
     * Access the helper object, for sending notifications based on
     * the {@link #getSubjectWrapperTemplate subject} and {@link
     * #getBodyWrapperTemplate body} templates.
     */
    @Transient
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
     */
    @Column(name="action", nullable=false)
    @Type(type="com.untangle.tran.virus.SMTPVirusMessageActionUserType")
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
    @Transient
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
     */
    @Column(name="notify_action", nullable=false)
    @Type(type="com.untangle.tran.mail.papi.smtp.SMTPNotifyActionUserType")
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
    @Transient
    public String[] getNotifyActionEnumeration()
    {
        SMTPNotifyAction[] azNotifyAction = SMTPNotifyAction.getValues();
        String[] azStr = new String[azNotifyAction.length];

        for (int i = 0; i < azNotifyAction.length; i++)
            azStr[i] = azNotifyAction[i].toString();

        return azStr;
    }
}
