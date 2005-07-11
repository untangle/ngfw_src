/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary statsrmation of
 * Metavize Inc. ("Confidential Statsrmation").  You shall
 * not disclose such Confidential Statsrmation.
 *
 * $Id$
 */
package com.metavize.tran.mail;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Log e-mail message stats.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="EMAIL_MESSAGE_STATS"
 * mutable="false"
 */
public class MessageStats implements Serializable
{
    /* constants */
    // private static final Logger zLog = Logger.getLogger(MessageStats.class.getName());

    /* columns */
    private Long id; /* id */

    private MessageInfo messageInfo; /* msg_id */

    private int numAttachments;

    private long numBytes;

    /* constructors */
    public MessageStats() {}

    public MessageStats(MessageInfo messageInfo, int numAttachments, long numBytes)
    {
        this.messageInfo = messageInfo;
        this.numAttachments = numAttachments;
        this.numBytes = numBytes;
    }

    /* public methods */

    /**
     * 
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Associate e-mail message stats with e-mail message info.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="all"
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
     * Total bytes in message, (body + header?  just body? XX)
     *
     * @return the number of bytes in the message
     * @hibernate.property
     * column="MSG_BYTES"
     */
    public long getNumBytes()
    {
        return numBytes;
    }

    public void setNumBytes(long numBytes)
    {
        this.numBytes = numBytes;
    }

    /**
     * Total attachments in message, (body + header?  just body? XX)
     *
     * @return the number of attachments in the message
     * @hibernate.property
     * column="MSG_ATTACHMENTS"
     */
    public int getNumAttachments()
    {
        return numAttachments;
    }

    public void setNumAttachments(int numAttachments)
    {
        this.numAttachments = numAttachments;
    }

}
