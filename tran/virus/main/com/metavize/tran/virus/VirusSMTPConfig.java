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

import com.metavize.tran.mail.SMTPNotifyAction;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_SMTP_CONFIG"
 */
public class VirusSMTPConfig implements Serializable
{
    private static final long serialVersionUID = 7520156745253589007L;

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private SMTPVirusMessageAction zMsgAction = SMTPVirusMessageAction.REMOVE;
    private SMTPNotifyAction zNotifyAction = SMTPNotifyAction.NEITHER;
    private boolean bScan = false;
    private String zNotes = NO_NOTES;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusSMTPConfig() {}

    public VirusSMTPConfig(boolean bScan, SMTPVirusMessageAction zMsgAction, SMTPNotifyAction zNotifyAction, String zNotes)
    {
        this.bScan = bScan;   
        this.zMsgAction = zMsgAction;   
        this.zNotifyAction = zNotifyAction;   
        this.zNotes = zNotes;   
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
     * @hibernate.id
     * column="CONFIG_ID"
     * generator-class="native"
     * not-null="true"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
        return;
    }

    /**
     * scan: a boolean specifying whether or not to scan a message for virus (defaults to true)
     *
     * @return whether or not to scan message for virus
     * @hibernate.property
     * column="SCAN"
     * not-null="true"
     */
    public boolean getScan()
    {
        return bScan;
    }

    public void setScan(boolean bScan)
    {
        this.bScan = bScan;
        return;
    }

    /**
     * messageAction: a string specifying a response if a message contains virus (defaults to CLEAN)
     * one of CLEAN, BLOCK, or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="MSG_ACTION"
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
     * notifyAction: a string specifying a response to events if a message containing virus (defaults to NEITHER)
     * one of SENDER, RECEIVER, BOTH, or NEITHER
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="NOTIFY_ACTION"
     * type="com.metavize.tran.mail.SMTPNotifyActionUserType"
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

    /**
     * notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this virus config
     * @hibernate.property
     * column="NOTES"
     */
    public String getNotes()
    {
        return zNotes;
    }

    public void setNotes(String zNotes)
    {
        this.zNotes = zNotes;
        return;
    }
}
