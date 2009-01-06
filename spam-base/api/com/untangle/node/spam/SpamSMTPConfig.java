/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.spam;

import javax.persistence.Column;
import javax.persistence.Entity;
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

    public static final int DEFAULT_SUPER_STRENGTH = 100;

    /* settings */
    private boolean throttle = false;
    private int throttleSec = 2; // 2 secs
    private SMTPSpamMessageAction zMsgAction = SMTPSpamMessageAction.QUARANTINE;
    private SpamSMTPNotifyAction zNotifyAction = SpamSMTPNotifyAction.NEITHER;
    private transient SmtpNotifyMessageGenerator m_notifyMsgGenerator;
    private String m_notifySubjectWrapperTemplate;
    private String m_notifyBodyWrapperTemplate;

    private int superSpamStrength = DEFAULT_SUPER_STRENGTH;
    private boolean blockSuperSpam = true;

    private boolean failClosed = true;

    // constructor ------------------------------------------------------------

    public SpamSMTPConfig() { }

    public SpamSMTPConfig(boolean bScan,
                          SMTPSpamMessageAction zMsgAction,
                          SpamSMTPNotifyAction zNotifyAction,
                          int strength,
                          boolean blockSuperSpam,
                          int superSpamStrength,
                          boolean failClosed,
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
        this.blockSuperSpam = blockSuperSpam;
        this.superSpamStrength = superSpamStrength;
        this.failClosed = failClosed;
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

    @Column(name="block_superspam", nullable=false)
    public boolean getBlockSuperSpam()
    {
        return blockSuperSpam;
    }

    public void setBlockSuperSpam(boolean blockSuperSpam)
    {
        this.blockSuperSpam = blockSuperSpam;
    }

    @Column(name="superspam_strength", nullable=false)
    public int getSuperSpamStrength()
    {
        return superSpamStrength;
    }

    public void setSuperSpamStrength(int superSpamStrength)
    {
        this.superSpamStrength = superSpamStrength;
    }

    @Column(name="fail_closed", nullable=false)
    public boolean getFailClosed()
    {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed)
    {
        this.failClosed = failClosed;
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
