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


/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_POP_CONFIG"
 */
public class SpamPOPConfig extends SpamProtoConfig
{
    private static final long serialVersionUID = 7520156745253589117L;

    /* settings */
    private SpamMessageAction zMsgAction = SpamMessageAction.MARK;


    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamPOPConfig() {}

    public SpamPOPConfig(boolean bScan, SpamMessageAction zMsgAction, int strength, String zNotes)
    {
        super(bScan, strength, zNotes);
        this.zMsgAction = zMsgAction;   
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
     * one of MARK or PASS
     *
     * @return the action to take if a message is judged to be spam.
     * @hibernate.property
     * column="MSG_ACTION"
     * type="com.metavize.tran.spam.SpamMessageActionUserType"
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

}
