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

import com.metavize.tran.mail.MessageAction;
import com.metavize.tran.mail.NotifyAction;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_POP_CONFIG"
 */
public class VirusPOPConfig implements Serializable
{
    private static final long serialVersionUID = 7520156745253589017L;

    public static final String NO_NOTES = "no description";

    private Long id;

    /* settings */
    private MessageAction zMsgAction = MessageAction.BLOCK;
    private NotifyAction zNotifyAction = NotifyAction.NEITHER;
    private boolean bScan = false;
    private boolean bCopyOnBlock = false;
    private String zNotes = NO_NOTES;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusPOPConfig() {}

    public VirusPOPConfig(boolean bScan, MessageAction zMsgAction, NotifyAction zNotifyAction, boolean bCopyOnBlock, String zNotes)
    {
        this.bScan = bScan;   
        this.zMsgAction = zMsgAction;   
        this.zNotifyAction = zNotifyAction;   
        this.bCopyOnBlock = bCopyOnBlock;   
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
     * messageAction: a string specifying a response to events if a message containing virus (defaults to BLOCK)
     * one of BLOCK, MARK, or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="MSG_ACTION"
     * type="com.metavize.tran.mail.MessageActionUserType"
     * not-null="true"
     */
    public MessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(MessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        MessageAction[] azMsgAction = MessageAction.getPOPValues();
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
     * type="com.metavize.tran.mail.NotifyActionUserType"
     * not-null="true"
     */
    public NotifyAction getNotifyAction()
    {
        return zNotifyAction;
    }

    public void setNotifyAction(NotifyAction zNotifyAction)
    {
        // Guard XXX
        this.zNotifyAction = zNotifyAction;
        return;
    }

    /* for GUI */
    public String[] getNotifyActionEnumeration()
    {
        NotifyAction[] azNotifyAction = NotifyAction.getValues();
        String[] azStr = new String[azNotifyAction.length];

        for (int i = 0; i < azNotifyAction.length; i++)
            azStr[i] = azNotifyAction[i].toString();

        return azStr;
    }

    /**
     * copyOnBlock: a boolean specifying whether or not to save a copy of message (e.g., quarantine message) when a filter definition blocks the message (defaults to false)
     *
     * @return whether or not to save a original copy of blocked message
     * @hibernate.property
     * column="COPY_ON_BLOCK"
     * not-null="true"
     */
    public boolean getCopyOnBlock()
    {
        return bCopyOnBlock;
    }

    public void setCopyOnBlock(boolean bCopyOnBlock)
    {
        this.bCopyOnBlock = bCopyOnBlock;
        return;
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
