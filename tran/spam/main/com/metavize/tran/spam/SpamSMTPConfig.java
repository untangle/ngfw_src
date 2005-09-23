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

import com.metavize.tran.mail.papi.smtp.SMTPNotifyAction;

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
    private SMTPSpamMessageAction zMsgAction = SMTPSpamMessageAction.MARK;
    private SMTPNotifyAction zNotifyAction = SMTPNotifyAction.NEITHER;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamSMTPConfig() {}

    public SpamSMTPConfig(boolean bScan,
        SMTPSpamMessageAction zMsgAction,
        SMTPNotifyAction zNotifyAction,
        int strength,
        String zNotes,
        String subjectTemplate,
        String bodyTemplate,
        String headerName,
        String isSpamHeaderValue,
        String isHamHeaderValue)
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
    }

    // business methods ------------------------------------------------------

    /*
    public String render(String site, String category)
    {
        String message = BLOCK_TEMPLATE.replace("@HEADER@", header);
        message = message.replace("@SITE@", site);
        message = message.replace("@CATEGORY@", category);
        message = message.replace("@CONTACT@", contact);

        return message;
    }
    */

    // accessors --------------------------------------------------------------

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
