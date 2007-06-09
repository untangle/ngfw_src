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

package com.untangle.node.spam;

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

import com.untangle.node.mail.papi.smtp.SmtpNotifyMessageGenerator;
import org.hibernate.annotations.Type;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="n_spam_smtp_config", schema="settings")
public class SpamSMTPConfig extends SpamProtoConfig
{
    private static final long serialVersionUID = 7520156745253589107L;

    /* settings */
    private boolean throttle = false;
    private int throttleSec = 2; // 2 secs
    private SMTPSpamMessageAction zMsgAction = SMTPSpamMessageAction.QUARANTINE;
    private SpamSMTPNotifyAction zNotifyAction = SpamSMTPNotifyAction.NEITHER;
    private transient SmtpNotifyMessageGenerator m_notifyMsgGenerator;
    private String m_notifySubjectWrapperTemplate;
    private String m_notifyBodyWrapperTemplate;

    // constructor ------------------------------------------------------------

    public SpamSMTPConfig() { }

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
                          String notifyBodyTemplate,
                          boolean throttle,
                          int throttleSec)
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
        this.throttle = throttle;
        this.throttleSec = throttleSec;
    }

    // accessors --------------------------------------------------------------



    /**
     * Get the tempalte used to create the subject
     * for a notification message.
     */
    @Transient
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
    @Transient
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
     */
    @Transient
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
     * messageAction: a string specifying a response if a message
     * contains spam (defaults to MARK) one of BLOCK, MARK, or PASS
     *
     * @return the action to take if a message is judged to be spam.
     */
    @Column(name="msg_action", nullable=false)
    @Type(type="com.untangle.node.spam.SMTPSpamMessageActionUserType")
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

    /** for GUI */
    @Transient
    public String[] getMsgActionEnumeration()
    {
        SMTPSpamMessageAction[] azMsgAction = SMTPSpamMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

    /**
     * notifyAction: a string specifying a response to events if a
     * message containing spam (defaults to NEITHER) one of SENDER,
     * RECEIVER, or NEITHER
     *
     * @return the action to take if a message is judged to be spam.
     */
    @Column(name="notify_action", nullable=false)
    @Type(type="com.untangle.node.spam.SpamSMTPNotifyActionUserType")
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
    @Transient
    public String[] getNotifyActionEnumeration()
    {
        SpamSMTPNotifyAction[] azNotifyAction = SpamSMTPNotifyAction.getValues();
        String[] azStr = new String[azNotifyAction.length];

        for (int i = 0; i < azNotifyAction.length; i++)
            azStr[i] = azNotifyAction[i].toString();

        return azStr;
    }

    /**
     * throttle: a boolean specifying whether or not to reject a
     * connection from a suspect spammer
     *
     * @return whether or not to reject a spammer
     */
    @Column(nullable=false)
    public boolean getThrottle()
    {
        return throttle;
    }

    public void setThrottle(boolean throttle)
    {
        this.throttle = throttle;
        return;
    }

    /**
     *
     * throttleSec: a integer specifying the # of secs to wait for
     * a response from a RBL site
     *
     * @return the # of secs to wait for a response from a RBL site
     */
    @Column(name="throttle_sec", nullable=false)
    public int getThrottleSec()
    {
        return throttleSec;
    }

    public void setThrottleSec(int throttleSec)
    {
        this.throttleSec = throttleSec;
        return;
    }
}
