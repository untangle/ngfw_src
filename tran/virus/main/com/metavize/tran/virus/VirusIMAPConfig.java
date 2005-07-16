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

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_IMAP_CONFIG"
 */
public class VirusIMAPConfig implements Serializable
{
    private static final long serialVersionUID = 7520156745253589027L;

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private VirusMessageAction zMsgAction = VirusMessageAction.REMOVE;
    private boolean bScan = false;
    private String zNotes = NO_NOTES;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusIMAPConfig() {}

    public VirusIMAPConfig(boolean bScan, VirusMessageAction zMsgAction, String zNotes)
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
     * one of CLEAN or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="MSG_ACTION"
     * type="com.metavize.tran.virus.VirusMessageActionUserType"
     * not-null="true"
     */
    public VirusMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(VirusMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        VirusMessageAction[] azMsgAction = VirusMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

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
