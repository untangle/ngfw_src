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

import com.metavize.tran.mail.SpamMessageAction;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_IMAP_CONFIG"
 */
public class SpamIMAPConfig implements Serializable
{
    private static final long serialVersionUID = 7520156745253589127L;

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private SpamMessageAction zMsgAction = SpamMessageAction.MARK;
    private boolean bScan = false;
    private String zNotes = NO_NOTES;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamIMAPConfig() {}

    public SpamIMAPConfig(boolean bScan, SpamMessageAction zMsgAction, String zNotes)
    {
        this.bScan = bScan;   
        this.zMsgAction = zMsgAction;   
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
     * scan: a boolean specifying whether or not to scan a message for spam (defaults to true)
     *
     * @return whether or not to scan message for spam
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
     * messageAction: a string specifying a response if a message contains spam (defaults to MARK)
     * one of MARK or PASS
     *
     * @return the action to take if a message is judged to be spam.
     * @hibernate.property
     * column="MSG_ACTION"
     * type="com.metavize.tran.mail.SpamMessageActionUserType"
     * not-null="true"
     */
    public SpamMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(SpamMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        SpamMessageAction[] azMsgAction = SpamMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

    /**
     * notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this spam config
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
