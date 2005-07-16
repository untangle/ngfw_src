/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpamLogEvent.java 502 2005-04-28 03:31:42Z amread $
 */

package com.metavize.tran.spam;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.mail.MessageInfo;

/**
 * Log for POP3/IMAP Spam events.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPAM_EVT"
 * mutable="false"
 */
public class SpamLogEvent extends LogEvent
{
    private MessageInfo messageInfo;
    private float score;
    private boolean isSpam;
    private SpamMessageAction action;
    private String vendorName;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpamLogEvent() { }

    public SpamLogEvent(MessageInfo messageInfo, float score, boolean isSpam,
                        SpamMessageAction action, String vendorName)
    {
        this.messageInfo = messageInfo;
        this.score = score;
        this.isSpam = isSpam;
        this.action = action;
        this.vendorName = vendorName;
    }

    // accessors --------------------------------------------------------------

    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="save-update"
     */
    public MessageInfo getMessageInfo()
    {
        return messageInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo)
    {
        this.messageInfo = messageInfo;
    }

    /**
     * Spam scan score.
     *
     * @return the spam score
     * @hibernate.property
     * column="SCORE"
     */
    public float getScore()
    {
        return score;
    }

    public void setScore(float score)
    {
        this.score = score;
    }

    /**
     * Was it declared spam?
     *
     * @return true if the message is declared to be Spam
     * @hibernate.property
     * column="IS_SPAM"
     */
    public boolean isSpam()
    {
        return isSpam;
    }

    public void setSpam(boolean isSpam)
    {
        this.isSpam = isSpam;
    }

    /**
     * The action taken
     *
     * @return action.
     * @hibernate.property
     * type="com.metavize.tran.spam.SpamMessageActionUserType"
     * column="ACTION"
     */
    public SpamMessageAction getAction()
    {
        return action;
    }

    public void setAction(SpamMessageAction action)
    {
        this.action = action;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     * @hibernate.property
     * column="VENDOR_NAME"
     */
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }
}
